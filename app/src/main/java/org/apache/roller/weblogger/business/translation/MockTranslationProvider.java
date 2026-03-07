package org.apache.roller.weblogger.business.translation;

/**
 * A mock translation provider for local development and verification.
 * It simulates translation by prefixing content with the target language code.
 * Supports all languages — it never makes real API calls.
 */
public class MockTranslationProvider implements TranslationProvider {

    @Override
    public TranslationResult translate(TranslationRequest request) throws TranslationException {
        TranslatableContent source = request.getContent();
        String targetCode = request.getTargetLanguage().getCode().toUpperCase();

        TranslatableContent translated = new TranslatableContent();
        translated.setTitle("[" + targetCode + "] " + source.getTitle());
        translated.setSummary("[" + targetCode + "] " + source.getSummary());

        // Simulating HTML preservation for text field
        if ("html".equalsIgnoreCase(request.getContentType()) || "xhtml".equalsIgnoreCase(request.getContentType())) {
            translated.setText("<div class=\"translated-mock\">[" + targetCode + "] " + source.getText() + "</div>");
        } else {
            translated.setText("[" + targetCode + "] " + source.getText());
        }

        return new TranslationResult(translated, getName(), true);
    }

    @Override
    public String getName() {
        return "MockProvider";
    }

    @Override
    public boolean supports(Language language) {
        // Mock supports all languages — it does not call any real API
        return language != null;
    }
}
