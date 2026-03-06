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
package org.apache.roller.weblogger.business.star;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;

/**
 * Command pattern: Executes star commands with logging.
 * This class is part of the Command design pattern implementation.
 */
public class StarCommandExecutor {

    private static final Log LOG = LogFactory.getLog(StarCommandExecutor.class);

    /**
     * Executes the given star command and logs the operation.
     *
     * @param command the star command to execute
     * @throws WebloggerException if the command fails
     */
    public void execute(StarCommand command) throws WebloggerException {
        LOG.debug("Executing: " + command.getDescription());
        command.execute();
        LOG.info("Completed: " + command.getDescription());
    }
}
