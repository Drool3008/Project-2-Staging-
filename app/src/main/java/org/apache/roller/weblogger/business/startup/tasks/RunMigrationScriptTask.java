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
import org.apache.roller.weblogger.business.startup.DatabaseScriptProvider;
import org.apache.roller.weblogger.business.startup.MigrationTask;
import org.apache.roller.weblogger.business.startup.SQLScriptRunner;
import org.apache.roller.weblogger.business.startup.StartupException;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration task that executes a SQL migration script file.
 * Extracted from DatabaseInstaller.upgradeTo400() to enable
 * independent testing and cleaner separation of concerns.
 */
public class RunMigrationScriptTask implements MigrationTask {
    
    private static Log log = LogFactory.getLog(RunMigrationScriptTask.class);
    
    private final DatabaseScriptProvider scripts;
    private final String scriptPath;
    private final boolean runScripts;
    private final List<String> messages = new ArrayList<>();
    
    /**
     * Create a new migration script execution task.
     * 
     * @param scripts Provider for loading database scripts
     * @param scriptPath Path to the migration script (e.g., "mysql/310-to-400-migration.sql")
     * @param runScripts Whether to actually execute the script (false for dry-run)
     */
    public RunMigrationScriptTask(DatabaseScriptProvider scripts, String scriptPath, boolean runScripts) {
        this.scripts = scripts;
        this.scriptPath = scriptPath;
        this.runScripts = runScripts;
    }
    
    @Override
    public void execute(Connection con) throws StartupException {
        // Extracted from DatabaseInstaller.upgradeTo400() lines 278-293
        SQLScriptRunner runner = null;
        try {
            if (runScripts) {
                successMessage("Running database upgrade script: " + scriptPath);
                runner = new SQLScriptRunner(scripts.getDatabaseScript(scriptPath));
                runner.runScript(con, true);
                messages.addAll(runner.getMessages());
            }
        } catch(Exception ex) {
            log.error("ERROR running database upgrade script: " + scriptPath, ex);
            if (runner != null) {
                messages.addAll(runner.getMessages());
            }
            
            errorMessage("Problem running migration script: " + scriptPath, ex);
            throw new StartupException("Problem running migration script: " + scriptPath, ex);
        }
    }
    
    @Override
    public String getName() {
        return "Run migration script: " + scriptPath;
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
