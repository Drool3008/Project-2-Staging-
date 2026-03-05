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
package org.apache.roller.weblogger.ui.struts2.core;

import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * Displays the logged-in user's starred (favourited) blog entries with
 * simple page-by-page pagination.
 */
public class StarredEntries extends UIAction {

    private static final Log log = LogFactory.getLog(StarredEntries.class);

    /** Entries per page. */
    private static final int PAGE_SIZE = 10;

    /** Current page (0-based, passed as request param "page"). */
    private int page = 0;

    /** Entries on the current page. */
    private List<WeblogEntry> entries = Collections.emptyList();

    /** Total number of starred entries (used to compute page count). */
    private int totalEntries = 0;

    public StarredEntries() {
        this.pageTitle = "starredEntries.title";
    }

    /** No weblog required – this is a user-level page. */
    @Override
    public boolean isWeblogRequired() {
        return false;
    }

    @Override
    public String execute() {
        if (getAuthenticatedUser() == null) {
            return LOGIN;
        }
        try {
            List<WeblogEntry> all = WebloggerFactory.getWeblogger()
                    .getWeblogEntryManager()
                    .getStarredEntriesForUser(getAuthenticatedUser());
            totalEntries = all.size();
            int from = page * PAGE_SIZE;
            int to   = Math.min(from + PAGE_SIZE, totalEntries);
            if (from < totalEntries) {
                entries = all.subList(from, to);
            }
        } catch (WebloggerException e) {
            log.error("Error loading starred entries for user "
                    + getAuthenticatedUser().getUserName(), e);
            addError("generic.error.check.logs");
        }
        return SUCCESS;
    }

    // ---------- getters / setters ----------

    public List<WeblogEntry> getEntries() { return entries; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }

    public int getTotalEntries() { return totalEntries; }

    public int getPageSize() { return PAGE_SIZE; }

    public boolean hasPrevPage() { return page > 0; }
    public boolean hasNextPage() { return (page + 1) * PAGE_SIZE < totalEntries; }
    public int getPrevPage() { return page - 1; }
    public int getNextPage() { return page + 1; }
}
