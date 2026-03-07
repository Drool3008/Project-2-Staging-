package org.apache.roller.weblogger.business.translation;

import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Service for translating blog entries.
 */
public interface TranslationService {
    /**
     * Translates a weblog entry to the specified target language.
     * 
     * @param entry          The entry to translate.
     * @param targetLanguage The target language.
     * @return The translation result.
     * @throws TranslationException if translation fails.
     */
    TranslationResult translateEntry(WeblogEntry entry, Language targetLanguage) throws TranslationException;

    /**
     * Translates a weblog entry using a specific provider.
     */
    TranslationResult translateEntry(WeblogEntry entry, Language targetLanguage, String providerName)
            throws TranslationException;
}
