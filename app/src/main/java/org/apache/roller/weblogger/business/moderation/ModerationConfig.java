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
import java.util.List;

/**
 * Holds admin-configurable settings for the moderation pipeline.
 *
 * In a full implementation these would be persisted to the database
 * and editable via the admin UI. For this release, sensible defaults
 * are hard-coded and each setting is individually overridable via
 * the setter methods.
 */
public class ModerationConfig {

    // ---------------------------------------------------------------- Spam

    /** Max fraction of external links per word before post is flagged. */
    private double linkDensityThreshold = 0.05;   // 5 links per 100 words

    /** Max fraction of ALL-CAPS words before post is flagged. */
    private double capsRatioThreshold   = 0.30;   // 30% words fully caps

    /**
     * Max fraction a single character may occupy across the entire body.
     * A body of "aaaaaaa...aaa" has a single-char ratio of 1.0.
     * Default 0.70 = flag when one character is ≥ 70% of all characters.
     */
    private double charFloodThreshold  = 0.70;

    /**
     * Max fraction of words that look like gibberish (no vowels, or a single
     * repeated character ≥ 5 chars long).  Default 0.60 = flag when ≥ 60%
     * of words are gibberish.
     */
    private double gibberishWordRatio  = 0.60;

    /** Trigger phrases whose presence immediately flags a post as spam. */
    private List<String> spamPhrases = Arrays.asList(
        "buy now", "click here", "limited offer",
        "100% free", "earn money fast", "work from home",
        "make money", "no credit card", "free trial",
        "act now", "order now", "special promotion"
    );

    // ---------------------------------------------------------------- PII toggles

    private boolean scrubEmails  = true;
    private boolean scrubPhones  = true;
    private boolean scrubCards   = true;
    private boolean scrubAadhaar = true;

    // ---------------------------------------------------------------- Profanity

    /** Words that will be replaced with asterisks in published posts. */
    private List<String> bannedWords = Arrays.asList(
        "profanity1", "profanity2"
    );

    // ---------------------------------------------------------------- Step enable/disable

    private boolean spamStepEnabled      = true;
    private boolean htmlStepEnabled      = true;
    private boolean piiStepEnabled       = true;
    private boolean profanityStepEnabled = true;

    // ---------------------------------------------------------------- Getters / setters

    public double getLinkDensityThreshold() { return linkDensityThreshold; }
    public void setLinkDensityThreshold(double linkDensityThreshold) {
        this.linkDensityThreshold = linkDensityThreshold;
    }

    public double getCapsRatioThreshold() { return capsRatioThreshold; }
    public void setCapsRatioThreshold(double capsRatioThreshold) {
        this.capsRatioThreshold = capsRatioThreshold;
    }

    public double getCharFloodThreshold() { return charFloodThreshold; }
    public void setCharFloodThreshold(double charFloodThreshold) {
        this.charFloodThreshold = charFloodThreshold;
    }

    public double getGibberishWordRatio() { return gibberishWordRatio; }
    public void setGibberishWordRatio(double gibberishWordRatio) {
        this.gibberishWordRatio = gibberishWordRatio;
    }

    public List<String> getSpamPhrases() { return spamPhrases; }
    public void setSpamPhrases(List<String> spamPhrases) {
        this.spamPhrases = spamPhrases;
    }

    public boolean isScrubEmails() { return scrubEmails; }
    public void setScrubEmails(boolean scrubEmails) { this.scrubEmails = scrubEmails; }

    public boolean isScrubPhones() { return scrubPhones; }
    public void setScrubPhones(boolean scrubPhones) { this.scrubPhones = scrubPhones; }

    public boolean isScrubCards() { return scrubCards; }
    public void setScrubCards(boolean scrubCards) { this.scrubCards = scrubCards; }

    public boolean isScrubAadhaar() { return scrubAadhaar; }
    public void setScrubAadhaar(boolean scrubAadhaar) { this.scrubAadhaar = scrubAadhaar; }

    public List<String> getBannedWords() { return bannedWords; }
    public void setBannedWords(List<String> bannedWords) { this.bannedWords = bannedWords; }

    public boolean isSpamStepEnabled() { return spamStepEnabled; }
    public void setSpamStepEnabled(boolean spamStepEnabled) {
        this.spamStepEnabled = spamStepEnabled;
    }

    public boolean isHtmlStepEnabled() { return htmlStepEnabled; }
    public void setHtmlStepEnabled(boolean htmlStepEnabled) {
        this.htmlStepEnabled = htmlStepEnabled;
    }

    public boolean isPiiStepEnabled() { return piiStepEnabled; }
    public void setPiiStepEnabled(boolean piiStepEnabled) {
        this.piiStepEnabled = piiStepEnabled;
    }

    public boolean isProfanityStepEnabled() { return profanityStepEnabled; }
    public void setProfanityStepEnabled(boolean profanityStepEnabled) {
        this.profanityStepEnabled = profanityStepEnabled;
    }
}
