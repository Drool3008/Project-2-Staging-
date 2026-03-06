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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.StarFacade;
import org.apache.roller.weblogger.pojos.StarredWeblogEntry;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * Displays the logged-in user's starred (favourited) weblogs with pagination.
 * This shows the same weblogs as in the main menu starred blogs section.
 * Uses the Facade design pattern via StarFacade for star operations.
 */
public class StarredEntries extends UIAction {

    private static final Log log = LogFactory.getLog(StarredEntries.class);

    /** Weblogs per page. */
    private static final int PAGE_SIZE = 5;

    /** Current page (0-based, passed as request param "page"). */
    private int page = 0;

    /** All starred weblogs for the user. */
    private List<StarredWeblogEntry> allStarredWeblogs = new ArrayList<>();

    /** Starred weblogs for the current page. */
    private List<StarredWeblogEntry> starredWeblogs = new ArrayList<>();

    /** Total number of starred weblogs. */
    private int totalWeblogs = 0;

    // Facade pattern: single point of access for star operations
    private final StarFacade starFacade = new StarFacade();

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
            // Facade pattern: use StarFacade to get starred weblogs
            allStarredWeblogs = starFacade.getStarredWeblogsSortedByRecency(getAuthenticatedUser());
            
            totalWeblogs = allStarredWeblogs.size();
            
            if (totalWeblogs > 0) {
                // Clamp page to valid range
                int maxPage = Math.max(0, (totalWeblogs - 1) / PAGE_SIZE);
                if (page > maxPage) {
                    page = maxPage;
                }
                if (page < 0) {
                    page = 0;
                }
                
                // Get the subset for the current page
                int startIndex = page * PAGE_SIZE;
                int endIndex = Math.min(startIndex + PAGE_SIZE, totalWeblogs);
                starredWeblogs = allStarredWeblogs.subList(startIndex, endIndex);
            }
            
            log.info("Loaded " + starredWeblogs.size() + " starred weblogs (page " + page 
                    + " of " + getTotalPages() + ") for user " + getAuthenticatedUser().getUserName());
        } catch (WebloggerException e) {
            log.error("Error loading starred weblogs for user "
                    + getAuthenticatedUser().getUserName(), e);
            addError("generic.error.check.logs");
        }
        return SUCCESS;
    }

    // ---------- getters / setters ----------

    public List<StarredWeblogEntry> getStarredWeblogs() { 
        return starredWeblogs; 
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }

    public int getTotalWeblogs() { 
        return totalWeblogs; 
    }

    public int getPageSize() { return PAGE_SIZE; }

    public int getTotalPages() { 
        return totalWeblogs == 0 ? 1 : (totalWeblogs + PAGE_SIZE - 1) / PAGE_SIZE; 
    }

    public boolean isHasPrevPage() { return page > 0; }
    public boolean isHasNextPage() { return (long)(page + 1) * PAGE_SIZE < totalWeblogs; }

    public int getPrevPage() { return page - 1; }
    public int getNextPage() { return page + 1; }
}
