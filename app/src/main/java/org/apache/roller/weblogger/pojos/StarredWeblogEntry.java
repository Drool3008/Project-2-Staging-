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
package org.apache.roller.weblogger.pojos;

import java.util.Date;

/**
 * Represents a starred (favourited) Weblog together with the publish time
 * of its most recent WeblogEntry, used for display on the user's home page.
 */
public class StarredWeblogEntry {

    private final Weblog weblog;
    private final Date latestPostTime;

    public StarredWeblogEntry(Weblog weblog, Date latestPostTime) {
        this.weblog = weblog;
        this.latestPostTime = latestPostTime;
    }

    public Weblog getWeblog() {
        return weblog;
    }

    public Date getLatestPostTime() {
        return latestPostTime;
    }
}
