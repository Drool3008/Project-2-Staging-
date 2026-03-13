/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.business.jpa;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.WebloggerRuntimeException;
import org.apache.roller.weblogger.business.pings.AutoPingManager;
import org.apache.roller.weblogger.business.pings.PingTargetManager;
import org.apache.roller.weblogger.config.WebloggerConfig;

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.roller.weblogger.business.MediaFileManager;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.Weblogger;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.AutoPing;
import org.apache.roller.weblogger.pojos.CustomTemplateRendition;
import org.apache.roller.weblogger.pojos.PingQueueEntry;
import org.apache.roller.weblogger.pojos.PingTarget;
import org.apache.roller.weblogger.pojos.StatCount;
import org.apache.roller.weblogger.pojos.StatCountCountComparator;
import org.apache.roller.weblogger.pojos.TagStat;
import org.apache.roller.weblogger.pojos.ThemeTemplate.ComponentType;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogBookmark;
import org.apache.roller.weblogger.pojos.WeblogBookmarkFolder;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntryTag;
import org.apache.roller.weblogger.pojos.WeblogEntryTagAggregate;
import org.apache.roller.weblogger.pojos.WeblogPermission;
import org.apache.roller.weblogger.pojos.WeblogTemplate;
import org.apache.roller.weblogger.pojos.StarredWeblogEntry;

/*
 * JPAWeblogManagerImpl.java
 * Created on May 31, 2006, 4:08 PM
 */
@com.google.inject.Singleton
public class JPAWeblogManagerImpl implements WeblogManager {

    private static final Log log = LogFactory.getLog(JPAWeblogManagerImpl.class);

    private static final Comparator<StatCount> STAT_COUNT_COUNT_REVERSE_COMPARATOR = Collections
            .reverseOrder(StatCountCountComparator.getInstance());

    private final Weblogger roller;
    private final JPAPersistenceStrategy strategy;

    // cached mapping of weblogHandles -> weblogIds
    private final Map<String, String> weblogHandleToIdMap = Collections.synchronizedMap(new HashMap<>());

    @com.google.inject.Inject
    protected JPAWeblogManagerImpl(Weblogger roller, JPAPersistenceStrategy strat) {
        log.debug("Instantiating JPA Weblog Manager");
        this.roller = roller;
        this.strategy = strat;
    }

    @FunctionalInterface
    private interface WebloggerTask {
        void run();
    }

    @FunctionalInterface
    private interface WebloggerQuery<T> {
        T get();
    }

    private void perform(WebloggerTask task) throws WebloggerException {
        try {
            task.run();
        } catch (WebloggerRuntimeException e) {
            if (e.getCause() instanceof WebloggerException) {
                throw (WebloggerException) e.getCause();
            }
            throw new WebloggerException(e);
        }
    }

    private <T> T query(WebloggerQuery<T> query) throws WebloggerException {
        try {
            return query.get();
        } catch (WebloggerRuntimeException e) {
            if (e.getCause() instanceof WebloggerException) {
                throw (WebloggerException) e.getCause();
            }
            throw new WebloggerException(e);
        }
    }

    @Override
    public void release() {
    }

    /**
     * Update existing weblog.
     */
    @Override
    public void saveWeblog(Weblog weblog) throws WebloggerException {
        perform(() -> doSaveWeblog(weblog));
    }

