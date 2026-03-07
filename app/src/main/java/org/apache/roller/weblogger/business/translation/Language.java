package org.apache.roller.weblogger.business.translation;

/**
 * All languages supported by the translation system.
 * Codes are ISO-639-1 short codes (e.g. "hi") used for the ?lang= URL
 * parameter.
 * Sarvam BCP-47 codes are stored separately in SarvamTranslationProvider.
 *
 * Aligned with languages supported by sarvam-translate:v1 (22 scheduled Indian
 * languages + English).
 */
public enum Language {
    // Core languages (original 6)
    ENGLISH("English", "en"),
    HINDI("Hindi", "hi"),
    TAMIL("Tamil", "ta"),
    TELUGU("Telugu", "te"),
    MARATHI("Marathi", "mr"),
    BENGALI("Bengali", "bn"),

    // Additional mayura:v1 / sarvam-translate:v1 languages
    GUJARATI("Gujarati", "gu"),
    KANNADA("Kannada", "kn"),
    MALAYALAM("Malayalam", "ml"),
    ODIA("Odia", "od"),
    PUNJABI("Punjabi", "pa"),

    // Newly added sarvam-translate:v1 languages
    ASSAMESE("Assamese", "as"),
    BODO("Bodo", "brx"),
    DOGRI("Dogri", "doi"),
    KONKANI("Konkani", "kok"),
    KASHMIRI("Kashmiri", "ks"),
    MAITHILI("Maithili", "mai"),
    MANIPURI("Manipuri", "mni"),
    NEPALI("Nepali", "ne"),
    SANSKRIT("Sanskrit", "sa"),
    SANTALI("Santali", "sat"),
    SINDHI("Sindhi", "sd"),
    URDU("Urdu", "ur"),

    // Common global languages — supported by Google Translate
    FRENCH("French", "fr"),
    GERMAN("German", "de"),
    SPANISH("Spanish", "es"),
    JAPANESE("Japanese", "ja"),
    CHINESE("Chinese", "zh"),
    ARABIC("Arabic", "ar"),
    PORTUGUESE("Portuguese", "pt"),
    RUSSIAN("Russian", "ru");

    private final String displayName;
    private final String code;

    Language(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    /**
     * Returns the Language for the given ISO-639-1 code, or null if unsupported.
     * Also handles BCP-47 codes like "hi-IN" by stripping the region tag.
     */
    public static Language fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        // Normalize: "hi-IN" -> "hi", "HI" -> "hi"
        String normalized = code.trim().toLowerCase();
        if (normalized.contains("-")) {
            normalized = normalized.substring(0, normalized.indexOf('-'));
        }
        for (Language lang : Language.values()) {
            if (lang.code.equalsIgnoreCase(normalized)) {
                return lang;
            }
        }
        return null;
    }
}
