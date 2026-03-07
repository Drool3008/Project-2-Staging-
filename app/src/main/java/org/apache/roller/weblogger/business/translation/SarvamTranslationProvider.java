package org.apache.roller.weblogger.business.translation;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;

/**
 * TranslationProvider backed by the Sarvam AI translation API.
 *
 * Uses model "sarvam-translate:v1" supporting all 22 scheduled Indian languages
 * in formal mode. Uses HttpHelper for shared HTTP and JSON utilities.
 *
 * Configuration keys (in roller-custom.properties):
 * translation.provider.sarvam.api_key — required
 * translation.provider.sarvam.model — optional, default: sarvam-translate:v1
 *
 * API reference: https://api.sarvam.ai/translate
 */
public class SarvamTranslationProvider implements TranslationProvider {

    private static final Log log = LogFactory.getLog(SarvamTranslationProvider.class);

    private static final String PROVIDER_NAME = "SarvamProvider";
    private static final String API_URL = "https://api.sarvam.ai/translate";
    private static final String CONFIG_API_KEY = "translation.provider.sarvam.api_key";
    private static final String CONFIG_MODEL = "translation.provider.sarvam.model";
    private static final String DEFAULT_MODEL = "sarvam-translate:v1";
    private static final int MAX_CHARS = 1900;

    // All languages supported by sarvam-translate:v1
    private static final Set<Language> SUPPORTED = EnumSet.of(
            Language.ENGLISH, Language.HINDI, Language.BENGALI,
            Language.GUJARATI, Language.KANNADA, Language.MALAYALAM,
            Language.MARATHI, Language.ODIA, Language.PUNJABI,
            Language.TAMIL, Language.TELUGU, Language.ASSAMESE,
            Language.BODO, Language.DOGRI, Language.KONKANI,
            Language.KASHMIRI, Language.MAITHILI, Language.MANIPURI,
            Language.NEPALI, Language.SANSKRIT, Language.SANTALI,
            Language.SINDHI, Language.URDU);

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(Language language) {
        return language != null && SUPPORTED.contains(language);
    }

    @Override
    public TranslationResult translate(TranslationRequest request) throws TranslationException {
        String apiKey = WebloggerConfig.getProperty(CONFIG_API_KEY, "").trim();
        if (apiKey.isEmpty()) {
            log.error("Sarvam API key not configured. "
                    + "Set 'translation.provider.sarvam.api_key' in roller-custom.properties.");
            return TranslationResult.failure(PROVIDER_NAME, "API key not configured");
        }

        if (!supports(request.getTargetLanguage())) {
            log.warn("Sarvam does not support target language: " + request.getTargetLanguage());
            return TranslationResult.failure(PROVIDER_NAME,
                    "Unsupported target language: " + request.getTargetLanguage());
        }

        String model = WebloggerConfig.getProperty(CONFIG_MODEL, DEFAULT_MODEL);
        String sourceLang = toSarvamCode(request.getSourceLanguage(), model);
        String targetLang = toSarvamCode(request.getTargetLanguage(), model);

        try {
            TranslatableContent original = request.getContent();
            String translatedTitle = translateField(original.getTitle(), sourceLang, targetLang, model, apiKey);
            String translatedText = translateField(original.getText(), sourceLang, targetLang, model, apiKey);
            String translatedSummary = translateField(original.getSummary(), sourceLang, targetLang, model, apiKey);

            TranslatableContent translated = new TranslatableContent(
                    translatedTitle, translatedText, translatedSummary);
            return new TranslationResult(translated, PROVIDER_NAME, true);

        } catch (IOException e) {
            log.error("Sarvam API call failed: " + e.getMessage(), e);
            return TranslationResult.failure(PROVIDER_NAME, "API call failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — use HttpHelper for shared JSON utilities
    // -------------------------------------------------------------------------

    private String translateField(String text, String sourceLang, String targetLang,
            String model, String apiKey) throws IOException {
        if (text == null || text.isBlank()) {
            return text;
        }
        String body = buildRequest(truncate(text), sourceLang, targetLang, model);
        String response = HttpHelper.postJson(API_URL, body, "api-subscription-key", apiKey);
        return extractTranslatedText(response);
    }

    /**
     * Builds the exact JSON body documented by Sarvam AI.
     * Uses HttpHelper.jsonString() — single source of truth for JSON string
     * encoding.
     */
    private String buildRequest(String input, String sourceLang, String targetLang, String model) {
        return "{"
                + "\"input\":" + HttpHelper.jsonString(input) + ","
                + "\"source_language_code\":" + HttpHelper.jsonString(sourceLang) + ","
                + "\"target_language_code\":" + HttpHelper.jsonString(targetLang) + ","
                + "\"speaker_gender\":\"Male\","
                + "\"mode\":\"formal\","
                + "\"model\":" + HttpHelper.jsonString(model)
                + "}";
    }

    /**
     * Safely parses "translated_text" from the Sarvam response.
     * Uses HttpHelper.findStringEnd to correctly skip escaped quotes.
     *
     * Response: {"request_id":"...","translated_text":"नमस्ते
     * दुनिया","source_language_code":"en-IN"}
     */
    private String extractTranslatedText(String response) throws IOException {
        String key = "\"translated_text\":";
        int idx = response.indexOf(key);
        if (idx < 0) {
            throw new IOException("Unexpected Sarvam response (no translated_text): " + response);
        }
        int start = response.indexOf('"', idx + key.length()) + 1;
        int end = HttpHelper.findStringEnd(response, start);
        if (start <= 0 || end <= start) {
            throw new IOException("Could not parse translated_text: " + response);
        }
        return HttpHelper.unescapeJsonString(response.substring(start, end));
    }

    /**
     * Maps a Language enum to the BCP-47 locale code Sarvam expects (e.g. "hi-IN").
     * Uses "auto" for source detection when mayura:v1 is selected and source is
     * unknown.
     */
    private String toSarvamCode(Language language, String model) {
        if (language == null) {
            return "mayura:v1".equals(model) ? "auto" : "en-IN";
        }
        switch (language) {
            case ENGLISH:
                return "en-IN";
            case HINDI:
                return "hi-IN";
            case BENGALI:
                return "bn-IN";
            case GUJARATI:
                return "gu-IN";
            case KANNADA:
                return "kn-IN";
            case MALAYALAM:
                return "ml-IN";
            case MARATHI:
                return "mr-IN";
            case ODIA:
                return "od-IN";
            case PUNJABI:
                return "pa-IN";
            case TAMIL:
                return "ta-IN";
            case TELUGU:
                return "te-IN";
            case ASSAMESE:
                return "as-IN";
            case BODO:
                return "brx-IN";
            case DOGRI:
                return "doi-IN";
            case KONKANI:
                return "kok-IN";
            case KASHMIRI:
                return "ks-IN";
            case MAITHILI:
                return "mai-IN";
            case MANIPURI:
                return "mni-IN";
            case NEPALI:
                return "ne-IN";
            case SANSKRIT:
                return "sa-IN";
            case SANTALI:
                return "sat-IN";
            case SINDHI:
                return "sd-IN";
            case URDU:
                return "ur-IN";
            default:
                return "en-IN";
        }
    }

    /** Truncates text at the last word boundary before MAX_CHARS. */
    private String truncate(String text) {
        if (text.length() <= MAX_CHARS) {
            return text;
        }
        int cut = text.lastIndexOf(' ', MAX_CHARS);
        return cut > 0 ? text.substring(0, cut) : text.substring(0, MAX_CHARS);
    }
}
