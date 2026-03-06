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
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.StarredWeblogEntry;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * Displays the logged-in user's starred (favourited) weblogs.
 * This shows the same weblogs as in the main menu starred blogs section.
 */
public class StarredEntries extends UIAction {

    private static final Log log = LogFactory.getLog(StarredEntries.class);

    /** Starred weblogs for the user. */
    private List<StarredWeblogEntry> starredWeblogs = new ArrayList<>();

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
            // Get starred weblogs - same as MainMenu
            starredWeblogs = WebloggerFactory.getWeblogger()
                    .getWeblogManager()
                    .getStarredWeblogsSortedByRecency(getAuthenticatedUser());
            log.info("Loaded " + starredWeblogs.size() + " starred weblogs for user " 
                    + getAuthenticatedUser().getUserName());
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

    public int getTotalWeblogs() { 
        return starredWeblogs.size(); 
    }
}
