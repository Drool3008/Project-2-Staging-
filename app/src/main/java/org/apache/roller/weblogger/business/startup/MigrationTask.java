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

package org.apache.roller.weblogger.business.startup;

import java.sql.Connection;
import java.util.List;

/**
 * Represents a single database migration step/task.
 * Each task encapsulates a specific migration operation that can be
 * executed independently, tested in isolation, and composed into a
 * larger migration sequence.
 */
public interface MigrationTask {
    
    /**
     * Execute this migration step against the database connection.
     * 
     * @param con Database connection (should have autoCommit=false)
     * @throws StartupException if migration step fails
     */
    void execute(Connection con) throws StartupException;
    
    /**
     * Get a human-readable name for this migration task.
     * Used for logging and error reporting.
     * 
     * @return Name of this migration task
     */
    String getName();
    
    /**
     * Get messages generated during task execution.
     * Includes both success and error messages.
     * 
     * @return List of messages from this task's execution
     */
    List<String> getMessages();
}
