package org.apache.roller.weblogger.business.translation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Centralizes language code resolution and validation.
 * Prevents scattered null-checks throughout the service and view layers.
 */
public final class LanguageValidator {

    private static final Log log = LogFactory.getLog(LanguageValidator.class);

    private LanguageValidator() {
        // Utility class — not instantiatable
    }

    /**
     * Resolves a raw language code string into a {@link Language} enum value.
     * Returns null if the code is null, blank, or maps to no known language.
     * Callers that receive null should fall back to original content.
     */
    public static Language resolve(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return null;
        }
        Language lang = Language.fromCode(rawCode.trim());
        if (lang == null) {
            log.warn("Unsupported language code requested: '" + rawCode + "'. Falling back to original content.");
        }
        return lang;
    }

    /**
     * Returns true if a translation attempt should be made.
     * False when target is null, or when target and source are the same language.
     */
    public static boolean shouldTranslate(Language target, Language source) {
        if (target == null) {
            return false;
        }
        if (target == source) {
            log.debug("Target language matches source. Skipping translation.");
            return false;
        }
        return true;
    }
}
