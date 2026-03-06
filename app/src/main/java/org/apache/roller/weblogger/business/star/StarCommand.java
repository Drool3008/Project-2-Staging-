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

import org.apache.roller.weblogger.WebloggerException;

/**
 * Command pattern: Interface for star/unstar operations.
 * Encapsulates a star/unstar request as an object, allowing for
 * logging, history tracking, and pre/post behavior hooks.
 */
public interface StarCommand {

    /**
     * Execute the star/unstar operation.
     * @throws WebloggerException if the operation fails
     */
    void execute() throws WebloggerException;

    /**
     * Get a human-readable description of this command.
     * @return description e.g. "Star weblog 'my-blog' by user 'alice'"
     */
    String getDescription();
}
