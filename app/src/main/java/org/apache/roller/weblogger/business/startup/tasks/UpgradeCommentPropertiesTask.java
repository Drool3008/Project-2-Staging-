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
 * Migration task that upgrades existing comments to use new plugin mechanism.
 * Extracted from DatabaseInstaller.upgradeTo400() to enable independent testing
 * and cleaner separation of concerns.
 * 
 * This task handles the Roller 4.0 comment system changes by:
 * 1. Querying existing comment property settings (autoformat, escapehtml)
 * 2. Determining appropriate content-type and plugins for existing comments
 * 3. Updating all existing comments with new content-type and plugins
 * 4. Setting new comment configuration properties
 */
public class UpgradeCommentPropertiesTask implements MigrationTask {
    
    private static Log log = LogFactory.getLog(UpgradeCommentPropertiesTask.class);
    
    private final List<String> messages = new ArrayList<>();
    
    @Override
    public void execute(Connection con) throws StartupException {
        // Extracted from DatabaseInstaller.upgradeTo400() lines 616-705
        // upgrade comments to use new plugin mechanism
        try {
            successMessage("Upgrading existing comments with content-type & plugins");

            // look in db and see if comment autoformatting is enabled
            boolean autoformatEnabled = false;
            String autoformat = null;
            PreparedStatement selectIsAutoformtEnabled = con.prepareStatement(
                "select value from roller_properties where name = 'users.comments.autoformat'");
            ResultSet rs = selectIsAutoformtEnabled.executeQuery();
            if (rs.next()) {
                autoformat = rs.getString(1);
                if(autoformat != null && "true".equals(autoformat)) {
                    autoformatEnabled = true;
                }
            }

            // look in db and see if comment html escaping is enabled
            boolean htmlEnabled = false;
            String escapehtml = null;
            PreparedStatement selectIsEscapehtmlEnabled = con.prepareStatement(
                "select value from roller_properties where name = 'users.comments.escapehtml'");
            ResultSet rs1 = selectIsEscapehtmlEnabled.executeQuery();
            if (rs1.next()) {
                escapehtml = rs1.getString(1);
                // NOTE: we allow html only when html escaping is OFF
                if(escapehtml != null && !"true".equals(escapehtml)) {
                    htmlEnabled = true;
                }
            }

            // first lets set the new 'users.comments.htmlenabled' property
            PreparedStatement addCommentHtmlProp = con.prepareStatement("insert into roller_properties(name,value) values(?,?)");
            addCommentHtmlProp.clearParameters();
            addCommentHtmlProp.setString(1, "users.comments.htmlenabled");
            if(htmlEnabled) {
                addCommentHtmlProp.setString(2, "true");
            } else {
                addCommentHtmlProp.setString(2, "false");
            }
            addCommentHtmlProp.executeUpdate();

            // determine content-type for existing comments
            String contentType = "text/plain";
            if(htmlEnabled) {
                contentType = "text/html";
            }

            // determine plugins for existing comments
            String plugins = "";
            if(htmlEnabled && autoformatEnabled) {
                plugins = "HTMLSubset,AutoFormat";
            } else if(htmlEnabled) {
                plugins = "HTMLSubset";
            } else if(autoformatEnabled) {
                plugins = "AutoFormat";
            }

            // set new comment plugins configuration property 'users.comments.plugins'
            PreparedStatement addCommentPluginsProp =
                    con.prepareStatement("insert into roller_properties(name,value) values(?,?)");
            addCommentPluginsProp.clearParameters();
            addCommentPluginsProp.setString(1, "users.comments.plugins");
            addCommentPluginsProp.setString(2, plugins);
            addCommentPluginsProp.executeUpdate();

            // set content-type for all existing comments
            PreparedStatement updateCommentsContentType =
                    con.prepareStatement("update roller_comment set posttime=posttime, contenttype = ?");
            updateCommentsContentType.clearParameters();
            updateCommentsContentType.setString(1, contentType);
            updateCommentsContentType.executeUpdate();

            // set plugins for all existing comments
            PreparedStatement updateCommentsPlugins =
                    con.prepareStatement("update roller_comment set posttime=posttime, plugins = ?");
            updateCommentsPlugins.clearParameters();
            updateCommentsPlugins.setString(1, plugins);
            updateCommentsPlugins.executeUpdate();

            if (!con.getAutoCommit()) {
                con.commit();
            }

            successMessage("Comments successfully updated to use new comment plugins.");

        } catch (Exception e) {
            errorMessage("Problem upgrading database to version 400", e);
            throw new StartupException("Problem upgrading database to version 400", e);
        }
    }
    
    @Override
    public String getName() {
        return "Upgrade comment content-type and plugins based on roller properties";
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
