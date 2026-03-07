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

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Chain of Responsibility — Step 3.
 *
 * Runs THIRD (after HTML is sanitized) because:
 * - HTML tags can hide PII inside attributes (e.g. href="tel:+91-98765-43210",
 *   href="mailto:user@example.com") — sanitizing first ensures those are gone.
 * - After Step 2, text is clean and regex patterns match accurately without
 *   false positives from embedded tag content.
 *
 * Redacts the following PII patterns inline:
 *   - Credit/debit card numbers  (4×4 digits, space/dash separated)
 *   - Aadhaar-style IDs          (12 digits in groups of 4)
 *   - Email addresses
 *   - Phone numbers              (Indian + international formats)
 *
 * Replaces each match with a [REDACTED TYPE] token.
 * Does NOT reject or quarantine the post — purely transformative.
 *
 * Patterns are applied in declaration order (most-specific first) to avoid
 * partial matches by the less-specific phone-number pattern.
 */
public class PiiScrubbingStep implements ContentProcessingStep {

    private static final Log log = LogFactory.getLog(PiiScrubbingStep.class);

    /**
     * Ordered map: regex pattern string → replacement token.
     * Order matters — more specific patterns must appear before general ones.
     */
    private static final Map<String, String> PATTERNS = new LinkedHashMap<>();
    static {
        // Credit/debit card: 4×4 digits optionally separated by space or dash
        PATTERNS.put(
            "\\b\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}\\b",
            "[REDACTED CARD]");
        // Aadhaar: exactly 12 digits in groups of 4 separated by single spaces
        PATTERNS.put(
            "\\b\\d{4}\\s\\d{4}\\s\\d{4}\\b",
            "[REDACTED ID]");
        // Email addresses
        PATTERNS.put(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
            "[REDACTED EMAIL]");
        // Phone numbers: optional country code + digits in various formats
        PATTERNS.put(
            "(\\+?\\d{1,3}[\\s\\-]?)?\\(?\\d{3,5}\\)?[\\s\\-]?\\d{3,5}[\\s\\-]?\\d{4,5}\\b",
            "[REDACTED PHONE]");
    }

    private final ModerationConfig config;

    public PiiScrubbingStep(ModerationConfig config) {
        this.config = config;
    }

    @Override
    public void process(WeblogEntry entry) throws WebloggerException {
        if (!config.isPiiStepEnabled()) {
            return;
        }

        String text = entry.getText() != null ? entry.getText() : "";

        for (Map.Entry<String, String> rule : PATTERNS.entrySet()) {
            String replacement = rule.getValue();
            // Honour per-type toggle flags
            if (replacement.contains("CARD")  && !config.isScrubCards())   continue;
            if (replacement.contains("ID")    && !config.isScrubAadhaar()) continue;
            if (replacement.contains("EMAIL") && !config.isScrubEmails())  continue;
            if (replacement.contains("PHONE") && !config.isScrubPhones())  continue;
            text = text.replaceAll(rule.getKey(), replacement);
        }

        entry.setText(text);
        log.debug("PiiScrubbingStep: processed entry '" + entry.getTitle() + "'");
    }
}
