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

import java.util.List;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;

/**
 * Chain of Responsibility pipeline runner.
 *
 * Executes each ContentProcessingStep in order. Stops early if any step
 * sets the entry status to PENDING (spam quarantine) — subsequent cleanup
 * and transformation steps are skipped for quarantined posts.
 *
 * Step order:
 *   1. SpamDetectionStep    — cheapest, fail-fast quarantine
 *   2. HtmlSanitizationStep — must run before regex-based steps
 *   3. PiiScrubbingStep     — runs on clean HTML-free text
 *   4. ProfanityFilterStep  — runs last on fully cleaned text
 */
public class ContentProcessingPipeline {

    private final List<ContentProcessingStep> steps;

    public ContentProcessingPipeline(List<ContentProcessingStep> steps) {
        this.steps = steps;
    }

    /**
     * Run all registered steps against the given entry.
     * Stops immediately if any step quarantines the entry (PENDING status).
     *
     * @param entry the WeblogEntry about to be persisted
     * @throws WebloggerException if any step throws
     */
    public void process(WeblogEntry entry) throws WebloggerException {
        for (ContentProcessingStep step : steps) {
            step.process(entry);
            // If spam detection quarantined the post, stop the chain
            if (PubStatus.PENDING.equals(entry.getStatus())) {
                break;
            }
        }
    }

    public List<ContentProcessingStep> getSteps() {
        return steps;
    }
}
