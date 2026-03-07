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

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;

/**
 * Chain of Responsibility — Step 1.
 *
 * Checks for spam signals FIRST because:
 * - If the post is spam, running the remaining 3 steps is wasteful (fail fast).
 * - This is the cheapest check: no HTML parsing, no external calls, pure regex.
 *
 * Spam signals checked (in order):
 *   1. Presence of known spam trigger phrases (case-insensitive substring match)
 *   2. External link density exceeds configurable threshold (links / words)
 *   3. Excessive ALL-CAPS word ratio exceeds configurable threshold
 *
 * If any signal is found, the entry status is set to PubStatus.PENDING
 * (quarantined for admin review). The post is NOT deleted.
 *
 * When status becomes PENDING the pipeline stops — Steps 2-4 are skipped.
 */
public class SpamDetectionStep implements ContentProcessingStep {

    private static final Log log = LogFactory.getLog(SpamDetectionStep.class);

    private static final Pattern EXTERNAL_LINK_PATTERN =
        Pattern.compile("href=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private final ModerationConfig config;

    public SpamDetectionStep(ModerationConfig config) {
        this.config = config;
    }

    @Override
    public void process(WeblogEntry entry) throws WebloggerException {
        if (!config.isSpamStepEnabled()) {
            return;
        }

        String body      = entry.getText() != null ? entry.getText() : "";
        String lowerBody = body.toLowerCase();
        String[] words   = body.trim().isEmpty() ? new String[0] : body.split("\\s+");
        int wordCount    = words.length;

        // Check 1: spam trigger phrases
        for (String phrase : config.getSpamPhrases()) {
            if (lowerBody.contains(phrase.toLowerCase())) {
                flagPost(entry, "Spam phrase detected: '" + phrase + "'");
                return;
            }
        }

        // Check 2: external link density
        if (wordCount > 0) {
            Matcher m = EXTERNAL_LINK_PATTERN.matcher(body);
            int linkCount = 0;
            while (m.find()) {
                linkCount++;
            }
            double density = (double) linkCount / wordCount;
            if (density > config.getLinkDensityThreshold()) {
                flagPost(entry, "High link density: " + linkCount
                        + " links / " + wordCount + " words = "
                        + String.format("%.2f", density));
                return;
            }
        }

        // Check 3: excessive ALL-CAPS words
        if (wordCount > 0) {
            long capsCount = Arrays.stream(words)
                .filter(w -> w.length() > 2
                          && w.equals(w.toUpperCase())
                          && w.matches("[A-Z]+"))
                .count();
            double capsRatio = (double) capsCount / wordCount;
            if (capsRatio > config.getCapsRatioThreshold()) {
                flagPost(entry, "Excessive CAPS: "
                        + String.format("%.0f", capsRatio * 100) + "% words in CAPS");
                return;
            }
        }

        log.debug("SpamDetectionStep: passed for entry '" + entry.getTitle() + "'");
    }

    private void flagPost(WeblogEntry entry, String reason) {
        log.warn("SpamDetectionStep: FLAGGED '" + entry.getTitle() + "' — " + reason);
        entry.setStatus(PubStatus.PENDING);
    }
}
