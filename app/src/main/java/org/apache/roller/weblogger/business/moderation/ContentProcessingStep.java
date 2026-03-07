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
package org.apache.roller.weblogger.business.moderation;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Chain of Responsibility pattern.
 * Each moderation step implements this interface.
 * Steps are independent — no step knows about any other step.
 *
 * Implementations may modify entry.getText(), entry.setStatus(),
 * or entry.setTitle(). They must NOT call saveWeblogEntry() or flush.
 */
public interface ContentProcessingStep {
    /**
     * Process the entry. May modify entry.getText(), entry.setStatus(),
     * or entry.setTitle(). Must NOT call saveWeblogEntry() or flush.
     *
     * @param entry the WeblogEntry being saved — may be mutated in-place
     * @throws WebloggerException on processing failure
     */
    void process(WeblogEntry entry) throws WebloggerException;
}
