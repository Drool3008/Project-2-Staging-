package org.apache.roller.weblogger.business.translation;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;

/**
 * TranslationProvider backed by MyMemory translation engine.
 * Works without an API key for up to 500 words/day.
 *
 * Configuration keys (in roller-custom.properties):
 * translation.provider.mymemory.email — optional (increases limit to 5000
 * words/day)
 *
 * API reference: https://mymemory.translated.net/doc/spec.php
 */
public class MyMemoryTranslationProvider implements TranslationProvider {

    private static final Log log = LogFactory.getLog(MyMemoryTranslationProvider.class);

    private static final String PROVIDER_NAME = "MyMemoryProvider";
    private static final String CONFIG_EMAIL = "translation.provider.mymemory.email";
    private static final String API_URL = "https://api.mymemory.translated.net/get";

    // Most common ISO-639-1 languages for testing alongside Sarvam
    private static final Set<Language> SUPPORTED_LANGUAGES = EnumSet.of(
            Language.ENGLISH,
            Language.HINDI,
            Language.TAMIL,
            Language.BENGALI,
            Language.MARATHI,
            Language.TELUGU);

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(Language language) {
        return language != null && SUPPORTED_LANGUAGES.contains(language);
    }

    @Override
    public TranslationResult translate(TranslationRequest request) throws TranslationException {
        if (!supports(request.getTargetLanguage())) {
            log.warn("MyMemory does not support target language: " + request.getTargetLanguage());
            return TranslationResult.failure(PROVIDER_NAME,
                    "Unsupported target language: " + request.getTargetLanguage());
        }

        String email = WebloggerConfig.getProperty(CONFIG_EMAIL, "");
        String srcCode = toIsoCode(request.getSourceLanguage());
        String tgtCode = toIsoCode(request.getTargetLanguage());

        try {
            TranslatableContent original = request.getContent();
            String translatedTitle = translateField(original.getTitle(), srcCode, tgtCode, email);
            String translatedText = translateField(original.getText(), srcCode, tgtCode, email);
            String translatedSummary = translateField(original.getSummary(), srcCode, tgtCode, email);

            TranslatableContent translated = new TranslatableContent(
                    translatedTitle, translatedText, translatedSummary);
            return new TranslationResult(translated, PROVIDER_NAME, true);

        } catch (IOException e) {
            log.error("MyMemory API call failed: " + e.getMessage(), e);
            return TranslationResult.failure(PROVIDER_NAME, "API call failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String translateField(String text, String srcCode, String tgtCode, String email) throws IOException {
        if (text == null || text.isBlank()) {
            return text;
        }

        String url = API_URL + "?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                + "&langpair=" + srcCode + "|" + tgtCode;

        if (email != null && !email.isBlank()) {
            url += "&de=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
        }

        String response = HttpHelper.getJson(url);
        return extractTranslatedText(response);
    }

    /**
     * Safely parses the "translatedText" field from the MyMemory response.
     * Uses HttpHelper.findStringEnd to handle escaped quotes safely.
     * Response format: {"responseData":{"translatedText":"Ciao Mondo",...}
     */
    private String extractTranslatedText(String response) throws IOException {
        String key = "\"translatedText\":";
        int idx = response.indexOf(key);
        if (idx < 0) {
            throw new IOException("Unexpected response (no translatedText): " + response);
        }
        int start = response.indexOf('"', idx + key.length()) + 1;
        int end = HttpHelper.findStringEnd(response, start); // escape-safe
        if (start <= 0 || end <= start) {
            throw new IOException("Could not parse translatedText from response");
        }
        return HttpHelper.unescapeJsonString(response.substring(start, end));
    }

    /** Maps Language enum to ISO-639-1 code used by MyMemory. */
    private String toIsoCode(Language language) {
        if (language == null)
            return "en";
        switch (language) {
            case HINDI:
                return "hi";
            case TAMIL:
                return "ta";
            case BENGALI:
                return "bn";
            case MARATHI:
                return "mr";
            case TELUGU:
                return "te";
            case ENGLISH:
            default:
                return "en";
        }
    }
}
