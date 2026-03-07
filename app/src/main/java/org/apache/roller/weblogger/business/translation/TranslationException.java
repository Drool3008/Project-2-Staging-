package org.apache.roller.weblogger.business.translation;

/**
 * Base exception for translation-related errors.
 */
public class TranslationException extends Exception {
    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
