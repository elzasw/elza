package cz.tacr.elza.domain.bridge;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;

/**
 * Normal forms of a name for the exact match of the whole search text against a field of an index
 * type (the fields of {@code index-search} with {@code index-type}). The index value and the search
 * text go through the same form, so the comparison ignores case, punctuation and the order of the
 * words: "Petr Pytelka", "pytelka, petr" and "PYTELKA Petr" are one form. The transliterated form
 * drops the diacritics as well.
 */
public final class IndexValueNormalizer {

    /** Anything that is not a letter, a digit or white space. */
    private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}\\s]+");

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private IndexValueNormalizer() {
    }

    /**
     * @return lower case, punctuation removed, words sorted; an empty string for a value without
     *         words
     */
    public static String exact(String value) {
        return sortedWords(value.toLowerCase(Locale.ROOT));
    }

    /**
     * @return {@link #exact(String)} without diacritics
     */
    public static String transliteratedExact(String value) {
        return sortedWords(removeDiacritics(value.toLowerCase(Locale.ROOT)));
    }

    /**
     * @return the value folded to ASCII (Lucene ASCII folding), e.g. "Němcová" to "Nemcova"
     */
    public static String removeDiacritics(String value) {
        char[] input = value.toCharArray();
        // folding may expand a character to several (e.g. a ligature)
        char[] output = new char[Math.max(4 * input.length, 1)];
        int length = ASCIIFoldingFilter.foldToASCII(input, 0, output, 0, input.length);
        return new String(output, 0, length);
    }

    private static String sortedWords(String value) {
        String stripped = PUNCTUATION.matcher(value).replaceAll(" ").trim();
        if (stripped.isEmpty()) {
            return "";
        }
        String[] words = WHITESPACE.split(stripped);
        Arrays.sort(words);
        return String.join(" ", words);
    }
}
