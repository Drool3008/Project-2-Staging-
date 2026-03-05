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
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.ui.core.RollerSession;
import org.apache.struts2.interceptor.ServletRequestAware;

/**
 * Struts2 action that handles star / unstar requests for Weblogs and
 * WeblogEntries. Returns a JSON result (via struts2-json-plugin).
 */
public class StarAction extends ActionSupport implements ServletRequestAware {

    private static final long serialVersionUID = 1L;

    private HttpServletRequest servletRequest;

    private String weblogId;
    private String entryId;
    private Map<String, Object> result = new HashMap<>();

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
            WebloggerFactory.getWeblogger().getWeblogManager()
                    .starWeblog(user, weblog);
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
            WebloggerFactory.getWeblogger().getWeblogManager()
                    .unstarWeblog(user, weblog);
            result.put("success", true);
        } catch (WebloggerException e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return SUCCESS;
    }

    public String starEntry() {
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
            WebloggerFactory.getWeblogger().getWeblogEntryManager()
                    .starEntry(user, entry);
            result.put("success", true);
        } catch (WebloggerException e) {
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
            WebloggerFactory.getWeblogger().getWeblogEntryManager()
                    .unstarEntry(user, entry);
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
