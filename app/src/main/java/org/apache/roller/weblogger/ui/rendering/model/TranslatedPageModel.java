package org.apache.roller.weblogger.ui.rendering.model;

import org.apache.roller.weblogger.business.translation.Language;
import org.apache.roller.weblogger.business.translation.TranslationResult;
import org.apache.roller.weblogger.business.translation.TranslationService;
import org.apache.roller.weblogger.business.translation.TranslationServiceFactory;
import org.apache.roller.weblogger.pojos.TranslatedWeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.wrapper.WeblogEntryWrapper;
import org.apache.roller.weblogger.ui.rendering.pagers.TranslatedWeblogEntriesPager;
import org.apache.roller.weblogger.ui.rendering.pagers.WeblogEntriesPager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A specialized PageModel that supports transparent translation based on the
 * 'lang' request parameter.
 */
public class TranslatedPageModel extends PageModel {

    private static final Log log = LogFactory.getLog(TranslatedPageModel.class);

    @Override
    public WeblogEntryWrapper getWeblogEntry() {
        WeblogEntryWrapper originalWrapper = super.getWeblogEntry();
        if (originalWrapper == null) {
            return null;
        }

        String langCode = getRequestParameter("lang");
        String providerName = getRequestParameter("provider");
        if (langCode == null || langCode.isEmpty()) {
            return originalWrapper;
        }

        try {
            Language targetLang = Language.fromCode(langCode);
            if (targetLang == null) {
                return originalWrapper;
            }

            WeblogEntry pojo = originalWrapper.getPojo();
            TranslationService service = TranslationServiceFactory.getTranslationService();
            TranslationResult result = service.translateEntry(pojo, targetLang, providerName);

            if (result.isSuccess()) {
                TranslatedWeblogEntry translatedPojo = new TranslatedWeblogEntry(pojo, result.getTranslatedContent());
                return WeblogEntryWrapper.wrap(translatedPojo,
                        org.apache.roller.weblogger.business.WebloggerFactory.getWeblogger().getUrlStrategy());
            } else {
                log.error("Translation failed for entry: " + result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error during translation in getWeblogEntry", e);
        }

        return originalWrapper;
    }

    /**
     * Note: For full support, we should also wrap the WeblogEntriesPager to return
     * translated entries.
     * This will be addressed in a follow-up step for Milestone 4.
     */
    @Override
    public WeblogEntriesPager getWeblogEntriesPager(String catArgument) {
        WeblogEntriesPager originalPager = super.getWeblogEntriesPager(catArgument);

        String langCode = getRequestParameter("lang");
        String providerName = getRequestParameter("provider");
        if (langCode == null || langCode.isEmpty()) {
            return originalPager;
        }

        try {
            Language targetLang = Language.fromCode(langCode);
            if (targetLang == null) {
                return originalPager;
            }
            return new TranslatedWeblogEntriesPager(originalPager, targetLang, providerName);
        } catch (Exception e) {
            log.error("Error creating TranslatedWeblogEntriesPager", e);
            return originalPager;
        }
    }

    @Override
    public WeblogEntriesPager getWeblogEntriesPagerByTag(String tagArgument) {
        WeblogEntriesPager originalPager = super.getWeblogEntriesPagerByTag(tagArgument);

        String langCode = getRequestParameter("lang");
        String providerName = getRequestParameter("provider");
        if (langCode == null || langCode.isEmpty()) {
            return originalPager;
        }

        try {
            Language targetLang = Language.fromCode(langCode);
            if (targetLang == null) {
                return originalPager;
            }
            return new TranslatedWeblogEntriesPager(originalPager, targetLang, providerName);
        } catch (Exception e) {
            log.error("Error creating TranslatedWeblogEntriesPager for tag", e);
            return originalPager;
        }
    }
}
