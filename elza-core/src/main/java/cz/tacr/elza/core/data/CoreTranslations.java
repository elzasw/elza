package cz.tacr.elza.core.data;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.tacr.elza.domain.RulTranslation;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.TranslationEntityType;
import cz.tacr.elza.packageimport.xml.Translation;
import cz.tacr.elza.packageimport.xml.Translations;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * Translations shipped with the core: the texts of {@link CoreMessage} in the languages ELZA
 * comes with, in files {@code translations/<tag>.xml} on the classpath written like the
 * translation files of a rules package. They form the lowest layer of
 * {@link PackageTranslations}, so a rules package overrides them like any other text.
 */
public final class CoreTranslations {

    private static final Logger logger = LoggerFactory.getLogger(CoreTranslations.class);

    /** Directory of the files on the classpath. */
    public static final String RESOURCE_DIR = "translations/";

    private CoreTranslations() {
    }

    /**
     * Rows of the shipped files of the languages; a language without a file contributes nothing.
     * A row with a broken pattern is left out with an error in the log - the shipped files are
     * checked by a test, so this guards a broken build only.
     */
    public static List<RulTranslation> load(Collection<SysLanguage> languages) {
        List<RulTranslation> rows = new ArrayList<>();
        for (SysLanguage language : languages) {
            Translations translations = read(language.getTag());
            if (translations == null) {
                continue;
            }
            for (Translation t : translations.getTranslations()) {
                TranslationEntityType type = TranslationEntityType.fromCode(t.getType());
                if (type == null || t.getCode() == null || t.getField() == null || t.getValue() == null
                        || !type.isFieldAllowed(t.getField())) {
                    logger.error("Core translations {}: invalid row {}.{}.{}", language.getTag(), t.getType(),
                                 t.getCode(), t.getField());
                    continue;
                }
                if (type == TranslationEntityType.MESSAGE && !isValidPattern(t.getValue(), language.getTag())) {
                    logger.error("Core translations {}: invalid message pattern of {}", language.getTag(), t.getCode());
                    continue;
                }
                RulTranslation row = new RulTranslation();
                row.setEntityType(type.name());
                row.setEntityCode(t.getCode());
                row.setField(t.getField());
                row.setLanguage(language);
                row.setTextValue(t.getValue());
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Texts of the messages of the shipped file of a language: key -> pattern.
     *
     * @return the messages, null when the language has no file
     */
    public static Map<String, String> messages(String tag) {
        Translations translations = read(tag);
        if (translations == null) {
            return null;
        }
        Map<String, String> messages = new LinkedHashMap<>();
        for (Translation t : translations.getTranslations()) {
            if (TranslationEntityType.MESSAGE.name().equals(t.getType())) {
                messages.put(t.getCode(), t.getValue());
            }
        }
        return messages;
    }

    /**
     * @return the shipped file of the language, null when there is none
     */
    static Translations read(String tag) {
        String resource = RESOURCE_DIR + tag + ".xml";
        try (InputStream in = CoreTranslations.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return (Translations) JAXBContext.newInstance(Translations.class).createUnmarshaller().unmarshal(in);
        } catch (IOException | JAXBException e) {
            throw new IllegalStateException("Cannot read the core translations " + resource, e);
        }
    }

    private static boolean isValidPattern(String pattern, String tag) {
        try {
            new MessageFormat(pattern, Locale.forLanguageTag(tag));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
