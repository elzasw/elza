package cz.tacr.elza.domain.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * The exact form of a name: the index value and the search text meet whatever the case, the
 * punctuation and the order of the words; the transliterated form also whatever the diacritics.
 */
class IndexValueNormalizerTest {

    @Test
    void exactIgnoresCasePunctuationAndTheOrderOfWords() {
        assertEquals("petr pytelka", IndexValueNormalizer.exact("Petr Pytelka"));
        assertEquals("petr pytelka", IndexValueNormalizer.exact("pytelka, petr"));
        assertEquals("petr pytelka", IndexValueNormalizer.exact("  PYTELKA   Petr "));
        assertEquals("akademie věd", IndexValueNormalizer.exact("Akademie věd."));
        assertEquals("brien o", IndexValueNormalizer.exact("O'Brien"));
        assertEquals("müller thurgau", IndexValueNormalizer.exact("Müller-Thurgau"));
    }

    @Test
    void exactKeepsTheDiacriticsAndTheTransliteratedFormDropsThem() {
        assertEquals("božena němcová", IndexValueNormalizer.exact("Božena Němcová"));
        assertNotEquals(IndexValueNormalizer.exact("Němcová"), IndexValueNormalizer.exact("Nemcova"));
        assertEquals("bozena nemcova", IndexValueNormalizer.transliteratedExact("Božena Němcová"));
        assertEquals(IndexValueNormalizer.transliteratedExact("Božena Němcová"),
                IndexValueNormalizer.transliteratedExact("nemcova, bozena"));
    }

    @Test
    void aValueWithoutWordsIsEmpty() {
        assertEquals("", IndexValueNormalizer.exact(""));
        assertEquals("", IndexValueNormalizer.exact(" - ( ) "));
        assertEquals("", IndexValueNormalizer.transliteratedExact("..."));
    }

    @Test
    void removeDiacriticsFoldsToAscii() {
        assertEquals("Nemcova", IndexValueNormalizer.removeDiacritics("Němcová"));
        assertEquals("", IndexValueNormalizer.removeDiacritics(""));
    }
}
