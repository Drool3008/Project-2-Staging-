package org.apache.roller.weblogger.business.translation;

import java.io.Serializable;

/**
 * Data contract for a translation request.
 */
public class TranslationRequest implements Serializable {
    private TranslatableContent content;
    private Language sourceLanguage;
    private Language targetLanguage;
    private String contentType; // e.g., "text", "html", "xhtml"

    public TranslationRequest(TranslatableContent content, Language sourceLanguage, Language targetLanguage,
            String contentType) {
        this.content = content;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.contentType = contentType;
    }

    public TranslatableContent getContent() {
        return content;
    }

    public Language getSourceLanguage() {
        return sourceLanguage;
    }

    public Language getTargetLanguage() {
        return targetLanguage;
    }

    public String getContentType() {
        return contentType;
    }
}
