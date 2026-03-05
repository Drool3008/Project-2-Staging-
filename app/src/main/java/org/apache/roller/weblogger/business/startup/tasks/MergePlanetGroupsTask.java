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
 * Migration task that merges planet groups 'external' into 'all'.
 * Extracted from DatabaseInstaller.upgradeTo400() to enable independent testing
 * and cleaner separation of concerns.
 * 
 * This task handles the Roller 4.0 planet data model changes by consolidating
 * duplicate planet groups. If both 'all' and 'external' groups exist, subscriptions
 * are moved from 'external' to 'all' and 'external' is deleted. If only 'external'
 * exists, it is renamed to 'all'.
 */
public class MergePlanetGroupsTask implements MigrationTask {
    
    private static Log log = LogFactory.getLog(MergePlanetGroupsTask.class);
    
    private final List<String> messages = new ArrayList<>();
    
    @Override
    public void execute(Connection con) throws StartupException {
        // Extracted from DatabaseInstaller.upgradeTo400() lines 510-563
        // 4.0 changes the planet data model a bit, so we need to clean that up
        try {
            successMessage("Merging planet groups 'all' and 'external'");

            // Move all subscriptions in the planet group 'external' to group 'all'

            String allGroupId = null;
            PreparedStatement selectAllGroupId = con.prepareStatement(
                "select id from rag_group where handle = 'all'");
            ResultSet rs = selectAllGroupId.executeQuery();
            if (rs.next()) {
                allGroupId = rs.getString(1);
            }

            String externalGroupId = null;
            PreparedStatement selectExternalGroupId = con.prepareStatement(
                "select id from rag_group where handle = 'external'");
            rs = selectExternalGroupId.executeQuery();
            if (rs.next()) {
                externalGroupId = rs.getString(1);
            }

            // we only need to merge if both of those groups already existed
            if(allGroupId != null && externalGroupId != null) {
                PreparedStatement updateGroupSubs = con.prepareStatement(
                        "update rag_group_subscription set group_id = ? where group_id = ?");
                updateGroupSubs.clearParameters();
                updateGroupSubs.setString( 1, allGroupId);
                updateGroupSubs.setString( 2, externalGroupId);
                updateGroupSubs.executeUpdate();

                // we no longer need the group 'external'
                PreparedStatement deleteExternalGroup = con.prepareStatement(
                        "delete from rag_group where handle = 'external'");
                deleteExternalGroup.executeUpdate();

            // if we only have group 'external' then just rename it to 'all'
            } else if(allGroupId == null && externalGroupId != null) {

                // rename 'external' to 'all'
                PreparedStatement renameExternalGroup = con.prepareStatement(
                        "update rag_group set handle = 'all' where handle = 'external'");
                renameExternalGroup.executeUpdate();
            }

            if (!con.getAutoCommit()) {
                con.commit();
            }

            successMessage("Planet group 'external' merged into group 'all'.");

        } catch (Exception e) {
            errorMessage("Problem upgrading database to version 400", e);
            throw new StartupException("Problem upgrading database to version 400", e);
        }
    }
    
    @Override
    public String getName() {
        return "Merge planet groups 'external' into 'all'";
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
