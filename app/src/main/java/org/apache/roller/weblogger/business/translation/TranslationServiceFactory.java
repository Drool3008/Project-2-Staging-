package org.apache.roller.weblogger.business.translation;

/**
 * Factory for TranslationService.
 */
public class TranslationServiceFactory {
    private static final TranslationService instance = new TranslationServiceImpl();

    public static TranslationService getTranslationService() {
        return instance;
    }
}
