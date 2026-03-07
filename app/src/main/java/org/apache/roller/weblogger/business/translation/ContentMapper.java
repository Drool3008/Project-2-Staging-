package org.apache.roller.weblogger.business.translation;

import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Helper to map between WeblogEntry fields and TranslatableContent DTO.
 */
public class ContentMapper {

    /**
     * Maps WeblogEntry content to TranslatableContent.
     */
    public static TranslatableContent mapToTranslatable(WeblogEntry entry) {
        return new TranslatableContent(
                entry.getTitle(),
                entry.getText(),
                entry.getSummary());
    }
}
