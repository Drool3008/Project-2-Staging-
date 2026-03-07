package org.apache.roller.weblogger.business.translation;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;

/**
 * Factory for selecting and instantiating translation providers.
 * Provider resolution order:
 * 1. Look up provider by the name in 'translation.provider.default'
 * 2. Warn and fall back to MockProvider if config name is unknown
 * 3. Notify early if the resolved provider does not support the target language
 */
public class TranslationProviderFactory {
    private static final Log log = LogFactory.getLog(TranslationProviderFactory.class);
    private static final Map<String, TranslationProvider> providers = new HashMap<>();

    static {
        registerProvider(new MockTranslationProvider());
        registerProvider(new SarvamTranslationProvider());
        registerProvider(new GoogleTranslateProvider());
    }

    public static void registerProvider(TranslationProvider provider) {
        providers.put(provider.getName().toLowerCase(), provider);
    }

    /**
     * Returns the provider specified in the configuration.
     * Falls back to MockProvider if the named provider is not registered.
     */
    public static TranslationProvider getProvider() {
        String providerName = WebloggerConfig.getProperty("translation.provider.default", "MockProvider");
        TranslationProvider provider = providers.get(providerName.toLowerCase());

        if (provider == null) {
            log.warn("Translation provider '" + providerName + "' not found. Falling back to MockProvider.");
            return providers.get("mockprovider");
        }
        return provider;
    }

    /**
     * Returns the default configured provider and logs a warning if the specified
     * language is not supported by that provider.
     */
    public static TranslationProvider getProvider(Language targetLanguage) {
        TranslationProvider provider = getProvider();
        if (targetLanguage != null && !provider.supports(targetLanguage)) {
            log.warn("Provider '" + provider.getName() + "' does not support language: " + targetLanguage);
        }
        return provider;
    }

    /**
     * Returns a specific provider by name.
     * Falls back to the default configured provider if name is null/empty.
     */
    public static TranslationProvider getProvider(String providerName, Language targetLanguage) {
        if (providerName == null || providerName.isBlank()) {
            return getProvider(targetLanguage);
        }

        TranslationProvider provider = providers.get(providerName.toLowerCase().trim());
        if (provider == null) {
            log.warn("Requested provider '" + providerName + "' not found. Falling back to default.");
            return getProvider(targetLanguage);
        }

        if (targetLanguage != null && !provider.supports(targetLanguage)) {
            log.warn("Provider '" + provider.getName() + "' does not support language: " + targetLanguage);
        }
        return provider;
    }
}
