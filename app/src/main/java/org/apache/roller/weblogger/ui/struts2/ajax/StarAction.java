/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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
 * limitations under the License.
 */
package org.apache.roller.weblogger.ui.struts2.ajax;

import com.opensymphony.xwork2.ActionSupport;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.StarFacade;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.ui.core.RollerSession;
import org.apache.struts2.interceptor.ServletRequestAware;

/**
 * Struts2 action that handles star / unstar requests for Weblogs and
 * WeblogEntries. Returns a JSON result (via struts2-json-plugin).
 *
 * Uses the Facade design pattern:
 * - StarFacade: Unified interface to star operations
 */
public class StarAction extends ActionSupport implements ServletRequestAware {

    private static final Log log = LogFactory.getLog(StarAction.class);
    private static final long serialVersionUID = 1L;

    private HttpServletRequest servletRequest;

    private String weblogId;
    private String entryId;
    private Map<String, Object> result = new HashMap<>();

    // Facade pattern: single point of access for star operations
    private final StarFacade starFacade = new StarFacade();

    // -------------------------------------------------- ServletRequestAware

    @Override
    public void setServletRequest(HttpServletRequest request) {
        this.servletRequest = request;
    }

    // -------------------------------------------------- Helper

    private User getAuthenticatedUser() {
        RollerSession rs = RollerSession.getRollerSession(servletRequest);
        return (rs != null) ? rs.getAuthenticatedUser() : null;
    }

    // -------------------------------------------------- Actions

    public String starWeblog() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                result.put("success", false);
                result.put("error", "Not authenticated");
                return SUCCESS;
            }
            Weblog weblog = WebloggerFactory.getWeblogger()
                    .getWeblogManager().getWeblog(weblogId);
            if (weblog == null) {
                result.put("success", false);
                result.put("error", "Weblog not found");
                return SUCCESS;
            }
            // Facade pattern: use StarFacade to star weblog
            starFacade.starWeblog(user, weblog);
            result.put("success", true);
        } catch (WebloggerException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return SUCCESS;
    }

    public String unstarWeblog() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                result.put("success", false);
                result.put("error", "Not authenticated");
                return SUCCESS;
            }
            Weblog weblog = WebloggerFactory.getWeblogger()
                    .getWeblogManager().getWeblog(weblogId);
            if (weblog == null) {
                result.put("success", false);
                result.put("error", "Weblog not found");
                return SUCCESS;
            }
            // Facade pattern: use StarFacade to unstar weblog
            starFacade.unstarWeblog(user, weblog);
            result.put("success", true);
        } catch (WebloggerException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return SUCCESS;
    }

    public String starEntry() {
        log.info("starEntry called with entryId=" + entryId);
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                log.warn("starEntry: user not authenticated");
                result.put("success", false);
                result.put("error", "Not authenticated");
                return SUCCESS;
            }
            log.info("starEntry: user=" + user.getUserName());
            WeblogEntry entry = WebloggerFactory.getWeblogger()
                    .getWeblogEntryManager().getWeblogEntry(entryId);
            if (entry == null) {
                log.warn("starEntry: entry not found for id=" + entryId);
                result.put("success", false);
                result.put("error", "Entry not found");
                return SUCCESS;
            }
            log.info("starEntry: starring entry '" + entry.getTitle() + "' for user " + user.getUserName());
            // Facade pattern: use StarFacade to star entry
            starFacade.starEntry(user, entry);
            log.info("starEntry: SUCCESS");
            result.put("success", true);
        } catch (WebloggerException e) {
            log.error("starEntry FAILED: " + e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return SUCCESS;
    }

    public String unstarEntry() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                result.put("success", false);
                result.put("error", "Not authenticated");
                return SUCCESS;
            }
            WeblogEntry entry = WebloggerFactory.getWeblogger()
                    .getWeblogEntryManager().getWeblogEntry(entryId);
            if (entry == null) {
                result.put("success", false);
                result.put("error", "Entry not found");
                return SUCCESS;
            }
            // Facade pattern: use StarFacade to unstar entry
            starFacade.unstarEntry(user, entry);
            result.put("success", true);
        } catch (WebloggerException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return SUCCESS;
    }

    // -------------------------------------------------- Getters / setters

    public String getWeblogId() { return weblogId; }
    public void setWeblogId(String weblogId) { this.weblogId = weblogId; }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public Map<String, Object> getResult() { return result; }
}
