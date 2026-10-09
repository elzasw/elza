package cz.tacr.elza.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import cz.tacr.elza.domain.RulTranslation;
import cz.tacr.elza.domain.SysLanguage;

/**
 * The translations shipped with the core: every message of {@link CoreMessage} has a text in
 * every shipped language, every pattern compiles and takes no more arguments than the message
 * has, and the files load as rows of the given languages.
 */
class CoreMessagesTest {

    private static final List<String> SHIPPED_LANGUAGES = List.of("cs", "en");

    @Test
    void everyMessageIsShippedInEveryLanguage() {
        for (String tag : SHIPPED_LANGUAGES) {
            Map<String, String> messages = CoreTranslations.messages(tag);
            assertNotNull(messages, "no core translations for " + tag);
            for (CoreMessage message : CoreMessage.values()) {
                String pattern = messages.get(message.key());
                assertNotNull(pattern, message.key() + " is missing in translations/" + tag + ".xml");
                assertPattern(pattern, message, tag);
            }
            for (String key : messages.keySet()) {
                assertTrue(key.startsWith(CoreMessage.PACKAGE + "/"), key + " in translations/" + tag + ".xml is not a core message");
                CoreMessage.valueOf(key.substring(CoreMessage.PACKAGE.length() + 1));
            }
        }
        for (CoreMessage message : CoreMessage.values()) {
            assertPattern(message.text(), message, "the code");
        }
        assertNull(CoreTranslations.messages("xx"));
    }

    @Test
    void filesLoadAsRowsOfTheLanguages() {
        SysLanguage cs = language(1, "cs");
        SysLanguage xx = language(9, "xx");
        List<RulTranslation> rows = CoreTranslations.load(List.of(cs, xx));
        assertEquals(CoreMessage.values().length, rows.size());
        RulTranslation row = rows.stream().filter(r -> r.getEntityCode().equals(CoreMessage.UNDEFINED_VALUE.key()))
                .findFirst().orElseThrow();
        assertEquals("MESSAGE", row.getEntityType());
        assertEquals("text", row.getField());
        assertEquals(cs.getLanguageId(), row.getLanguageId());
        assertEquals("výjimka", row.getTextValue());
    }

    private static void assertPattern(String pattern, CoreMessage message, String where) {
        MessageFormat format;
        try {
            format = new MessageFormat(pattern, Locale.ROOT);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(message.key() + " in " + where + ": invalid pattern " + pattern, e);
        }
        assertTrue(format.getFormatsByArgumentIndex().length <= message.arity(),
                   message.key() + " in " + where + " uses more arguments than " + message.arity());
    }

    private static SysLanguage language(int id, String tag) {
        SysLanguage language = new SysLanguage();
        language.setLanguageId(id);
        language.setCode(tag.toUpperCase(Locale.ROOT));
        language.setTag(tag);
        return language;
    }
}
