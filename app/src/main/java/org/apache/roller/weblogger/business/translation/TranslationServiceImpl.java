package org.apache.roller.weblogger.business.translation;

import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Orchestration layer for content translation.
 *
 * Responsibilities:
 * - Validate inputs
 * - Delegate language resolution to LanguageValidator
 * - Determine source language from entry metadata
 * - Dispatch to the correct provider via TranslationProviderFactory
 * - Cache results in-memory to avoid redundant API calls
 * - Return original content on any failure (never propagates provider errors to
 * view)
 *
 * Does NOT:
 * - Make any HTTP calls
 * - Contain provider-specific logic
 * - Manipulate DOM or rendered HTML
 */
public class TranslationServiceImpl implements TranslationService {
    private static final Log log = LogFactory.getLog(TranslationServiceImpl.class);

    /**
     * In-memory cache: key = "entryId:langCode:providerName", value =
     * TranslationResult.
     * ConcurrentHashMap makes this safe under Jetty's multi-threaded environment.
     * Cache is intentionally simple (no TTL) — entries are immutable once
     * published,
     * so stale data is not a concern for this use case.
     */
    private static final ConcurrentHashMap<String, TranslationResult> translationCache = new ConcurrentHashMap<>();

    @Override
    public TranslationResult translateEntry(WeblogEntry entry, Language targetLanguage) throws TranslationException {
        return translateEntry(entry, targetLanguage, null);
    }

    @Override
    public TranslationResult translateEntry(WeblogEntry entry, Language targetLanguage, String providerName)
            throws TranslationException {
        if (entry == null) {
            throw new TranslationException("Entry cannot be null");
        }
        if (targetLanguage == null) {
            throw new TranslationException("Target language cannot be null");
        }

        // Determine source language (fallback to ENGLISH if blog locale is also
        // unknown)
        String sourceLocale = entry.getLocale() != null ? entry.getLocale()
                : (entry.getWebsite() != null ? entry.getWebsite().getLocale() : "en");
        Language sourceLanguage = LanguageValidator.resolve(sourceLocale);

        // Short-circuit: no translation needed if source == target
        if (!LanguageValidator.shouldTranslate(targetLanguage, sourceLanguage)) {
            log.debug("Skipping translation for entry " + entry.getId() + " — source matches target.");
            TranslatableContent content = ContentMapper.mapToTranslatable(entry);
            return new TranslationResult(content, "NoOpProvider", true);
        }

        // Build cache key
        String effectiveProvider = (providerName != null && !providerName.isBlank())
                ? providerName.toLowerCase().trim()
                : "default";
        String cacheKey = entry.getId() + ":" + targetLanguage.getCode() + ":" + effectiveProvider;

        // Cache hit — return immediately, no API call
        TranslationResult cached = translationCache.get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for entry " + entry.getId()
                    + " -> " + targetLanguage.getCode()
                    + " via " + effectiveProvider);
            return cached;
        }

        log.debug("Translating entry " + entry.getId() + " from "
                + (sourceLanguage != null ? sourceLanguage.getCode() : "unknown")
                + " to " + targetLanguage.getCode() + " using provider: " + providerName);

        TranslatableContent sourceContent = ContentMapper.mapToTranslatable(entry);
        TranslationRequest request = new TranslationRequest(sourceContent, sourceLanguage, targetLanguage,
                entry.getContentType());

        // Delegate to the configured/requested provider
        TranslationProvider provider = TranslationProviderFactory.getProvider(providerName, targetLanguage);
        TranslationResult result = provider.translate(request);

        if (!result.isSuccess()) {
            log.error("Translation failed for entry " + entry.getId() + " via " + provider.getName()
                    + ": " + result.getErrorMessage());
        } else {
            // Only cache successful results
            translationCache.put(cacheKey, result);
            log.debug("Cache STORE for entry " + entry.getId()
                    + " -> " + targetLanguage.getCode()
                    + " via " + effectiveProvider
                    + " (cache size: " + translationCache.size() + ")");
        }

        return result;
    }
}
