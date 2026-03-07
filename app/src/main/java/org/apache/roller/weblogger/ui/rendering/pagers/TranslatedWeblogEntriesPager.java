package org.apache.roller.weblogger.ui.rendering.pagers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.roller.weblogger.business.translation.Language;
import org.apache.roller.weblogger.business.translation.TranslationResult;
import org.apache.roller.weblogger.business.translation.TranslationService;
import org.apache.roller.weblogger.business.translation.TranslationServiceFactory;
import org.apache.roller.weblogger.pojos.TranslatedWeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.wrapper.WeblogEntryWrapper;

/**
 * A decorator for WeblogEntriesPager that translates all entries in the
 * collection.
 */
public class TranslatedWeblogEntriesPager implements WeblogEntriesPager {

    private final WeblogEntriesPager pager;
    private final Language targetLanguage;
    private final String providerName;
    private Map<Date, List<WeblogEntryWrapper>> translatedEntries = null;

    public TranslatedWeblogEntriesPager(WeblogEntriesPager pager, Language targetLanguage, String providerName) {
        this.pager = pager;
        this.targetLanguage = targetLanguage;
        this.providerName = providerName;
    }

    @Override
    public Map<Date, List<WeblogEntryWrapper>> getEntries() {
        if (translatedEntries != null) {
            return translatedEntries;
        }

        Map<Date, ? extends Collection<WeblogEntryWrapper>> originalMap = pager.getEntries();
        translatedEntries = new LinkedHashMap<>();
        TranslationService service = TranslationServiceFactory.getTranslationService();

        for (Map.Entry<Date, ? extends Collection<WeblogEntryWrapper>> entry : originalMap.entrySet()) {
            List<WeblogEntryWrapper> translatedList = new ArrayList<>();
            for (WeblogEntryWrapper wrapper : entry.getValue()) {
                try {
                    WeblogEntry pojo = wrapper.getPojo();
                    TranslationResult result = service.translateEntry(pojo, targetLanguage, providerName);
                    if (result.isSuccess()) {
                        TranslatedWeblogEntry translatedPojo = new TranslatedWeblogEntry(pojo,
                                result.getTranslatedContent());
                        translatedList.add(WeblogEntryWrapper.wrap(translatedPojo,
                                org.apache.roller.weblogger.business.WebloggerFactory.getWeblogger().getUrlStrategy()));
                    } else {
                        translatedList.add(wrapper);
                    }
                } catch (Exception e) {
                    translatedList.add(wrapper);
                }
            }
            translatedEntries.put(entry.getKey(), translatedList);
        }

        return translatedEntries;
    }

    @Override
    public String getHomeLink() {
        return pager.getHomeLink();
    }

    @Override
    public String getHomeName() {
        return pager.getHomeName();
    }

    @Override
    public String getNextLink() {
        return pager.getNextLink();
    }

    @Override
    public String getNextName() {
        return pager.getNextName();
    }

    @Override
    public String getPrevLink() {
        return pager.getPrevLink();
    }

    @Override
    public String getPrevName() {
        return pager.getPrevName();
    }

    @Override
    public String getNextCollectionLink() {
        return pager.getNextCollectionLink();
    }

    @Override
    public String getNextCollectionName() {
        return pager.getNextCollectionName();
    }

    @Override
    public String getPrevCollectionLink() {
        return pager.getPrevCollectionLink();
    }

    @Override
    public String getPrevCollectionName() {
        return pager.getPrevCollectionName();
    }
}
