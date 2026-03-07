package org.apache.roller.weblogger.business.translation;

import java.io.Serializable;

/**
 * Data contract for a translation result.
 */
public class TranslationResult implements Serializable {
    private TranslatableContent translatedContent;
    private String providerName;
    private boolean success;
    private String errorMessage;

    public TranslationResult(TranslatableContent translatedContent, String providerName, boolean success) {
        this.translatedContent = translatedContent;
        this.providerName = providerName;
        this.success = success;
    }

    public static TranslationResult failure(String providerName, String errorMessage) {
        TranslationResult result = new TranslationResult(null, providerName, false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public TranslatableContent getTranslatedContent() {
        return translatedContent;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
