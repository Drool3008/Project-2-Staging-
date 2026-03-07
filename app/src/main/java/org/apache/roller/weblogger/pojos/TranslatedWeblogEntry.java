package org.apache.roller.weblogger.pojos;

import org.apache.roller.weblogger.business.translation.TranslatableContent;

/**
 * A transient subclass of WeblogEntry that holds translated content.
 * This allows us to use localized versions of an entry in the UI without
 * affecting the persistent state in the database.
 */
public class TranslatedWeblogEntry extends WeblogEntry {

    private String translatedTitle;
    private String translatedText;
    private String translatedSummary;

    public TranslatedWeblogEntry(WeblogEntry original, TranslatableContent translatedContent) {
        // Copy all standard fields using the built-in setData method
        this.setData(original);

        // Set translated fields (these will override the ones from setData)
        this.translatedTitle = translatedContent.getTitle();
        this.translatedText = translatedContent.getText();
        this.translatedSummary = translatedContent.getSummary();
    }

    @Override
    public String getTitle() {
        return translatedTitle != null ? translatedTitle : super.getTitle();
    }

    @Override
    public String getText() {
        return translatedText != null ? translatedText : super.getText();
    }

    @Override
    public String getSummary() {
        return translatedSummary != null ? translatedSummary : super.getSummary();
    }
}
