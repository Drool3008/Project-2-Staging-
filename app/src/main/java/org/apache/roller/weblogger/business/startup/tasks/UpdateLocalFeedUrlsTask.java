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

package org.apache.roller.weblogger.business.startup.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.startup.MigrationTask;
import org.apache.roller.weblogger.business.startup.StartupException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration task that updates local planet subscription feeds to new feed URL format.
 * Extracted from DatabaseInstaller.upgradeTo400() to enable independent testing
 * and cleaner separation of concerns.
 * 
 * This task converts local feed URLs from absolute URL format to the new
 * weblogger:handle format introduced in Roller 4.0.
 */
public class UpdateLocalFeedUrlsTask implements MigrationTask {
    
    private static Log log = LogFactory.getLog(UpdateLocalFeedUrlsTask.class);
    
    private final List<String> messages = new ArrayList<>();
    
    @Override
    public void execute(Connection con) throws StartupException {
        // Extracted from DatabaseInstaller.upgradeTo400() lines 567-612
        // update local planet subscriptions to use new local feed format
        try {
            successMessage("Upgrading local planet subscription feeds to new feed url format");

            // need to start by looking up absolute site url
            PreparedStatement selectAbsUrl =
                    con.prepareStatement("select value from roller_properties where name = 'site.absoluteurl'");
            String absUrl = null;
            ResultSet rs = selectAbsUrl.executeQuery();
            if(rs.next()) {
                absUrl = rs.getString(1);
            }

            if(absUrl != null && absUrl.length() > 0) {
                PreparedStatement selectSubs =
                        con.prepareStatement("select id,feed_url,author from rag_subscription");

            PreparedStatement updateSubUrl =
                    con.prepareStatement("update rag_subscription set last_updated=last_updated, feed_url = ? where id = ?");

            ResultSet rset = selectSubs.executeQuery();
            while (rset.next()) {
                String id = rset.getString(1);
                String feed_url = rset.getString(2);
                String handle = rset.getString(3);

                // only work on local feed urls
                if (feed_url.startsWith(absUrl)) {
                    // update feed_url to 'weblogger:<handle>'
                    updateSubUrl.clearParameters();
                    updateSubUrl.setString( 1, "weblogger:"+handle);
                    updateSubUrl.setString( 2, id);
                    updateSubUrl.executeUpdate();
                }
            }
            }

            if (!con.getAutoCommit()) {
                con.commit();
            }

            // Note: Original code had inconsistent success message here
            // (said "Comments successfully updated..." but this is feed URL section)
            // Preserving exact behavior for now
            successMessage("Comments successfully updated to use new comment plugins.");

        } catch (Exception e) {
            errorMessage("Problem upgrading database to version 400", e);
            throw new StartupException("Problem upgrading database to version 400", e);
        }
    }
    
    @Override
    public String getName() {
        return "Update local planet subscription feeds to weblogger:handle format";
    }
    
    @Override
    public List<String> getMessages() {
        return messages;
    }
    
    private void errorMessage(String msg, Throwable t) {
        messages.add(msg);
        log.error(msg, t);
    }
    
    private void successMessage(String msg) {
        messages.add(msg);
        log.trace(msg);
    }
}
