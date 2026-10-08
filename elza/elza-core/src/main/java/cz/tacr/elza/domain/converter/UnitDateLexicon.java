package cz.tacr.elza.domain.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Text forms of unit dates per language, read from {@value #RESOURCE}.
 *
 * <p>The file is shared with the client (elza-react {@code src/components/shared/unitdate}), which
 * imports it at build time; the corpus {@code unitdate/cases.json} keeps both parsers in step. The
 * patterns are regular expressions valid in Java and ECMAScript; {@code MONTH} in a pattern stands
 * for the alternation of the language's month names.
 */
public final class UnitDateLexicon {

    public static final String RESOURCE = "/unitdate/lexicon.json";

    /** Placeholder in a date pattern replaced by the alternation of month names. */
    public static final String MONTH_PLACEHOLDER = "MONTH";

    private static final UnitDateLexicon INSTANCE = load();

    /**
     * Language-independent delimiters and number forms.
     */
    public static final class Common {

        /** Delimiter of an interval ("-") */
        public final String intervalDelimiter;

        /** Characters read as the interval delimiter (en dash) */
        public final List<String> intervalDelimiterAliases;

        /** Delimiter of an interval written with spaces (" - "); it may separate dates containing "-" */
        public final String spacedIntervalDelimiter;

        /** Delimiter of an interval whose both ends are estimated ("/") */
        public final String estimateIntervalDelimiter;

        public final List<String> estimateOpen;

        public final List<String> estimateClose;

        /** Year AD, one group */
        public final String year;

        /** Year BC, one group */
        public final String bcYear;

        /** ISO full date, groups year, month, day */
        public final String isoDate;

        /** Time, groups hour, minute, optional second */
        public final String time;

        Common(final JsonNode node) {
            intervalDelimiter = node.get("intervalDelimiter").asText();
            intervalDelimiterAliases = strings(node.get("intervalDelimiterAliases"));
            spacedIntervalDelimiter = node.get("spacedIntervalDelimiter").asText();
            estimateIntervalDelimiter = node.get("estimateIntervalDelimiter").asText();
            estimateOpen = strings(node.get("estimateOpen"));
            estimateClose = strings(node.get("estimateClose"));
            year = node.get("year").asText();
            bcYear = node.get("bcYear").asText();
            isoDate = node.get("isoDate").asText();
            time = node.get("time").asText();
        }
    }

    /**
     * Forms of one language.
     */
    public static final class Language {

        /** BCP 47 tag ({@code cs}, {@code en}) */
        public final String tag;

        /** Marker of a date before Christ, written after the number; no capturing group */
        public final String bc;

        /** Marker of a century, written after the number; no capturing group */
        public final String century;

        /** Year and month, groups month (number or name) and year */
        public final String yearMonth;

        /** Date, groups day, month (number or name) and year */
        public final String date;

        public final List<String> months;

        public final List<String> monthsShort;

        /** Further accepted month names (lower case) to the month number */
        public final Map<String, Integer> monthAliases;

        /** Rendering templates with {@code {placeholder}}s, see {@link UnitDateConverter} */
        public final Map<String, String> render;

        private final Map<String, Integer> monthByName;

        Language(final String tag, final JsonNode node) {
            this.tag = tag;
            months = strings(node.get("months"));
            monthsShort = strings(node.get("monthsShort"));
            Map<String, Integer> aliases = new HashMap<>();
            JsonNode aliasNode = node.get("monthAliases");
            if (aliasNode != null) {
                for (Iterator<Map.Entry<String, JsonNode>> it = aliasNode.fields(); it.hasNext();) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    aliases.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().asInt());
                }
            }
            monthAliases = Collections.unmodifiableMap(aliases);
            Map<String, Integer> byName = new HashMap<>(aliases);
            for (int i = 0; i < months.size(); i++) {
                byName.put(months.get(i).toLowerCase(Locale.ROOT), i + 1);
            }
            for (int i = 0; i < monthsShort.size(); i++) {
                byName.put(monthsShort.get(i).toLowerCase(Locale.ROOT), i + 1);
            }
            monthByName = Collections.unmodifiableMap(byName);
            String monthAlternation = byName.keySet().stream()
                    .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|", "(?:", ")"));
            bc = node.get("bc").asText();
            century = node.get("century").asText();
            yearMonth = node.get("yearMonth").asText().replace(MONTH_PLACEHOLDER, monthAlternation);
            date = node.get("date").asText().replace(MONTH_PLACEHOLDER, monthAlternation);
            Map<String, String> templates = new LinkedHashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.get("render").fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> entry = it.next();
                templates.put(entry.getKey(), entry.getValue().asText());
            }
            render = Collections.unmodifiableMap(templates);
        }

        /**
         * @param text
         *            month number or a month name of this language in any case
         * @return month 1-12
         * @throws IllegalArgumentException
         *             unknown name or number out of range
         */
        public int month(final String text) {
            String value = text.trim();
            if (!value.isEmpty() && Character.isDigit(value.charAt(0))) {
                int month = Integer.parseInt(value);
                if (month < 1 || month > 12) {
                    throw new IllegalArgumentException("Invalid month: " + text);
                }
                return month;
            }
            Integer month = monthByName.get(value.toLowerCase(Locale.ROOT));
            if (month == null) {
                throw new IllegalArgumentException("Unknown month: " + text);
            }
            return month;
        }

        /**
         * @return short name of the month (1-12), the full name when the language has no short names
         */
        public String monthName(final int month) {
            List<String> names = monthsShort.isEmpty() ? months : monthsShort;
            return names.get(month - 1);
        }

        public String template(final String key) {
            String template = render.get(key);
            if (template == null) {
                throw new IllegalStateException("Missing render template '" + key + "' for language " + tag);
            }
            return template;
        }
    }

    private final String defaultLanguageTag;

    private final Common common;

    private final Map<String, Language> languages;

    private UnitDateLexicon(final JsonNode root) {
        defaultLanguageTag = root.get("defaultLanguage").asText();
        common = new Common(root.get("common"));
        Map<String, Language> byTag = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = root.get("languages").fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> entry = it.next();
            byTag.put(entry.getKey(), new Language(entry.getKey(), entry.getValue()));
        }
        if (!byTag.containsKey(defaultLanguageTag)) {
            throw new IllegalStateException("Default language " + defaultLanguageTag + " is not defined in " + RESOURCE);
        }
        languages = Collections.unmodifiableMap(byTag);
    }

    public static UnitDateLexicon get() {
        return INSTANCE;
    }

    private static UnitDateLexicon load() {
        try (InputStream stream = UnitDateLexicon.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE);
            }
            return new UnitDateLexicon(new ObjectMapper().readTree(stream));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + RESOURCE, e);
        }
    }

    public Common common() {
        return common;
    }

    public Language defaultLanguage() {
        return languages.get(defaultLanguageTag);
    }

    /**
     * Languages in the order of the file, the default language first.
     */
    public Collection<Language> languages() {
        return languages.values();
    }

    /**
     * @param tag
     *            BCP 47 tag; a tag with a region falls back to its language; null or unknown gives
     *            the default language
     */
    public Language language(final String tag) {
        if (tag == null) {
            return defaultLanguage();
        }
        Language language = languages.get(tag);
        if (language == null) {
            int dash = tag.indexOf('-');
            if (dash > 0) {
                language = languages.get(tag.substring(0, dash));
            }
        }
        return language != null ? language : defaultLanguage();
    }

    private static List<String> strings(final JsonNode node) {
        if (node == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            result.add(item.asText());
        }
        return Collections.unmodifiableList(result);
    }
}
