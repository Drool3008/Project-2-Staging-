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

package org.apache.roller.weblogger.business;

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.TestUtils;
import org.apache.roller.weblogger.business.runnable.ThreadManager;
import org.apache.roller.weblogger.pojos.TaskLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test TaskLock related business operations.
 */
public class TaskLockTest  {
    
    public static Log log = LogFactory.getLog(TaskLockTest.class);

    @BeforeEach
    public void setUp() throws Exception {
        // setup weblogger
        TestUtils.setupWeblogger();

        // registerLease requires a pre-existing TaskLock row in the DB with an
        // expired lease (the same bootstrap that ThreadManagerImpl.initialize()
        // normally performs). We always reset the row so the lease is expired,
        // even if a previous test left it in a locked state.
        ThreadManager mgr = WebloggerFactory.getWeblogger().getThreadManager();
        TestTask task = new TestTask();
        task.init();
        TaskLock taskLock = mgr.getTaskLockByName(task.getName());
        if (taskLock == null) {
            taskLock = new TaskLock();
            taskLock.setName(task.getName());
        }
        // Reset to expired state: timeAcquired=epoch, timeLeased=0 -> lease expired
        taskLock.setLastRun(new Date(0));
        taskLock.setTimeAcquired(new Date(0));
        taskLock.setTimeLeased(0);
        taskLock.setClientId(null);
        mgr.saveTaskLock(taskLock);
        // flush=true so the INSERT/UPDATE is committed before the test runs
        TestUtils.endSession(true);
    }

    @AfterEach
    public void tearDown() throws Exception {
    }
    
    
    /**
     * Test basic persistence operations ... Create, Update, Delete.
     * @throws Exception if one is raised
     */
    @Test
    public void testTaskLockCRUD() throws Exception {
        
        ThreadManager mgr = WebloggerFactory.getWeblogger().getThreadManager();
        
        // need a test task to play with
        TestTask task = new TestTask();
        task.init();
        
        // try to acquire a lock
        assertTrue(mgr.registerLease(task), "Failed to acquire lease.");
        // We don't flush here because registerLease should flush on its own
        TestUtils.endSession(false);
        
        // make sure task is locked
        assertFalse(mgr.registerLease(task),"Acquired lease a second time when we shouldn't have been able to.");
        TestUtils.endSession(false);
        
        // try to release a lock
        assertTrue(mgr.unregisterLease(task), "Release of lease failed.");
        // We don't flush here because unregisterLease should flush on its own
        TestUtils.endSession(false);

        // Current unregisterLease semantics are idempotent.  Double release should
        // actually succeed.
        assertTrue( mgr.unregisterLease(task), "Second release failed.");
        TestUtils.endSession(false);
    }
    
}
