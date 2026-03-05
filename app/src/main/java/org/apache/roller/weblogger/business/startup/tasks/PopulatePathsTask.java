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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration task that populates full path columns for weblogcategory and folder tables.
 * Extracted from DatabaseInstaller.upgradeTo400() to enable independent testing
 * and cleaner separation of concerns.
 * 
 * This is the most complex migration task as it requires hierarchical tree traversal
 * using iterative SQL queries to build full paths for multi-level category/folder
 * structures (e.g., /parent/child/grandchild).
 */
public class PopulatePathsTask implements MigrationTask {
    
    private static Log log = LogFactory.getLog(PopulatePathsTask.class);
    
    private final List<String> messages = new ArrayList<>();
    
    @Override
    public void execute(Connection con) throws StartupException {
        // Extracted from DatabaseInstaller.upgradeTo400() lines 358-509
        try {
            successMessage("Populating path columns for weblogcategory and folder tables.");

            // Populate path in weblogcategory and folder tables.
            //
            // It would be nice if there was a simple sql solution for doing
            // this, but sadly the only real way to do it is through brute
            // force walking the hierarchical trees.  Luckily, it seems that
            // most people don't create multi-level hierarchies, so hopefully
            // this won't be too bad

            // === CATEGORY PATHS ===
            
            // set path to '/' for nodes with no parents (aka root nodes)
            PreparedStatement setRootPaths = con.prepareStatement(
                "update weblogcategory set path = '/' where parentid is NULL");
            setRootPaths.clearParameters();
            setRootPaths.executeUpdate();

            // select all nodes whose parent has no parent (aka 1st level nodes)
            PreparedStatement selectL1Children = con.prepareStatement(
                "select f.id, f.name from weblogcategory f, weblogcategory p "+
                    "where f.parentid = p.id and p.parentid is NULL");
            // update L1 nodes with their path (/<name>)
            PreparedStatement updateL1Children = con.prepareStatement(
                "update weblogcategory set path=? where id=?");
            ResultSet L1Set = selectL1Children.executeQuery();
            while (L1Set.next()) {
                String id = L1Set.getString(1);
                String name = L1Set.getString(2);
                updateL1Children.clearParameters();
                updateL1Children.setString( 1, "/"+name);
                updateL1Children.setString( 2, id);
                updateL1Children.executeUpdate();
            }

            // now for the complicated part =(
            // we need to keep iterating over L2, L3, etc nodes and setting
            // their path until all nodes have been updated.

            // select all nodes whose parent path has been set, excluding L1 nodes
            PreparedStatement selectLxChildren = con.prepareStatement(
                "select f.id, f.name, p.path from weblogcategory f, weblogcategory p "+
                    "where f.parentid = p.id and p.path <> '/' "+
                    "and p.path is not NULL and f.path is NULL");
            // update Lx nodes with their path (<parentPath>/<name>)
            PreparedStatement updateLxChildren = con.prepareStatement(
                "update weblogcategory set path=? where id=?");

            // this loop allows us to run this part of the upgrade process as
            // long as is necessary based on the depth of the hierarchy, and
            // we use the do/while construct to ensure it's run at least once
            int catNumCounted = 0;
            do {
                log.debug("Doing pass over Lx children for categories");

                // reset count for each iteration of outer loop
                catNumCounted = 0;

                ResultSet LxSet = selectLxChildren.executeQuery();
                while (LxSet.next()) {
                    String id = LxSet.getString(1);
                    String name = LxSet.getString(2);
                    String parentPath = LxSet.getString(3);
                    updateLxChildren.clearParameters();
                    updateLxChildren.setString( 1, parentPath+"/"+name);
                    updateLxChildren.setString( 2, id);
                    updateLxChildren.executeUpdate();

                    // count the updated rows
                    catNumCounted++;
                }

                log.debug("Updated "+catNumCounted+" Lx category paths");
            } while(catNumCounted > 0);


            // === FOLDER PATHS ===

            // set path to '/' for nodes with no parents (aka root nodes)
            setRootPaths = con.prepareStatement(
                "update folder set path = '/' where parentid is NULL");
            setRootPaths.clearParameters();
            setRootPaths.executeUpdate();

            // select all nodes whose parent has no parent (aka 1st level nodes)
            selectL1Children = con.prepareStatement(
                "select f.id, f.name from folder f, folder p "+
                    "where f.parentid = p.id and p.parentid is NULL");
            // update L1 nodes with their path (/<name>)
            updateL1Children = con.prepareStatement(
                "update folder set path=? where id=?");
            L1Set = selectL1Children.executeQuery();
            while (L1Set.next()) {
                String id = L1Set.getString(1);
                String name = L1Set.getString(2);
                updateL1Children.clearParameters();
                updateL1Children.setString( 1, "/"+name);
                updateL1Children.setString( 2, id);
                updateL1Children.executeUpdate();
            }

            // now for the complicated part =(
            // we need to keep iterating over L2, L3, etc nodes and setting
            // their path until all nodes have been updated.

            // select all nodes whose parent path has been set, excluding L1 nodes
            selectLxChildren = con.prepareStatement(
                "select f.id, f.name, p.path from folder f, folder p "+
                    "where f.parentid = p.id and p.path <> '/' "+
                    "and p.path is not NULL and f.path is NULL");
            // update Lx nodes with their path (/<name>)
            updateLxChildren = con.prepareStatement(
                "update folder set path=? where id=?");

            // this loop allows us to run this part of the upgrade process as
            // long as is necessary based on the depth of the hierarchy, and
            // we use the do/while construct to ensure it's run at least once
            int folderNumUpdated = 0;
            do {
                log.debug("Doing pass over Lx children for folders");

                // reset count for each iteration of outer loop
                folderNumUpdated = 0;

                ResultSet LxSet = selectLxChildren.executeQuery();
                while (LxSet.next()) {
                    String id = LxSet.getString(1);
                    String name = LxSet.getString(2);
                    String parentPath = LxSet.getString(3);
                    updateLxChildren.clearParameters();
                    updateLxChildren.setString( 1, parentPath+"/"+name);
                    updateLxChildren.setString( 2, id);
                    updateLxChildren.executeUpdate();

                    // count the updated rows
                    folderNumUpdated++;
                }

                log.debug("Updated "+folderNumUpdated+" Lx folder paths");
            } while(folderNumUpdated > 0);

            if (!con.getAutoCommit()) {
                con.commit();
            }

            successMessage("Done populating path columns.");

        } catch (SQLException e) {
            log.error("Problem upgrading database to version 320", e);
            throw new StartupException("Problem upgrading database to version 320", e);
        }
    }
    
    @Override
    public String getName() {
        return "Populate full paths for weblogcategory and folder hierarchies";
    }
    
    @Override
    public List<String> getMessages() {
        return messages;
    }
    
    private void successMessage(String msg) {
        messages.add(msg);
        log.trace(msg);
    }
}