    private void doSaveWeblog(Weblog weblog) {
        weblog.setLastModified(new java.util.Date());
        try {
            strategy.store(weblog);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public void removeWeblog(Weblog weblog) throws WebloggerException {
        perform(() -> doRemoveWeblog(weblog));
    }

    private void doRemoveWeblog(Weblog weblog) {
        // remove contents first, then remove weblog
        this.removeWeblogContents(weblog);
        try {
            this.strategy.remove(weblog);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }

        // remove entry from cache mapping
        this.weblogHandleToIdMap.remove(weblog.getHandle());
    }

    /**
     * convenience method for removing contents of a weblog.
     * TODO BACKEND: use manager methods instead of queries here
     */
    private void removeWeblogContents(Weblog weblog) {

        try {
            UserManager umgr = roller.getUserManager();
            WeblogEntryManager emgr = roller.getWeblogEntryManager();

            // remove tags
            TypedQuery<WeblogEntryTag> tagQuery = strategy.getNamedQuery("WeblogEntryTag.getByWeblog",
                    WeblogEntryTag.class);
            tagQuery.setParameter(1, weblog);
            List<WeblogEntryTag> results = tagQuery.getResultList();

            for (WeblogEntryTag tagData : results) {
                if (tagData.getWeblogEntry() != null) {
                    tagData.getWeblogEntry().getTags().remove(tagData);
                }
                this.strategy.remove(tagData);
            }

            // remove site tag aggregates
            List<TagStat> tags = emgr.getTags(weblog, null, null, 0, -1);
            updateTagAggregates(tags);

            // delete all weblog tag aggregates
            Query removeAggs = strategy.getNamedUpdate(
                    "WeblogEntryTagAggregate.removeByWeblog");
            removeAggs.setParameter(1, weblog);
            removeAggs.executeUpdate();

            // delete all bad counts
            Query removeCounts = strategy.getNamedUpdate(
                    "WeblogEntryTagAggregate.removeByTotalLessEqual");
            removeCounts.setParameter(1, 0);
            removeCounts.executeUpdate();

            // Remove the weblog's ping queue entries
            TypedQuery<PingQueueEntry> q = strategy.getNamedQuery("PingQueueEntry.getByWebsite", PingQueueEntry.class);
            q.setParameter(1, weblog);
            List<PingQueueEntry> queueEntries = q.getResultList();
            for (Object obj : queueEntries) {
                this.strategy.remove(obj);
            }

            // Remove the weblog's auto ping configurations
            AutoPingManager autoPingMgr = roller.getAutopingManager();
            List<AutoPing> autopings = autoPingMgr.getAutoPingsByWebsite(weblog);
            for (AutoPing autoPing : autopings) {
                this.strategy.remove(autoPing);
            }

            // remove associated templates
            TypedQuery<WeblogTemplate> templateQuery = strategy.getNamedQuery("WeblogTemplate.getByWeblog",
                    WeblogTemplate.class);
            templateQuery.setParameter(1, weblog);
            List<WeblogTemplate> templates = templateQuery.getResultList();

            for (WeblogTemplate template : templates) {
                this.strategy.remove(template);
            }

            // remove folders (including bookmarks)
            TypedQuery<WeblogBookmarkFolder> folderQuery = strategy.getNamedQuery("WeblogBookmarkFolder.getByWebsite",
                    WeblogBookmarkFolder.class);
            folderQuery.setParameter(1, weblog);
            List<WeblogBookmarkFolder> folders = folderQuery.getResultList();
            for (WeblogBookmarkFolder wbf : folders) {
                this.strategy.remove(wbf);
            }

            // remove mediafile metadata
            // remove uploaded files
            MediaFileManager mfmgr = WebloggerFactory.getWeblogger().getMediaFileManager();
            mfmgr.removeAllFiles(weblog);
            // List<MediaFileDirectory> dirs = mmgr.getMediaFileDirectories(weblog);
            // for (MediaFileDirectory dir : dirs) {
            // this.strategy.remove(dir);
            // }
            this.strategy.flush();

            // remove entries
            TypedQuery<WeblogEntry> refQuery = strategy.getNamedQuery("WeblogEntry.getByWebsite", WeblogEntry.class);
            refQuery.setParameter(1, weblog);
            List<WeblogEntry> entries = refQuery.getResultList();
            for (WeblogEntry entry : entries) {
                emgr.removeWeblogEntry(entry);
            }
            this.strategy.flush();

            // delete all weblog categories
            Query removeCategories = strategy.getNamedUpdate("WeblogCategory.removeByWeblog");
            removeCategories.setParameter(1, weblog);
            removeCategories.executeUpdate();

            // remove permissions
            for (WeblogPermission perm : umgr.getWeblogPermissions(weblog)) {
                umgr.revokeWeblogPermission(perm.getWeblog(), perm.getUser(), WeblogPermission.ALL_ACTIONS);
            }

            // flush the changes before returning. This is required as there is a
            // circular dependency between WeblogCategory and Weblog
            try {
                this.strategy.flush();
            } catch (WebloggerException e) {
                throw new WebloggerRuntimeException(e);
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    protected void updateTagAggregates(List<TagStat> tags) {
        try {
            for (TagStat stat : tags) {
                TypedQuery<WeblogEntryTagAggregate> query = strategy.getNamedQueryCommitFirst(
                        "WeblogEntryTagAggregate.getByName&WebsiteNullOrderByLastUsedDesc",
                        WeblogEntryTagAggregate.class);
                query.setParameter(1, stat.getName());
                try {
                    WeblogEntryTagAggregate agg = query.getSingleResult();
                    agg.setTotal(agg.getTotal() - stat.getCount());
                } catch (NoResultException ignored) {
                    // nothing to update
                }
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * @see org.apache.roller.weblogger.business.WeblogManager#saveTemplate(WeblogTemplate)
     */
    @Override
    public void saveTemplate(WeblogTemplate template) throws WebloggerException {
        perform(() -> doSaveTemplate(template));
    }

    private void doSaveTemplate(WeblogTemplate template) {
        try {
            this.strategy.store(template);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }

        // update weblog last modified date. date updated by saveWeblog()
        // Note: saveWeblog is public and throws WebloggerException, but here we act
        // internally.
        // We should call doSaveWeblog to stay internal.
        doSaveWeblog(template.getWeblog());
    }

    @Override
    public void saveTemplateRendition(CustomTemplateRendition rendition) throws WebloggerException {
        perform(() -> doSaveTemplateRendition(rendition));
    }

    private void doSaveTemplateRendition(CustomTemplateRendition rendition) {
        try {
            this.strategy.store(rendition);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }

        // update weblog last modified date. date updated by saveWeblog()
        doSaveWeblog(rendition.getWeblogTemplate().getWeblog());
    }

    @Override
    public void removeTemplate(WeblogTemplate template) throws WebloggerException {
        perform(() -> doRemoveTemplate(template));
    }

    private void doRemoveTemplate(WeblogTemplate template) {
        try {
            this.strategy.remove(template);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
        // update weblog last modified date. date updated by saveWeblog()
        doSaveWeblog(template.getWeblog());
    }

    @Override
    public void addWeblog(Weblog newWeblog) throws WebloggerException {
        perform(() -> doAddWeblog(newWeblog));
    }

    private void doAddWeblog(Weblog newWeblog) {
        try {
            this.strategy.store(newWeblog);
            this.strategy.flush();
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
        this.addWeblogContents(newWeblog);
    }

    private void addWeblogContents(Weblog newWeblog) {
        try {
            // grant weblog creator ADMIN permission
            List<String> actions = new ArrayList<>();
            actions.add(WeblogPermission.ADMIN);
            roller.getUserManager().grantWeblogPermission(
                    newWeblog, newWeblog.getCreator(), actions);

            String cats = WebloggerConfig.getProperty("newuser.categories");
            WeblogCategory firstCat = null;
            if (cats != null) {
                String[] splitcats = cats.split(",");
                for (String split : splitcats) {
                    if (split.isBlank()) {
                        continue;
                    }
                    WeblogCategory c = new WeblogCategory(
                            newWeblog,
                            split,
                            null,
                            null);
                    if (firstCat == null) {
                        firstCat = c;
                    }
                    this.strategy.store(c);
                }
            }

            // Use first category as default for Blogger API
            if (firstCat != null) {
                newWeblog.setBloggerCategory(firstCat);
            }

            this.strategy.store(newWeblog);

            // add default bookmarks
            WeblogBookmarkFolder defaultFolder = new WeblogBookmarkFolder(
                    "default", newWeblog);
            this.strategy.store(defaultFolder);

            String blogroll = WebloggerConfig.getProperty("newuser.blogroll");
            if (blogroll != null) {
                String[] splitroll = blogroll.split(",");
                for (String splitItem : splitroll) {
                    String[] rollitems = splitItem.split("\\|");
                    if (rollitems.length > 1) {
                        WeblogBookmark b = new WeblogBookmark(
                                defaultFolder,
                                rollitems[0],
                                "",
                                rollitems[1].trim(),
                                null,
                                null);
                        this.strategy.store(b);
                    }
                }
            }

            roller.getMediaFileManager().createDefaultMediaFileDirectory(newWeblog);

            // flush so that all data up to this point can be available in db
            this.strategy.flush();

            // add any auto enabled ping targets
            PingTargetManager pingTargetMgr = roller.getPingTargetManager();
            AutoPingManager autoPingMgr = roller.getAutopingManager();

            for (PingTarget pingTarget : pingTargetMgr.getCommonPingTargets()) {
                if (pingTarget.isAutoEnabled()) {
                    AutoPing autoPing = new AutoPing(
                            null, pingTarget, newWeblog);
                    autoPingMgr.saveAutoPing(autoPing);
                }
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public Weblog getWeblog(String id) throws WebloggerException {
        return query(() -> doGetWeblog(id));
    }

    private Weblog doGetWeblog(String id) {
        try {
            return (Weblog) this.strategy.load(Weblog.class, id);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public Weblog getWeblogByHandle(String handle) throws WebloggerException {
        return getWeblogByHandle(handle, Boolean.TRUE);
    }

    /**
     * Return weblog specified by handle.
     */
    @Override
    public Weblog getWeblogByHandle(String handle, Boolean visible) throws WebloggerException {
        return query(() -> doGetWeblogByHandle(handle, visible));
    }

    private Weblog doGetWeblogByHandle(String handle, Boolean visible) {

        if (handle == null) {
            throw new WebloggerRuntimeException("Handle cannot be null");
        } else if (!isAlphanumeric(handle)) {
            throw new WebloggerRuntimeException("Invalid handle: '" + handle + "'");
        }

        // check cache first
        // NOTE: if we ever allow changing handles then this needs updating
        String blogID = this.weblogHandleToIdMap.get(handle);
        if (blogID != null) {

            Weblog weblog = this.doGetWeblog(blogID);
            if (weblog != null) {
                // only return weblog if enabled status matches
                if (visible == null || visible.equals(weblog.getVisible())) {
                    log.debug("weblogHandleToId CACHE HIT - " + handle);
                    return weblog;
                }
            } else {
                // mapping hit with lookup miss? mapping must be old, remove it
                this.weblogHandleToIdMap.remove(handle);
            }
        }

        try {
            TypedQuery<Weblog> query = strategy.getNamedQuery("Weblog.getByHandle", Weblog.class);
            query.setParameter(1, handle);
            Weblog weblog;
            try {
                weblog = query.getSingleResult();
            } catch (NoResultException e) {
                weblog = null;
            }

            // add mapping to cache
            if (weblog != null) {
                log.debug("weblogHandleToId CACHE MISS - " + handle);
                this.weblogHandleToIdMap.put(weblog.getHandle(), weblog.getId());
            }

            if (weblog != null &&
                    (visible == null || visible.equals(weblog.getVisible()))) {
                return weblog;
            } else {
                return null;
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * Get weblogs of a user
     */
    @Override
    public List<Weblog> getWeblogs(
            Boolean enabled, Boolean active,
            Date startDate, Date endDate, int offset, int length) throws WebloggerException {
        return query(() -> doGetWeblogs(enabled, active, startDate, endDate, offset, length));
    }

    private List<Weblog> doGetWeblogs(
            Boolean enabled, Boolean active,
            Date startDate, Date endDate, int offset, int length) {

        try {
            // if (endDate == null) endDate = new Date();

            List<Object> params = new ArrayList<>();
            int size = 0;
            String queryString;
            StringBuilder whereClause = new StringBuilder();

            queryString = "SELECT w FROM Weblog w WHERE ";

            if (startDate != null) {
                Timestamp start = new Timestamp(startDate.getTime());
                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }
                params.add(size++, start);
                whereClause.append(" w.dateCreated > ?").append(size);
            }
            if (endDate != null) {
                Timestamp end = new Timestamp(endDate.getTime());
                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }
                params.add(size++, end);
                whereClause.append(" w.dateCreated < ?").append(size);
            }
            if (enabled != null) {
                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }
                params.add(size++, enabled);
                whereClause.append(" w.visible = ?").append(size);
            }
            if (active != null) {
                if (whereClause.length() > 0) {
                    whereClause.append(" AND ");
                }
                params.add(size++, active);
                whereClause.append(" w.active = ?").append(size);
            }

            whereClause.append(" ORDER BY w.dateCreated DESC");

            TypedQuery<Weblog> query = strategy.getDynamicQuery(queryString + whereClause.toString(), Weblog.class);
            if (offset != 0) {
                query.setFirstResult(offset);
            }
            if (length != -1) {
                query.setMaxResults(length);
            }
            for (int i = 0; i < params.size(); i++) {
                query.setParameter(i + 1, params.get(i));
            }

            return query.getResultList();
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<Weblog> getUserWeblogs(User user, boolean enabledOnly) throws WebloggerException {
        return query(() -> doGetUserWeblogs(user, enabledOnly));
    }

    private List<Weblog> doGetUserWeblogs(User user, boolean enabledOnly) {
        try {
            List<Weblog> weblogs = new ArrayList<>();
            if (user == null) {
                return weblogs;
            }
            List<WeblogPermission> perms = roller.getUserManager().getWeblogPermissions(user);
            for (WeblogPermission perm : perms) {
                Weblog weblog = perm.getWeblog();
                if ((!enabledOnly || weblog.getVisible()) && BooleanUtils.isTrue(weblog.getActive())) {
                    weblogs.add(weblog);
                }
            }
            return weblogs;
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) { // For getWeblogPermissions or other calls if they were strict
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<User> getWeblogUsers(Weblog weblog, boolean enabledOnly) throws WebloggerException {
        return query(() -> doGetWeblogUsers(weblog, enabledOnly));
    }

    private List<User> doGetWeblogUsers(Weblog weblog, boolean enabledOnly) {
        try {
            List<User> users = new ArrayList<>();
            List<WeblogPermission> perms = roller.getUserManager().getWeblogPermissions(weblog);
            for (WeblogPermission perm : perms) {
                User user = perm.getUser();
                if (user == null) {
                    log.error("ERROR user is null, userName:" + perm.getUserName());
                    continue;
                }
                if (!enabledOnly || user.getEnabled()) {
                    users.add(user);
                }
            }
            return users;
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public WeblogTemplate getTemplate(String id) throws WebloggerException {
        return query(() -> doGetTemplate(id));
    }

    private WeblogTemplate doGetTemplate(String id) {
        // Don't hit database for templates stored on disk
        if (id != null && id.endsWith(".vm")) {
            return null;
        }

        try {
            return (WeblogTemplate) this.strategy.load(WeblogTemplate.class, id);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * Use JPA directly because Weblogger's Query API does too much allocation.
     */
    @Override
    public WeblogTemplate getTemplateByLink(Weblog weblog, String templateLink) throws WebloggerException {
        return query(() -> doGetTemplateByLink(weblog, templateLink));
    }

    private WeblogTemplate doGetTemplateByLink(Weblog weblog, String templateLink) {

        if (weblog == null) {
            throw new WebloggerRuntimeException("userName is null");
        }

        if (templateLink == null) {
            throw new WebloggerRuntimeException("templateLink is null");
        }

        try {
            TypedQuery<WeblogTemplate> query = strategy.getNamedQuery("WeblogTemplate.getByWeblog&Link",
                    WeblogTemplate.class);
            query.setParameter(1, weblog);
            query.setParameter(2, templateLink);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * @see org.apache.roller.weblogger.business.WeblogManager#getTemplateByAction(Weblog,
     *      ComponentType)
     */
    @Override
    public WeblogTemplate getTemplateByAction(Weblog weblog, ComponentType action) throws WebloggerException {
        return query(() -> doGetTemplateByAction(weblog, action));
    }

    private WeblogTemplate doGetTemplateByAction(Weblog weblog, ComponentType action) {

        if (weblog == null) {
            throw new WebloggerRuntimeException("weblog is null");
        }

        if (action == null) {
            throw new WebloggerRuntimeException("Action name is null");
        }

        try {
            TypedQuery<WeblogTemplate> query = strategy.getNamedQuery("WeblogTemplate.getByAction",
                    WeblogTemplate.class);
            query.setParameter(1, weblog);
            query.setParameter(2, action);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * @see org.apache.roller.weblogger.business.WeblogManager#getTemplateByName(Weblog,
     *      java.lang.String)
     */
    @Override
    public WeblogTemplate getTemplateByName(Weblog weblog, String templateName) throws WebloggerException {
        return query(() -> doGetTemplateByName(weblog, templateName));
    }

    private WeblogTemplate doGetTemplateByName(Weblog weblog, String templateName) {

        if (weblog == null) {
            throw new WebloggerRuntimeException("weblog is null");
        }

        if (templateName == null) {
            throw new WebloggerRuntimeException("Template name is null");
        }

        try {
            TypedQuery<WeblogTemplate> query = strategy.getNamedQuery("WeblogTemplate.getByWeblog&Name",
                    WeblogTemplate.class);
            query.setParameter(1, weblog);
            query.setParameter(2, templateName);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * @see org.apache.roller.weblogger.business.WeblogManager#getTemplates(Weblog)
     */
    @Override
    public List<WeblogTemplate> getTemplates(Weblog weblog) throws WebloggerException {
        return query(() -> doGetTemplates(weblog));
    }

    private List<WeblogTemplate> doGetTemplates(Weblog weblog) {
        if (weblog == null) {
            throw new WebloggerRuntimeException("weblog is null");
        }
        try {
            TypedQuery<WeblogTemplate> q = strategy.getNamedQuery(
                    "WeblogTemplate.getByWeblogOrderByName", WeblogTemplate.class);
            q.setParameter(1, weblog);
            return q.getResultList();
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public Map<String, Long> getWeblogHandleLetterMap() throws WebloggerException {
        return query(() -> doGetWeblogHandleLetterMap());
    }

    private Map<String, Long> doGetWeblogHandleLetterMap() {
        String lc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Map<String, Long> results = new TreeMap<>();
        try {
            TypedQuery<Long> query = strategy.getNamedQuery(
                    "Weblog.getCountByHandleLike", Long.class);
            for (int i = 0; i < 26; i++) {
                char currentChar = lc.charAt(i);
                query.setParameter(1, currentChar + "%");
                List<Long> row = query.getResultList();
                Long count = row.get(0);
                results.put(String.valueOf(currentChar), count);
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
        return results;
    }

    @Override
    public List<Weblog> getWeblogsByLetter(char letter, int offset, int length) throws WebloggerException {
        return query(() -> doGetWeblogsByLetter(letter, offset, length));
    }

    private List<Weblog> doGetWeblogsByLetter(char letter, int offset, int length) {
        try {
            TypedQuery<Weblog> query = strategy.getNamedQuery(
                    "Weblog.getByLetterOrderByHandle", Weblog.class);
            query.setParameter(1, letter + "%");
            if (offset != 0) {
                query.setFirstResult(offset);
            }
            if (length != -1) {
                query.setMaxResults(length);
            }
            return query.getResultList();
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<StatCount> getMostCommentedWeblogs(Date startDate, Date endDate,
            int offset, int length) throws WebloggerException {
        return query(() -> doGetMostCommentedWeblogs(startDate, endDate, offset, length));
    }

    private List<StatCount> doGetMostCommentedWeblogs(Date startDate, Date endDate,
            int offset, int length) {

        try {
            Query query;

            if (endDate == null) {
                endDate = new Date();
            }

            if (startDate != null) {
                Timestamp start = new Timestamp(startDate.getTime());
                Timestamp end = new Timestamp(endDate.getTime());
                query = strategy.getNamedQuery(
                        "WeblogEntryComment.getMostCommentedWebsiteByEndDate&StartDate");
                query.setParameter(1, end);
                query.setParameter(2, start);
            } else {
                Timestamp end = new Timestamp(endDate.getTime());
                query = strategy.getNamedQuery(
                        "WeblogEntryComment.getMostCommentedWebsiteByEndDate");
                query.setParameter(1, end);
            }
            if (offset != 0) {
                query.setFirstResult(offset);
            }
            if (length != -1) {
                query.setMaxResults(length);
            }
            List<?> queryResults = query.getResultList();
            List<StatCount> results = new ArrayList<>();
            if (queryResults != null) {
                for (Object obj : queryResults) {
                    Object[] row = (Object[]) obj;
                    StatCount sc = new StatCount(
                            (String) row[1], // weblog id
                            (String) row[2], // weblog handle
                            (String) row[3], // weblog name
                            "statCount.weblogCommentCountType", // stat type
                            ((Long) row[0])); // # comments
                    sc.setWeblogHandle((String) row[2]);
                    results.add(sc);
                }
            }

            // Original query ordered by desc # comments.
            // JPA QL doesn't allow queries to be ordered by aggregates; do it in memory
            results.sort(STAT_COUNT_COUNT_REVERSE_COMPARATOR);

            return results;
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * Get count of weblogs, active and inactive
     */
    @Override
    public long getWeblogCount() throws WebloggerException {
        return query(() -> doGetWeblogCount());
    }

    private long doGetWeblogCount() {
        try {
            List<Long> results = strategy.getNamedQuery(
                    "Weblog.getCountAllDistinct", Long.class).getResultList();
            return results.get(0);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * Returns true if alphanumeric or '_'.
     */
    private boolean isAlphanumeric(String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isLetterOrDigit(str.charAt(i)) && str.charAt(i) != '_') {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ Star (favourite) methods

    @Override
    public void starWeblog(User user, Weblog weblog) throws WebloggerException {
        // Re-attach detached user entity so the collection change is tracked
        User managedUser = (User) strategy.getEntityManager(true).merge(user);
        managedUser.getStarredWeblogs().add(weblog);
        strategy.flush();
    }

    @Override
    public void unstarWeblog(User user, Weblog weblog) throws WebloggerException {
        // Re-attach detached user entity so the collection change is tracked
        User managedUser = (User) strategy.getEntityManager(true).merge(user);
        managedUser.getStarredWeblogs().remove(weblog);
        strategy.flush();
    }

    @Override
    public List<StarredWeblogEntry> getStarredWeblogsSortedByRecency(User user) throws WebloggerException {
        try {
            // Use a transaction-aware EM so all pending writes are visible,
            // and bypass EclipseLink's shared L2 cache so we always get
            // up-to-date pubTime values after a new entry is published.
            jakarta.persistence.EntityManager em = strategy.getEntityManager(true);
            Query q = em.createNamedQuery("Weblog.getStarredByUserSortedByRecency");
            q.setHint("jakarta.persistence.cache.retrieveMode",
                      jakarta.persistence.CacheRetrieveMode.BYPASS);
            q.setParameter(1, user.getId());
            List<?> rawResults = q.getResultList();
            List<StarredWeblogEntry> results = new ArrayList<>();
            for (Object row : rawResults) {
                Weblog weblog;
                Date latestPostTime = null;
                if (row instanceof Object[]) {
                    Object[] cols = (Object[]) row;
                    weblog = (Weblog) cols[0];
                    if (cols[1] != null) {
                        latestPostTime = toDate(cols[1]);
                    }
                } else {
                    // Only a Weblog was returned (no scalar column)
                    weblog = (Weblog) row;
                }
                results.add(new StarredWeblogEntry(weblog, latestPostTime));
            }
            // Ensure most-recent-post-first ordering (NULLs last)
            results.sort((a, b) -> {
                if (a.getLatestPostTime() == null && b.getLatestPostTime() == null) return 0;
                if (a.getLatestPostTime() == null) return 1;
                if (b.getLatestPostTime() == null) return -1;
                return b.getLatestPostTime().compareTo(a.getLatestPostTime());
            });
            return results;
        } catch (jakarta.persistence.PersistenceException pe) {
            throw new WebloggerException("Error fetching starred weblogs", pe);
        }
    }

    /** Convert a JPA-returned temporal value to a java.util.Date. */
    private static Date toDate(Object ts) {
        if (ts instanceof java.sql.Timestamp) {
            return new Date(((java.sql.Timestamp) ts).getTime());
        } else if (ts instanceof Date) {
            return (Date) ts;
        } else if (ts instanceof java.time.LocalDateTime) {
            return Date.from(((java.time.LocalDateTime) ts)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant());
        } else if (ts instanceof java.time.Instant) {
            return Date.from((java.time.Instant) ts);
        } else {
            // fallback: epoch millis
            return new Date(((Number) ts).longValue());
        }
    }

    @Override
    public boolean isWeblogStarredByUser(User user, Weblog weblog) throws WebloggerException {
        TypedQuery<Long> q = strategy.getNamedQuery("Weblog.countStarredByUser", Long.class);
        q.setParameter(1, weblog.getId());
        q.setParameter(2, user.getId());
        return q.getSingleResult() > 0;
    }

    @Override
    public List<Object[]> getTrendingWeblogs(int limit) throws WebloggerException {
        // Single aggregate GROUP BY query — counts stars per weblog without
        // iterating over individual weblogs in application code.
        try {
            jakarta.persistence.EntityManager em = strategy.getEntityManager(true);
            Query q = em.createNamedQuery("Weblog.getTopStarredWeblogs");
            q.setHint("jakarta.persistence.cache.retrieveMode",
                      jakarta.persistence.CacheRetrieveMode.BYPASS);
            q.setMaxResults(limit);
            @SuppressWarnings("unchecked")
            List<Object[]> results = (List<Object[]>) q.getResultList();
            return results;
        } catch (Exception e) {
            throw new WebloggerException("Error fetching trending weblogs", e);
        }
    }

}
