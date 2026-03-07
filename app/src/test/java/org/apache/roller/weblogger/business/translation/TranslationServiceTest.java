package org.apache.roller.weblogger.business.translation;

import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TranslationServiceTest {

    private TranslationService translationService;

    @BeforeEach
    public void setUp() {
        translationService = TranslationServiceFactory.getTranslationService();
    }

    @Test
    public void testGetTranslationService() {
        assertNotNull(translationService);
        // Singleton check
        assertEquals(translationService, TranslationServiceFactory.getTranslationService());
    }

    @Test
    public void testTranslateEntryWithMockProvider() throws TranslationException {
        WeblogEntry entry = new WeblogEntry();
        entry.setId("test-id");
        entry.setTitle("Original Title");
        entry.setText("Original Text");
        entry.setSummary("Original Summary");

        // MockProvider just returns the same text
        TranslationResult result = translationService.translateEntry(entry, Language.HINDI, "MockProvider");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("[HI] Original Title", result.getTranslatedContent().getTitle());
        assertEquals("[HI] Original Text", result.getTranslatedContent().getText());
    }

    @Test
    public void testLanguageDetection() {
        // Basic detection logic check
        assertEquals(Language.HINDI, Language.fromCode("hi"));
        assertEquals(Language.HINDI, Language.fromCode("hi-IN"));
        assertEquals(Language.FRENCH, Language.fromCode("fr"));
        assertNull(Language.fromCode("unknown"));
    }
}
