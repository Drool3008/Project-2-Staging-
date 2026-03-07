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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * Chain of Responsibility — Step 2.
 *
 * Runs SECOND because PiiScrubbingStep and ProfanityFilterStep use regex
 * on plain text. If HTML tags remain (e.g. &lt;script&gt;, encoded entities,
 * event handlers like onload=), those downstream regex steps will miss
 * content hidden inside tag attributes.
 * Sanitizing HTML first gives downstream steps clean, trustworthy text.
 *
 * Tags stripped: &lt;script&gt;, &lt;iframe&gt;, &lt;object&gt;, &lt;embed&gt;,
 *                all event handlers (onload=, onclick= etc.), javascript: hrefs.
 * Tags kept:     &lt;p&gt;, &lt;b&gt;, &lt;i&gt;, &lt;a href&gt;, &lt;img src&gt;,
 *                &lt;ul&gt;, &lt;ol&gt;, &lt;li&gt;, &lt;blockquote&gt;,
 *                &lt;code&gt;, &lt;pre&gt;, &lt;h1&gt;-&lt;h6&gt;, basic formatting.
 *
 * Uses the OWASP Java HTML Sanitizer library (com.googlecode.owasp-java-html-sanitizer).
 */
public class HtmlSanitizationStep implements ContentProcessingStep {

    private static final Log log = LogFactory.getLog(HtmlSanitizationStep.class);

    /** Combined safe policy: formatting + links + images + block elements + styles. */
    private static final PolicyFactory POLICY =
        Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.STYLES);

    private final ModerationConfig config;

    public HtmlSanitizationStep(ModerationConfig config) {
        this.config = config;
    }

    @Override
    public void process(WeblogEntry entry) throws WebloggerException {
        if (!config.isHtmlStepEnabled()) {
            return;
        }

        String original  = entry.getText() != null ? entry.getText() : "";
        String sanitized = POLICY.sanitize(original);
        entry.setText(sanitized);

        log.debug("HtmlSanitizationStep: sanitized entry '" + entry.getTitle() + "'");
    }
}
