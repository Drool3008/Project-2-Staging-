package org.apache.roller.weblogger.business.translation;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TranslationProvider backed by Google Translate (unofficial free endpoint).
 *
 * Uses the client=gtx endpoint which requires no API key and supports 100+
 * languages. Suitable as a free second provider alongside Sarvam AI.
 *
 * Falls back gracefully on network errors — never throws to the caller.
 *
 * Response format:
 * [[["translated","original",null,null,10]],null,"en"]
 */
public class GoogleTranslateProvider implements TranslationProvider {

    private static final Log log = LogFactory.getLog(GoogleTranslateProvider.class);

    private static final String PROVIDER_NAME = "GoogleTranslateProvider";

    // Unofficial GTX endpoint — same one Chrome extensions use
    private static final String API_URL = "https://translate.googleapis.com/translate_a/single"
            + "?client=gtx&dt=t";

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean supports(Language language) {
        return language != null; // Google supports all our enum values
    }

    @Override
    public TranslationResult translate(TranslationRequest request) throws TranslationException {
        Language target = request.getTargetLanguage();
        Language source = request.getSourceLanguage();
        String targetCode = target.getCode();
        String sourceCode = source != null ? source.getCode() : "auto";

        try {
            TranslatableContent original = request.getContent();
            String translatedTitle = translateText(original.getTitle(), sourceCode, targetCode);
            String translatedText = translateText(original.getText(), sourceCode, targetCode);
            String translatedSummary = translateText(original.getSummary(), sourceCode, targetCode);

            TranslatableContent translated = new TranslatableContent(
                    translatedTitle, translatedText, translatedSummary);
            return new TranslationResult(translated, PROVIDER_NAME, true);

        } catch (IOException e) {
            log.error("Google Translate request failed: " + e.getMessage(), e);
            return TranslationResult.failure(PROVIDER_NAME, "Request failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------

    private String translateText(String text, String sourceLang, String targetLang)
            throws IOException {
        if (text == null || text.isBlank()) {
            return text;
        }

        String url = API_URL
                + "&sl=" + sourceLang
                + "&tl=" + targetLang
                + "&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        // Custom headers to mimic a browser — some CDN nodes reject bot-like requests
        String response = HttpHelper.getJson(url,
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                        + "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                "Accept-Language", "en-US,en;q=0.9",
                "Referer", "https://translate.google.com/");

        return extractTranslation(response);
    }

    /**
     * Parses the first translated segment from the GTX response.
     *
     * Response shape (condensed):
     * [[["translated text","source text",null,null,10],...],null,"en",...]
     *
     * We collect ALL first-element strings from the outer array and join them
     * to handle long texts that Google splits into multiple segments.
     */
    private String extractTranslation(String response) throws IOException {
        if (response == null || response.length() < 5) {
            throw new IOException("Empty response from Google Translate");
        }

        // Response starts with: [[[" — find the first segment
        StringBuilder result = new StringBuilder();
        int pos = 0;

        // Outer array: [[seg1],[seg2],...]
        // Each seg: ["translated","original",...]
        // We iterate all top-level array elements and collect translations.
        while (true) {
            // Find opening ["
            int segStart = response.indexOf("[\"", pos);
            if (segStart < 0)
                break;

            int strStart = segStart + 2; // skip ["
            int strEnd = HttpHelper.findStringEnd(response, strStart);
            if (strEnd < 0)
                break;

            String segment = HttpHelper.unescapeJsonString(response.substring(strStart, strEnd));

            // Verify there's a comma after the closing quote (seg has more fields)
            // This filters out non-translation array elements
            if (strEnd + 1 < response.length() && response.charAt(strEnd + 1) == ',') {
                result.append(segment);
            }

            pos = strEnd + 1;

            // Stop once we pass the first closing "]," of the outer segments array
            // (Google wraps all text segments in the first top-level array)
            int nextClose = response.indexOf("]]", pos);
            if (nextClose >= 0 && nextClose < response.indexOf("[\"", pos) - 2) {
                break;
            }
        }

        String translation = result.toString().trim();
        if (translation.isEmpty()) {
            throw new IOException("Could not parse translation from response: "
                    + response.substring(0, Math.min(200, response.length())));
        }
        return translation;
    }
}
