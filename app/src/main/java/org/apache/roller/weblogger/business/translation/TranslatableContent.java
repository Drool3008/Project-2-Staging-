package org.apache.roller.weblogger.business.translation;

import java.io.Serializable;

/**
 * A structured container for blog entry content that can be translated.
 */
public class TranslatableContent implements Serializable {
    private String title;
    private String text;
    private String summary;

    public TranslatableContent() {
    }

    public TranslatableContent(String title, String text, String summary) {
        this.title = title;
        this.text = text;
        this.summary = summary;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
