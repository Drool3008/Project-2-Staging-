package org.apache.roller.weblogger.business.translation;

/**
 * Strategy interface for translation providers.
 * Each provider encapsulates all provider-specific API and request logic.
 */
public interface TranslationProvider {

    /**
     * Translate the given request. Implementations must never throw unchecked
     * exceptions — wrap errors in a failed TranslationResult instead.
     */
    TranslationResult translate(TranslationRequest request) throws TranslationException;

    /** Human-readable provider name used for registration and logging. */
    String getName();

    /**
     * Returns true if this provider supports translating to the given target
     * language.
     * Used to emit an early warning before dispatching, avoiding runtime surprises.
     */
    boolean supports(Language language);
}
