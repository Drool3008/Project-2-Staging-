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
 * Migration task that populates parent IDs for weblogcategory and folder tables.
 * Extracted from DatabaseInstaller.upgradeTo400() to enable independent testing
 * and cleaner separation of concerns.
 * 
 * This task reads from association tables (weblogcategoryassoc and folderassoc)
 * and populates the parentid column in the main tables.
 */
public class PopulateParentidsTask implements MigrationTask {
    
    private static Log log = LogFactory.getLog(PopulateParentidsTask.class);
    
    private final List<String> messages = new ArrayList<>();
    
    @Override
    public void execute(Connection con) throws StartupException {
        // Extracted from DatabaseInstaller.upgradeTo400() lines 296-355
        try {
            successMessage("Populating parentid columns for weblogcategory and folder tables");

            // Populate parentid in weblogcategory and folder tables.
            //
            // We'd like to do something like the below, but few databases
            // support multiple table udpates, which are part of SQL-99
            //
            // update weblogcategory, weblogcategoryassoc
            //   set weblogcategory.parentid = weblogcategoryassoc.ancestorid
            //   where
            //      weblogcategory.id = weblogcategoryassoc.categoryid
            //      and weblogcategoryassoc.relation = 'PARENT';
            //
            // update folder,folderassoc
            //   set folder.parentid = folderassoc.ancestorid
            //   where
            //      folder.id = folderassoc.folderid
            //      and folderassoc.relation = 'PARENT';

            PreparedStatement selectParents = con.prepareStatement(
                "select categoryid, ancestorid from weblogcategoryassoc where relation='PARENT'");
            PreparedStatement updateParent = con.prepareStatement(
                "update weblogcategory set parentid=? where id=?");
            ResultSet parentSet = selectParents.executeQuery();
            while (parentSet.next()) {
                String categoryid = parentSet.getString(1);
                String parentid = parentSet.getString(2);
                updateParent.clearParameters();
                updateParent.setString( 1, parentid);
                updateParent.setString( 2, categoryid);
                updateParent.executeUpdate();
            }

            selectParents = con.prepareStatement(
                "select folderid, ancestorid from folderassoc where relation='PARENT'");
            updateParent = con.prepareStatement(
                "update folder set parentid=? where id=?");
            parentSet = selectParents.executeQuery();
            while (parentSet.next()) {
                String folderid = parentSet.getString(1);
                String parentid = parentSet.getString(2);
                updateParent.clearParameters();
                updateParent.setString( 1, parentid);
                updateParent.setString( 2, folderid);
                updateParent.executeUpdate();
            }

            if (!con.getAutoCommit()) {
                con.commit();
            }

            successMessage("Done populating parentid columns.");

        } catch (Exception e) {
            errorMessage("Problem upgrading database to version 320", e);
            throw new StartupException("Problem upgrading database to version 320", e);
        }
    }
    
    @Override
    public String getName() {
        return "Populate parent IDs for weblogcategory and folder tables";
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
