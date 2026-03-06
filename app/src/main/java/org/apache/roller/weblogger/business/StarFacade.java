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
package org.apache.roller.weblogger.business;

import java.util.List;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.StarredWeblogEntry;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Facade pattern: Provides a unified interface to star/unstar operations
 * for both Weblogs and WeblogEntries. This hides the complexity of which
 * manager (WeblogManager vs WeblogEntryManager) handles which operation.
 */
public class StarFacade {

    /**
     * Star a weblog for a user (add to favourites).
     * Delegates to WeblogManager.starWeblog().
     */
    public void starWeblog(User user, Weblog weblog) throws WebloggerException {
        WebloggerFactory.getWeblogger().getWeblogManager().starWeblog(user, weblog);
    }

    /**
     * Unstar a weblog for a user (remove from favourites).
     * Delegates to WeblogManager.unstarWeblog().
     */
    public void unstarWeblog(User user, Weblog weblog) throws WebloggerException {
        WebloggerFactory.getWeblogger().getWeblogManager().unstarWeblog(user, weblog);
    }

    /**
     * Star a weblog entry for a user (add to favourites).
     * Delegates to WeblogEntryManager.starEntry().
     */
    public void starEntry(User user, WeblogEntry entry) throws WebloggerException {
        WebloggerFactory.getWeblogger().getWeblogEntryManager().starEntry(user, entry);
    }

    /**
     * Unstar a weblog entry for a user (remove from favourites).
     * Delegates to WeblogEntryManager.unstarEntry().
     */
    public void unstarEntry(User user, WeblogEntry entry) throws WebloggerException {
        WebloggerFactory.getWeblogger().getWeblogEntryManager().unstarEntry(user, entry);
    }

    /**
     * Check if a weblog is starred by a user.
     * Delegates to WeblogManager.isWeblogStarredByUser().
     */
    public boolean isWeblogStarred(User user, Weblog weblog) throws WebloggerException {
        return WebloggerFactory.getWeblogger().getWeblogManager()
                .isWeblogStarredByUser(user, weblog);
    }

    /**
     * Check if a weblog entry is starred by a user.
     * Delegates to WeblogEntryManager.isEntryStarredByUser().
     */
    public boolean isEntryStarred(User user, WeblogEntry entry) throws WebloggerException {
        return WebloggerFactory.getWeblogger().getWeblogEntryManager()
                .isEntryStarredByUser(user, entry);
    }

    /**
     * Get starred weblogs for a user, sorted by most-recent entry pubTime descending.
     * Delegates to WeblogManager.getStarredWeblogsSortedByRecency().
     */
    public List<StarredWeblogEntry> getStarredWeblogsSortedByRecency(User user) throws WebloggerException {
        return WebloggerFactory.getWeblogger().getWeblogManager()
                .getStarredWeblogsSortedByRecency(user);
    }

    /**
     * Get all WeblogEntries starred by the given user.
     * Delegates to WeblogEntryManager.getStarredEntriesForUser().
     */
    public List<WeblogEntry> getStarredEntriesForUser(User user) throws WebloggerException {
        return WebloggerFactory.getWeblogger().getWeblogEntryManager()
                .getStarredEntriesForUser(user);
    }
}
