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
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Chain of Responsibility — Step 4 (final step).
 *
 * Runs LAST because:
 * - HTML is already stripped (Step 2) — no tags interfering with word matching.
 * - PII is already redacted (Step 3) — no phone/email substrings causing false
 *   positives when a banned word appears inside a larger token.
 * - Clean plain text = accurate word-boundary regex matching.
 *
 * Each banned word is matched case-insensitively with \b word-boundary anchors,
 * so substrings are NOT matched (e.g. checking "ass" will NOT flag "class").
 *
 * Replacement: each character of the banned word is replaced with '*'.
 * e.g. a 4-letter word → "****"
 *
 * Does NOT reject or quarantine the post — purely transformative.
 */
public class ProfanityFilterStep implements ContentProcessingStep {

    private static final Log log = LogFactory.getLog(ProfanityFilterStep.class);

    private final ModerationConfig config;

    public ProfanityFilterStep(ModerationConfig config) {
        this.config = config;
    }

    @Override
    public void process(WeblogEntry entry) throws WebloggerException {
        if (!config.isProfanityStepEnabled()) {
            return;
        }

        List<String> bannedWords = config.getBannedWords();
        if (bannedWords == null || bannedWords.isEmpty()) {
            return;
        }

        String text = entry.getText() != null ? entry.getText() : "";

        for (String word : bannedWords) {
            if (word == null || word.trim().isEmpty()) {
                continue;
            }
            String trimmed     = word.trim();
            String regex       = "(?i)\\b" + Pattern.quote(trimmed) + "\\b";
            String replacement = "*".repeat(trimmed.length());
            text = text.replaceAll(regex, replacement);
        }

        entry.setText(text);
        log.debug("ProfanityFilterStep: processed entry '" + entry.getTitle() + "'");
    }
}
