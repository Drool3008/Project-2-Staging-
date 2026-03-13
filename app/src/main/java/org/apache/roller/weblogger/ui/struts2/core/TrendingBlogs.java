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
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * Displays the top 5 trending blog posts and blog pages ranked by the number
 * of users who have starred (liked) them.
 *
 * Trending data is fetched with two aggregate GROUP BY JPQL queries —
 * no per-article iteration in application code.
 */
public class TrendingBlogs extends UIAction {

    private static final Log log = LogFactory.getLog(TrendingBlogs.class);

    /** Top 5 trending WeblogEntries: each row is Object[]{WeblogEntry, Long starCount} */
    private List<Object[]> trendingEntries = new ArrayList<>();

    /** Top 5 trending Weblogs: each row is Object[]{Weblog, Long starCount} */
    private List<Object[]> trendingWeblogs = new ArrayList<>();

    public TrendingBlogs() {
        this.pageTitle = "trendingBlogs.title";
    }

    /** TrendingBlogs does not require a weblog context. */
    @Override
    public boolean isWeblogRequired() {
        return false;
    }

    @Override
    public String execute() {
        try {
            // Fetch top 5 blog posts (WeblogEntries) by star count —
            // single aggregate GROUP BY query, no application-level iteration.
            trendingEntries = WebloggerFactory.getWeblogger()
                    .getWeblogEntryManager()
                    .getTrendingEntries(5);
        } catch (WebloggerException e) {
            log.error("Error fetching trending entries", e);
        }

        try {
            // Fetch top 5 blog pages (Weblogs) by star count —
            // single aggregate GROUP BY query, no application-level iteration.
            trendingWeblogs = WebloggerFactory.getWeblogger()
                    .getWeblogManager()
                    .getTrendingWeblogs(5);
        } catch (WebloggerException e) {
            log.error("Error fetching trending weblogs", e);
        }

        return SUCCESS;
    }

    // ------------------------------------------------------------------ Getters

    /**
     * Returns up to 5 Object[] rows for the top trending blog posts.
     * Each row: [0] = WeblogEntry, [1] = Long starCount.
     */
    public List<Object[]> getTrendingEntries() {
        return trendingEntries;
    }

    /**
     * Returns up to 5 Object[] rows for the top trending blog pages.
     * Each row: [0] = Weblog, [1] = Long starCount.
     */
    public List<Object[]> getTrendingWeblogs() {
        return trendingWeblogs;
    }
}
