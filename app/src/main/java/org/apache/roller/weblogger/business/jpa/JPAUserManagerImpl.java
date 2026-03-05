
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

import java.sql.Timestamp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.WebloggerRuntimeException;
import org.apache.roller.weblogger.business.UserManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.pojos.GlobalPermission;
import org.apache.roller.weblogger.pojos.RollerPermission;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.UserRole;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogPermission;

@com.google.inject.Singleton
public class JPAUserManagerImpl implements UserManager {
    private static final Log log = LogFactory.getLog(JPAUserManagerImpl.class);

    private final JPAPersistenceStrategy strategy;

    // cached mapping of userNames -> userIds
    private final Map<String, String> userNameToIdMap = Collections.synchronizedMap(new HashMap<>());

    @com.google.inject.Inject
    protected JPAUserManagerImpl(JPAPersistenceStrategy strat) {
        log.debug("Instantiating JPA User Manager");
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

    // --------------------------------------------------------------- user CRUD

    @Override
    public void saveUser(User user) throws WebloggerException {
        perform(() -> doSaveUser(user));
    }

    private void doSaveUser(User user) {
        try {
            this.strategy.store(user);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public void removeUser(User user) throws WebloggerException {
        perform(() -> doRemoveUser(user));
    }

    private void doRemoveUser(User user) {
        String userName = user.getUserName();

        // remove permissions, maintaining both sides of relationship
        List<WeblogPermission> perms = doGetAllWeblogPermissions(user); // Use helper to avoid exception wrapping in
                                                                        // loop
        for (WeblogPermission perm : perms) {
            try {
                this.strategy.remove(perm);
            } catch (WebloggerException e) {
                throw new WebloggerRuntimeException(e);
            }
        }
        try {
            this.strategy.remove(user);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }

        // remove entry from cache mapping
        this.userNameToIdMap.remove(userName);
    }

    private List<WeblogPermission> doGetAllWeblogPermissions(User user) {
        // Internal version of getWeblogPermissions to avoid double wrapping
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery("WeblogPermission.getByUserName",
                    WeblogPermission.class);
            q.setParameter(1, user.getUserName());
            return q.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public void addUser(User newUser) throws WebloggerException {
        perform(() -> doAddUser(newUser));
    }

    private void doAddUser(User newUser) {

        if (newUser == null) {
            throw new WebloggerRuntimeException(new WebloggerException("cannot add null user"));
        }

        boolean adminUser = false;
        // Use internal doGetUsers to avoid exception wrapping
        List<User> existingUsers = this.doGetUsers(Boolean.TRUE, null, null, 0, 1);
        boolean firstUserAdmin = WebloggerConfig.getBooleanProperty("users.firstUserAdmin");
        if (existingUsers.isEmpty() && firstUserAdmin) {
            // Make first user an admin
            adminUser = true;

            // if user was disabled (because of activation user
            // account with e-mail property), enable it for admin user
            newUser.setEnabled(Boolean.TRUE);
            newUser.setActivationCode(null);
        }

        if (doGetUserByUserName(newUser.getUserName()) != null ||
                doGetUserByUserName(newUser.getUserName().toLowerCase()) != null) {
            throw new WebloggerRuntimeException(new WebloggerException("error.add.user.userNameInUse"));
        }

        try {
            this.strategy.store(newUser);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }

        doGrantRole("editor", newUser);
        if (adminUser) {
            doGrantRole("admin", newUser);
        }
    }

    @Override
    public User getUser(String id) throws WebloggerException {
        return query(() -> doGetUser(id));
    }

    private User doGetUser(String id) {
        try {
            return (User) this.strategy.load(User.class, id);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    // ------------------------------------------------------------ user queries

    @Override
    public User getUserByUserName(String userName) throws WebloggerException {
        return getUserByUserName(userName, Boolean.TRUE);
    }

    @Override
    public User getUserByOpenIdUrl(String openIdUrl) throws WebloggerException {
        return query(() -> doGetUserByOpenIdUrl(openIdUrl));
    }

    private User doGetUserByOpenIdUrl(String openIdUrl) {
        if (openIdUrl == null) {
            throw new WebloggerRuntimeException("OpenID URL cannot be null");
        }

        TypedQuery<User> query;
        User user;
        try {
            query = strategy.getNamedQuery(
                    "User.getByOpenIdUrl", User.class);
            query.setParameter(1, openIdUrl);
            try {
                user = query.getSingleResult();
            } catch (NoResultException e) {
                user = null;
            }
            return user;
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public User getUserByUserName(String userName, Boolean enabled)
            throws WebloggerException {
        return query(() -> doGetUserByUserName(userName, enabled));
    }

    private User doGetUserByUserName(String userName, Boolean enabled) {

        if (userName == null) {
            throw new WebloggerRuntimeException("userName cannot be null");
        }

        // check cache first
        // NOTE: if we ever allow changing usernames then this needs updating
        if (this.userNameToIdMap.containsKey(userName)) {

            User user = this.doGetUser(
                    this.userNameToIdMap.get(userName));
            if (user != null) {
                // only return the user if the enabled status matches
                if (enabled == null || enabled.equals(user.getEnabled())) {
                    log.debug("userNameToIdMap CACHE HIT - " + userName);
                    return user;
                }
            } else {
                // mapping hit with lookup miss? mapping must be old, remove it
                this.userNameToIdMap.remove(userName);
            }
        }

        // cache failed, do lookup
        TypedQuery<User> query;
        Object[] params;
        try {
            if (enabled != null) {
                query = strategy.getNamedQuery(
                        "User.getByUserName&Enabled", User.class);
                params = new Object[] { userName, enabled };
            } else {
                query = strategy.getNamedQuery(
                        "User.getByUserName", User.class);
                params = new Object[] { userName };
            }
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
            User user;
            try {
                user = query.getSingleResult();
            } catch (NoResultException e) {
                user = null;
            }

            // add mapping to cache
            if (user != null) {
                log.debug("userNameToIdMap CACHE MISS - " + userName);
                this.userNameToIdMap.put(user.getUserName(), user.getId());
            }

            return user;
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * Internal helper to avoid re-wrapping exceptions when calling from other
     * methods.
     */
    private User doGetUserByUserName(String userName) {
        return doGetUserByUserName(userName, Boolean.TRUE);
    }

    @Override
    public List<User> getUsers(Boolean enabled, Date startDate, Date endDate,
            int offset, int length)
            throws WebloggerException {
        return query(() -> doGetUsers(enabled, startDate, endDate, offset, length));
    }

    private List<User> doGetUsers(Boolean enabled, Date startDate, Date endDate,
            int offset, int length) {
        try {
            TypedQuery<User> query;

            Timestamp end = new Timestamp(endDate != null ? endDate.getTime() : new Date().getTime());

            if (enabled != null) {
                if (startDate != null) {
                    Timestamp start = new Timestamp(startDate.getTime());
                    query = strategy.getNamedQuery(
                            "User.getByEnabled&EndDate&StartDateOrderByStartDateDesc", User.class);
                    query.setParameter(1, enabled);
                    query.setParameter(2, end);
                    query.setParameter(3, start);
                } else {
                    query = strategy.getNamedQuery(
                            "User.getByEnabled&EndDateOrderByStartDateDesc", User.class);
                    query.setParameter(1, enabled);
                    query.setParameter(2, end);
                }
            } else {
                if (startDate != null) {
                    Timestamp start = new Timestamp(startDate.getTime());
                    query = strategy.getNamedQuery(
                            "User.getByEndDate&StartDateOrderByStartDateDesc", User.class);
                    query.setParameter(1, end);
                    query.setParameter(2, start);
                } else {
                    query = strategy.getNamedQuery(
                            "User.getByEndDateOrderByStartDateDesc", User.class);
                    query.setParameter(1, end);
                }
            }
            if (offset != 0) {
                query.setFirstResult(offset);
            }
            if (length != -1) {
                query.setMaxResults(length);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<User> getUsersStartingWith(String startsWith, Boolean enabled,
            int offset, int length) throws WebloggerException {
        return query(() -> doGetUsersStartingWith(startsWith, enabled, offset, length));
    }

    private List<User> doGetUsersStartingWith(String startsWith, Boolean enabled,
            int offset, int length) {
        try {
            TypedQuery<User> query;

            if (enabled != null) {
                if (startsWith != null) {
                    query = strategy.getNamedQuery(
                            "User.getByEnabled&UserNameOrEmailAddressStartsWith", User.class);
                    query.setParameter(1, enabled);
                    query.setParameter(2, startsWith + '%');
                    query.setParameter(3, startsWith + '%');
                } else {
                    query = strategy.getNamedQuery(
                            "User.getByEnabled", User.class);
                    query.setParameter(1, enabled);
                }
            } else {
                if (startsWith != null) {
                    query = strategy.getNamedQuery(
                            "User.getByUserNameOrEmailAddressStartsWith", User.class);
                    query.setParameter(1, startsWith + '%');
                } else {
                    query = strategy.getNamedQuery("User.getAll", User.class);
                }
            }
            if (offset != 0) {
                query.setFirstResult(offset);
            }
            if (length != -1) {
                query.setMaxResults(length);
            }
            return query.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public Map<String, Long> getUserNameLetterMap() throws WebloggerException {
        return query(() -> doGetUserNameLetterMap());
    }

    private Map<String, Long> doGetUserNameLetterMap() {
        String lc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Map<String, Long> results = new TreeMap<>();
        try {
            TypedQuery<Long> query = strategy.getNamedQuery(
                    "User.getCountByUserNameLike", Long.class);
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
    public List<User> getUsersByLetter(char letter, int offset, int length)
            throws WebloggerException {
        return query(() -> doGetUsersByLetter(letter, offset, length));
    }

    private List<User> doGetUsersByLetter(char letter, int offset, int length) {
        try {
            TypedQuery<User> query = strategy.getNamedQuery(
                    "User.getByUserNameOrderByUserName", User.class);
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

    /**
     * Get count of users, enabled only
     */
    @Override
    public long getUserCount() throws WebloggerException {
        return query(() -> doGetUserCount());
    }

    private long doGetUserCount() {
        try {
            TypedQuery<Long> q = strategy.getNamedQuery("User.getCountEnabledDistinct", Long.class);
            q.setParameter(1, Boolean.TRUE);
            List<Long> results = q.getResultList();
            return results.get(0);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public User getUserByActivationCode(String activationCode) throws WebloggerException {
        return query(() -> doGetUserByActivationCode(activationCode));
    }

    private User doGetUserByActivationCode(String activationCode) {
        if (activationCode == null) {
            throw new WebloggerRuntimeException("activationcode is null");
        }
        try {
            TypedQuery<User> q = strategy.getNamedQuery("User.getUserByActivationCode", User.class);
            q.setParameter(1, activationCode);
            try {
                return q.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    // -------------------------------------------------------- permissions CRUD

    @Override
    public boolean checkPermission(RollerPermission perm, User user) throws WebloggerException {
        return query(() -> doCheckPermission(perm, user));
    }

    private boolean doCheckPermission(RollerPermission perm, User user) {
        // if permission a weblog permission
        if (perm instanceof WeblogPermission) {
            // if user has specified permission in weblog return true
            WeblogPermission permToCheck = (WeblogPermission) perm;
            try {
                RollerPermission existingPerm = doGetWeblogPermission(permToCheck.getWeblog(), user);
                if (existingPerm != null && existingPerm.implies(perm)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        // if Blog Server admin would still have weblog permission above
        GlobalPermission globalPerm;
        try {
            globalPerm = new GlobalPermission(user);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        }

        if (globalPerm.implies(perm)) {
            return true;
        }

        if (log.isDebugEnabled()) {
            log.debug("PERM CHECK FAILED: user " + user.getUserName() + " does not have " + perm.toString());
        }
        return false;
    }

    @Override
    public WeblogPermission getWeblogPermission(Weblog weblog, User user) throws WebloggerException {
        return query(() -> doGetWeblogPermission(weblog, user));
    }

    private WeblogPermission doGetWeblogPermission(Weblog weblog, User user) {
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery("WeblogPermission.getByUserName&WeblogId",
                    WeblogPermission.class);
            q.setParameter(1, user.getUserName());
            q.setParameter(2, weblog.getHandle());
            try {
                return q.getSingleResult();
            } catch (NoResultException ignored) {
                return null;
            }
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public WeblogPermission getWeblogPermissionIncludingPending(Weblog weblog, User user) throws WebloggerException {
        return query(() -> doGetWeblogPermissionIncludingPending(weblog, user));
    }

    private WeblogPermission doGetWeblogPermissionIncludingPending(Weblog weblog, User user) {
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery(
                    "WeblogPermission.getByUserName&WeblogIdIncludingPending",
                    WeblogPermission.class);
            q.setParameter(1, user.getUserName());
            q.setParameter(2, weblog.getHandle());
            try {
                return q.getSingleResult();
            } catch (NoResultException ignored) {
                return null;
            }
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public void grantWeblogPermission(Weblog weblog, User user, List<String> actions) throws WebloggerException {
        perform(() -> doGrantWeblogPermission(weblog, user, actions));
    }

    private void doGrantWeblogPermission(Weblog weblog, User user, List<String> actions) {
        // first, see if user already has a permission for the specified object
        WeblogPermission existingPerm = doGetWeblogPermissionIncludingPending(weblog, user);

        try {
            // permission already exists, so add any actions specified in perm argument
            if (existingPerm != null) {
                existingPerm.addActions(actions);
                this.strategy.store(existingPerm);
            } else {
                // it's a new permission, so store it
                WeblogPermission perm = new WeblogPermission(weblog, user, actions);
                this.strategy.store(perm);
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public void grantWeblogPermissionPending(Weblog weblog, User user, List<String> actions) throws WebloggerException {
        perform(() -> doGrantWeblogPermissionPending(weblog, user, actions));
    }

    private void doGrantWeblogPermissionPending(Weblog weblog, User user, List<String> actions) {
        // first, see if user already has a permission for the specified object
        WeblogPermission existingPerm = doGetWeblogPermissionIncludingPending(weblog, user);

        // permission already exists, so complain
        if (existingPerm != null) {
            throw new WebloggerRuntimeException("Cannot make existing permission into pending permission");

        } else {
            // it's a new permission, so store it
            WeblogPermission perm = new WeblogPermission(weblog, user, actions);
            perm.setPending(true);
            try {
                this.strategy.store(perm);
            } catch (WebloggerException e) {
                throw new WebloggerRuntimeException(e);
            } catch (Exception e) {
                throw new WebloggerRuntimeException(e);
            }
        }
    }

    @Override
    public void confirmWeblogPermission(Weblog weblog, User user) throws WebloggerException {
        perform(() -> doConfirmWeblogPermission(weblog, user));
    }

    private void doConfirmWeblogPermission(Weblog weblog, User user) {
        // get specified permission
        WeblogPermission existingPerm = doGetWeblogPermissionIncludingPending(weblog, user);
        if (existingPerm == null) {
            throw new WebloggerRuntimeException("ERROR: permission not found");
        }
        // set pending to false
        existingPerm.setPending(false);
        try {
            this.strategy.store(existingPerm);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public void declineWeblogPermission(Weblog weblog, User user) throws WebloggerException {
        perform(() -> doDeclineWeblogPermission(weblog, user));
    }

    private void doDeclineWeblogPermission(Weblog weblog, User user) {
        // get specified permission
        WeblogPermission existingPerm = doGetWeblogPermissionIncludingPending(weblog, user);

        if (existingPerm == null) {
            throw new WebloggerRuntimeException("ERROR: permission not found");
        }
        // remove permission
        try {
            this.strategy.remove(existingPerm);
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public void revokeWeblogPermission(Weblog weblog, User user, List<String> actions) throws WebloggerException {
        perform(() -> doRevokeWeblogPermission(weblog, user, actions));
    }

    private void doRevokeWeblogPermission(Weblog weblog, User user, List<String> actions) {
        // get specified permission
        WeblogPermission oldperm = doGetWeblogPermissionIncludingPending(weblog, user);

        if (oldperm == null) {
            throw new WebloggerRuntimeException("ERROR: permission not found");
        }

        // remove actions specified in perm argument
        oldperm.removeActions(actions);

        try {
            if (oldperm.isEmpty()) {
                // no actions left in permission so remove it
                this.strategy.remove(oldperm);
            } else {
                // otherwise save it
                this.strategy.store(oldperm);
            }
        } catch (WebloggerException e) {
            throw new WebloggerRuntimeException(e);
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<WeblogPermission> getWeblogPermissions(User user) throws WebloggerException {
        return query(() -> doGetWeblogPermissions(user));
    }

    private List<WeblogPermission> doGetWeblogPermissions(User user) {
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery("WeblogPermission.getByUserName",
                    WeblogPermission.class);
            q.setParameter(1, user.getUserName());
            return q.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<WeblogPermission> getWeblogPermissions(Weblog weblog) throws WebloggerException {
        return query(() -> doGetWeblogPermissions(weblog));
    }

    private List<WeblogPermission> doGetWeblogPermissions(Weblog weblog) {
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery("WeblogPermission.getByWeblogId",
                    WeblogPermission.class);
            q.setParameter(1, weblog.getHandle());
            return q.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<WeblogPermission> getWeblogPermissionsIncludingPending(Weblog weblog) throws WebloggerException {
        return query(() -> doGetWeblogPermissionsIncludingPending(weblog));
    }

    private List<WeblogPermission> doGetWeblogPermissionsIncludingPending(Weblog weblog) {
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery("WeblogPermission.getByWeblogIdIncludingPending",
                    WeblogPermission.class);
            q.setParameter(1, weblog.getHandle());
            return q.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<WeblogPermission> getPendingWeblogPermissions(User user) throws WebloggerException {
        return query(() -> doGetPendingWeblogPermissions(user));
    }

    private List<WeblogPermission> doGetPendingWeblogPermissions(User user) {
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery("WeblogPermission.getByUserName&Pending",
                    WeblogPermission.class);
            q.setParameter(1, user.getUserName());
            return q.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    @Override
    public List<WeblogPermission> getPendingWeblogPermissions(Weblog weblog) throws WebloggerException {
        return query(() -> doGetPendingWeblogPermissions(weblog));
    }

    private List<WeblogPermission> doGetPendingWeblogPermissions(Weblog weblog) {
        try {
            TypedQuery<WeblogPermission> q = strategy.getNamedQuery("WeblogPermission.getByWeblogId&Pending",
                    WeblogPermission.class);
            q.setParameter(1, weblog.getHandle());
            return q.getResultList();
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    // -------------------------------------------------------------- role CRUD

    /**
     * Returns true if user has role specified.
     */
    @Override
    public boolean hasRole(String roleName, User user) throws WebloggerException {
        return query(() -> doHasRole(roleName, user));
    }

    private boolean doHasRole(String roleName, User user) {
        try {
            TypedQuery<UserRole> q = strategy.getNamedQuery("UserRole.getByUserNameAndRole", UserRole.class);
            q.setParameter(1, user.getUserName());
            q.setParameter(2, roleName);
            try {
                q.getSingleResult();
            } catch (NoResultException e) {
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * Get all of user's roles.
     */
    @Override
    public List<String> getRoles(User user) throws WebloggerException {
        return query(() -> doGetRoles(user));
    }

    private List<String> doGetRoles(User user) {
        try {
            TypedQuery<UserRole> q = strategy.getNamedQuery("UserRole.getByUserName", UserRole.class);
            q.setParameter(1, user.getUserName());
            List<UserRole> roles = q.getResultList();
            List<String> roleNames = new ArrayList<>();
            if (roles != null) {
                for (UserRole userRole : roles) {
                    roleNames.add(userRole.getRole());
                }
            }
            return roleNames;
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }

    /**
     * Grant to user role specified by role name.
     */
    @Override
    public void grantRole(String roleName, User user) throws WebloggerException {
        perform(() -> doGrantRole(roleName, user));
    }

    private void doGrantRole(String roleName, User user) {
        if (!doHasRole(roleName, user)) {
            UserRole role = new UserRole(user.getUserName(), roleName);
            try {
                this.strategy.store(role);
            } catch (WebloggerException e) {
                throw new WebloggerRuntimeException(e);
            } catch (Exception e) {
                throw new WebloggerRuntimeException(e);
            }
        }
    }

    @Override
    public void revokeRole(String roleName, User user) throws WebloggerException {
        perform(() -> doRevokeRole(roleName, user));
    }

    private void doRevokeRole(String roleName, User user) {
        try {
            TypedQuery<UserRole> q = strategy.getNamedQuery("UserRole.getByUserNameAndRole", UserRole.class);
            q.setParameter(1, user.getUserName());
            q.setParameter(2, roleName);
            try {
                UserRole role = q.getSingleResult();
                this.strategy.remove(role);

            } catch (NoResultException e) {
                throw new WebloggerRuntimeException("ERROR: removing role", e);
            }
        } catch (Exception e) {
            throw new WebloggerRuntimeException(e);
        }
    }
}
