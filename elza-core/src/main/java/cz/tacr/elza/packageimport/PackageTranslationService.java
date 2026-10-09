package cz.tacr.elza.packageimport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tacr.elza.domain.RulPackage;
import cz.tacr.elza.domain.RulTranslation;
import cz.tacr.elza.domain.SysLanguage;
import cz.tacr.elza.domain.TranslationEntityType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.PackageCode;
import cz.tacr.elza.packageimport.xml.Translation;
import cz.tacr.elza.packageimport.xml.Translations;
import cz.tacr.elza.repository.RulTranslationRepository;
import cz.tacr.elza.repository.SysLanguageRepository;
import jakarta.persistence.EntityManager;

/**
 * Translation files of rules packages ({@code translations/<tag>.xml}): import, export, removal and
 * the check for orphaned and outdated translations.
 */
@Service
public class PackageTranslationService {

    private static final Logger logger = LoggerFactory.getLogger(PackageTranslationService.class);

    public static final String TRANSLATIONS_DIR = "translations/";

    private static final String XML_EXTENSION = ".xml";

    /** Number of hex characters of the SHA-256 hash stored in {@code source_hash}. */
    private static final int SOURCE_HASH_LENGTH = 16;

    /** Length of {@code rul_translation.source_hash}. */
    private static final int SOURCE_HASH_MAX_LENGTH = 20;

    /** Length of {@code rul_translation.entity_code}. */
    private static final int CODE_MAX_LENGTH = 100;

    /**
     * Problem of a stored translation.
     */
    public enum IssueKind {
        /** The translated entity or message does not exist. */
        ORPHAN,
        /** The source text changed since the translation was made. */
        OUTDATED
    }

    public record TranslationIssue(IssueKind kind, String entityType, String entityCode, String field,
                                   String languageTag) {
    }

    private final RulTranslationRepository translationRepository;
    private final SysLanguageRepository sysLanguageRepository;
    private final EntityManager entityManager;

    @Autowired
    public PackageTranslationService(RulTranslationRepository translationRepository,
                                     SysLanguageRepository sysLanguageRepository,
                                     EntityManager entityManager) {
        this.translationRepository = translationRepository;
        this.sysLanguageRepository = sysLanguageRepository;
        this.entityManager = entityManager;
    }

    /**
     * Source language of a package from {@code <language>} in {@code package.xml}; Czech when the
     * element is missing.
     */
    public SysLanguage resolvePackageLanguage(String tag) {
        String languageTag = StringUtils.isBlank(tag) ? "cs" : tag.trim();
        SysLanguage language = sysLanguageRepository.findByTagIgnoreCase(languageTag);
        if (language == null) {
            throw new BusinessException("Unknown language of the package: " + languageTag, PackageCode.CODE_NOT_FOUND)
                    .set("code", languageTag)
                    .set("file", PackageContext.PACKAGE_XML);
        }
        return language;
    }

    /**
     * Replaces the translations of the imported package by the content of its translation files.
     * Has to run after all entities of the package were saved, so orphans can be detected and
     * source hashes computed against them.
     *
     * The source hash of a translation is the hash of the source text it was made from: taken from
     * the {@code src-hash} attribute when the file has it; otherwise kept from the stored row when
     * the translated text did not change (the translation still renders the source text it was
     * made from); otherwise computed from the current source text (a new or changed translation).
     */
    public void importTranslations(PackageContext pkgCtx) {
        RulPackage rulPackage = pkgCtx.getPackage();
        SysLanguage sourceLanguage = rulPackage.getLanguage();

        Map<String, String> ownMessages = new HashMap<>();
        SourceTexts sourceTexts = new SourceTexts(rulPackage, ownMessages);

        List<RulTranslation> rows = new ArrayList<>();
        Map<Integer, String> filesByLanguage = new HashMap<>();
        for (String file : translationFiles(pkgCtx)) {
            Translations translations = pkgCtx.convertXmlStreamToObject(Translations.class, file);
            readFile(file, translations, rulPackage, sourceLanguage, sourceTexts, filesByLanguage, rows, ownMessages);
        }

        Map<String, RulTranslation> previous = new HashMap<>();
        for (RulTranslation row : translationRepository.findByRulPackage(rulPackage)) {
            previous.put(rowKey(row), row);
        }

        for (RulTranslation row : rows) {
            if (isOwnMessage(row, rulPackage)) {
                continue;
            }
            TranslationEntityType type = TranslationEntityType.valueOf(row.getEntityType());
            if (!sourceTexts.isChecked(type)) {
                continue;
            }
            String source = sourceTexts.get(type, row.getEntityCode(), row.getField());
            if (source == null && type != TranslationEntityType.MESSAGE) {
                // a message without a source row is defined by a rule, with its text there
                logger.warn("Package {}: translation of a missing text {}.{}.{} ({}) is kept as orphan",
                            rulPackage.getCode(), row.getEntityType(), row.getEntityCode(), row.getField(),
                            row.getLanguage().getTag());
            }
            if (row.getSourceHash() != null) {
                continue;
            }
            RulTranslation before = previous.get(rowKey(row));
            if (before != null && before.getSourceHash() != null
                    && StringUtils.equals(before.getTextValue(), row.getTextValue())) {
                row.setSourceHash(before.getSourceHash());
            } else if (source != null) {
                row.setSourceHash(sourceHash(source));
            }
        }

        translationRepository.deleteByRulPackage(rulPackage);
        translationRepository.flush();
        translationRepository.saveAll(rows);
    }

    /**
     * Translations of a package grouped into files by language, rows sorted by type, code and
     * field.
     *
     * @return file name -> content
     */
    public Map<String, Translations> exportTranslations(RulPackage rulPackage) {
        Map<String, Translations> files = new LinkedHashMap<>();
        List<RulTranslation> rows = translationRepository.findByRulPackageOrdered(rulPackage);
        rows.sort(Comparator.comparing((RulTranslation t) -> t.getLanguage().getTag())
                .thenComparing(RulTranslation::getEntityType)
                .thenComparing(RulTranslation::getEntityCode)
                .thenComparing(RulTranslation::getField));
        for (RulTranslation row : rows) {
            String tag = row.getLanguage().getTag();
            Translations file = files.computeIfAbsent(TRANSLATIONS_DIR + tag + XML_EXTENSION, k -> {
                Translations t = new Translations();
                t.setLang(tag);
                t.setTranslations(new ArrayList<>());
                return t;
            });
            Translation t = new Translation();
            t.setType(row.getEntityType());
            t.setCode(row.getEntityCode());
            t.setField(row.getField());
            t.setSrcHash(row.getSourceHash());
            t.setValue(row.getTextValue());
            file.getTranslations().add(t);
        }
        return files;
    }

    public void deleteTranslations(RulPackage rulPackage) {
        translationRepository.deleteByRulPackage(rulPackage);
    }

    /**
     * Orphaned and outdated translations of a package. Type groups have no stored source text and
     * are not checked.
     */
    public List<TranslationIssue> checkTranslations(RulPackage rulPackage) {
        List<TranslationIssue> issues = new ArrayList<>();
        SourceTexts sourceTexts = new SourceTexts(rulPackage, null);
        for (RulTranslation row : translationRepository.findByRulPackageOrdered(rulPackage)) {
            TranslationEntityType type = TranslationEntityType.fromCode(row.getEntityType());
            if (type == null || !sourceTexts.isChecked(type) || isOwnMessage(row, rulPackage)) {
                continue;
            }
            String source = sourceTexts.get(type, row.getEntityCode(), row.getField());
            if (source == null) {
                // a message without a source row is defined by a rule, with its text there
                if (type != TranslationEntityType.MESSAGE) {
                    issues.add(issue(IssueKind.ORPHAN, row));
                }
            } else if (row.getSourceHash() != null && !row.getSourceHash().equals(sourceHash(source))) {
                issues.add(issue(IssueKind.OUTDATED, row));
            }
        }
        return issues;
    }

    /**
     * Short hash of a source text, stored with a translation made from it.
     */
    public static String sourceHash(String sourceText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sourceText.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(SOURCE_HASH_LENGTH);
            for (int i = 0; sb.length() < SOURCE_HASH_LENGTH; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException("SHA-256 is not available", e);
        }
    }

    /**
     * A message defined by the package itself: its row in the package's source language is the
     * source text, not a translation.
     */
    private static boolean isOwnMessage(RulTranslation row, RulPackage rulPackage) {
        return TranslationEntityType.MESSAGE.name().equals(row.getEntityType())
                && row.getLanguageId().equals(rulPackage.getLanguageId())
                && row.getEntityCode().startsWith(rulPackage.getCode() + TranslationEntityType.CODE_SEPARATOR);
    }

    private static String rowKey(RulTranslation row) {
        return row.getEntityType() + "|" + row.getEntityCode() + "|" + row.getField() + "|" + row.getLanguageId();
    }

    private static TranslationIssue issue(IssueKind kind, RulTranslation row) {
        return new TranslationIssue(kind, row.getEntityType(), row.getEntityCode(), row.getField(),
                row.getLanguage().getTag());
    }

    /**
     * Translation files of the package: {@code translations/<tag>.xml}, no subdirectories.
     */
    private static Set<String> translationFiles(PackageContext pkgCtx) {
        Set<String> files = new TreeSet<>();
        for (String key : pkgCtx.getByteStreamKeys()) {
            if (key.startsWith(TRANSLATIONS_DIR) && key.endsWith(XML_EXTENSION)
                    && key.indexOf('/', TRANSLATIONS_DIR.length()) < 0) {
                files.add(key);
            }
        }
        return files;
    }

    private void readFile(String file, Translations translations, RulPackage rulPackage, SysLanguage sourceLanguage,
                          SourceTexts sourceTexts, Map<Integer, String> filesByLanguage,
                          List<RulTranslation> rows, Map<String, String> ownMessages) {
        String fileTag = file.substring(TRANSLATIONS_DIR.length(), file.length() - XML_EXTENSION.length());
        if (translations == null || !fileTag.equalsIgnoreCase(StringUtils.trimToEmpty(translations.getLang()))) {
            throw invalid(file, "lang", "The lang attribute must be equal to the file name: " + fileTag);
        }
        SysLanguage language = sysLanguageRepository.findByTagIgnoreCase(fileTag);
        if (language == null) {
            throw new BusinessException("Unknown language of translation file: " + fileTag, PackageCode.CODE_NOT_FOUND)
                    .set("code", fileTag)
                    .set("file", file);
        }
        String otherFile = filesByLanguage.putIfAbsent(language.getLanguageId(), file);
        if (otherFile != null) {
            throw invalid(file, "lang", "Another file has the same language: " + otherFile);
        }
        boolean sourceFile = language.getLanguageId().equals(sourceLanguage.getLanguageId());
        Locale locale = Locale.forLanguageTag(language.getTag());

        Set<String> keys = new HashSet<>();
        List<Translation> items = translations.getTranslations() != null ? translations.getTranslations() : List.of();
        for (Translation t : items) {
            String key = t.getType() + "." + t.getCode() + "." + t.getField();
            TranslationEntityType type = TranslationEntityType.fromCode(t.getType());
            if (type == null) {
                throw invalid(file, key, "Unknown type, allowed: " + List.of(TranslationEntityType.values()));
            }
            if (StringUtils.isBlank(t.getCode())) {
                throw invalid(file, key, "Missing code");
            }
            if (t.getCode().length() > CODE_MAX_LENGTH) {
                throw invalid(file, key, "Code longer than " + CODE_MAX_LENGTH + " characters");
            }
            if (StringUtils.isBlank(t.getField()) || !type.isFieldAllowed(t.getField())) {
                throw invalid(file, key, "Missing or not allowed field, allowed: " + new TreeSet<>(type.getFields()));
            }
            if (StringUtils.isBlank(t.getValue())) {
                throw invalid(file, key, "Missing text");
            }
            if (t.getSrcHash() != null && (t.getSrcHash().isBlank() || t.getSrcHash().length() > SOURCE_HASH_MAX_LENGTH)) {
                throw invalid(file, key, "Invalid src-hash");
            }
            if (!keys.add(key)) {
                throw invalid(file, key, "Duplicate key");
            }
            boolean ownMessage = false;
            if (type == TranslationEntityType.MESSAGE || type == TranslationEntityType.TYPE_GROUP) {
                int separator = t.getCode().indexOf(TranslationEntityType.CODE_SEPARATOR);
                if (separator <= 0) {
                    throw invalid(file, key, "Code has to be qualified: <PACKAGE or RULE SET>/<CODE>");
                }
                ownMessage = type == TranslationEntityType.MESSAGE && sourceFile
                        && t.getCode().substring(0, separator).equals(rulPackage.getCode());
            }
            if (type == TranslationEntityType.MESSAGE) {
                try {
                    new MessageFormat(t.getValue(), locale);
                } catch (IllegalArgumentException e) {
                    throw invalid(file, key, "Invalid message pattern: " + e.getMessage());
                }
            }
            // The source texts of the package's own entities are in its entity files; a row in its
            // source language for an entity of ANOTHER package overrides that package's text.
            if (sourceFile && type != TranslationEntityType.MESSAGE
                    && sourceTexts.isOwn(type, t.getCode(), rulPackage.getCode())) {
                logger.warn("Package {}: {} translates its own {} into its source language, skipped",
                            rulPackage.getCode(), file, key);
                continue;
            }
            if (ownMessage) {
                ownMessages.put(t.getCode(), t.getValue());
            }

            RulTranslation row = new RulTranslation();
            row.setRulPackage(rulPackage);
            row.setEntityType(type.name());
            row.setEntityCode(t.getCode());
            row.setField(t.getField());
            row.setLanguage(language);
            row.setTextValue(t.getValue());
            row.setSourceHash(t.getSrcHash());
            rows.add(row);
        }
    }

    private static BusinessException invalid(String file, String key, String reason) {
        return (BusinessException) new BusinessException("Invalid translation " + key + " in " + file + ": " + reason,
                PackageCode.INVALID_TRANSLATION)
                .set("file", file)
                .set("key", key)
                .set("reason", reason);
    }

    /**
     * Current source texts and owning packages of translated entities and messages, loaded per
     * type and field on first use.
     */
    private class SourceTexts {

        private final RulPackage rulPackage;
        /** Messages defined by the package being imported; null outside an import. */
        private final Map<String, String> ownMessages;
        private final Map<String, Map<String, String>> cache = new HashMap<>();

        SourceTexts(RulPackage rulPackage, Map<String, String> ownMessages) {
            this.rulPackage = rulPackage;
            this.ownMessages = ownMessages;
        }

        /** Type groups have no source text in the database. */
        boolean isChecked(TranslationEntityType type) {
            return type != TranslationEntityType.TYPE_GROUP;
        }

        String get(TranslationEntityType type, String code, String field) {
            if (type == TranslationEntityType.MESSAGE) {
                return message(code);
            }
            return cache.computeIfAbsent(type.name() + "." + field, k -> load(type, "e." + field)).get(code);
        }

        /**
         * The entity belongs to the package: its owner, or for an entity class one of the packages
         * declaring it.
         */
        boolean isOwn(TranslationEntityType type, String code, String packageCode) {
            if (type == TranslationEntityType.AP_TYPE || type == TranslationEntityType.PART_TYPE
                    || type == TranslationEntityType.ITEM_TYPE || type == TranslationEntityType.ITEM_SPEC) {
                Set<String> declaring = declarations(type).get(code);
                return declaring != null && declaring.contains(packageCode);
            }
            return packageCode.equals(ownerPackageCode(type, code));
        }

        private final Map<TranslationEntityType, Map<String, Set<String>>> declarations = new HashMap<>();

        /** Codes of the packages declaring each entity class, part type, item type or specification. */
        private Map<String, Set<String>> declarations(TranslationEntityType type) {
            return declarations.computeIfAbsent(type, t -> {
                String query = switch (t) {
                    case AP_TYPE -> "SELECT d.apType.code, d.rulPackage.code FROM rul_ap_type_declaration d";
                    case PART_TYPE -> "SELECT d.partType.code, d.rulPackage.code FROM rul_part_type_declaration d";
                    case ITEM_SPEC -> "SELECT d.itemSpec.code, d.rulPackage.code FROM rul_item_spec_declaration d";
                    default -> "SELECT d.itemType.code, d.rulPackage.code FROM rul_item_type_declaration d";
                };
                Map<String, Set<String>> result = new HashMap<>();
                for (Object[] r : entityManager.createQuery(query, Object[].class).getResultList()) {
                    result.computeIfAbsent((String) r[0], k -> new HashSet<>()).add((String) r[1]);
                }
                return result;
            });
        }

        /**
         * Code of the package owning the translated entity; for a type group the package owning
         * its rule set. Null when the entity does not exist.
         */
        String ownerPackageCode(TranslationEntityType type, String code) {
            if (type == TranslationEntityType.MESSAGE) {
                return null;
            }
            if (type == TranslationEntityType.TYPE_GROUP) {
                String ruleSet = code.substring(0, code.indexOf(TranslationEntityType.CODE_SEPARATOR));
                return cache.computeIfAbsent("owner.RULE_SET", k -> load(TranslationEntityType.RULE_SET, "e.rulPackage.code"))
                        .get(ruleSet);
            }
            return cache.computeIfAbsent("owner." + type.name(), k -> load(type, "e.rulPackage.code")).get(code);
        }

        private Map<String, String> load(TranslationEntityType type, String selected) {
            // entity name and selected path come from TranslationEntityType and this class, never from the file
            String where = type == TranslationEntityType.TEMPLATE ? " WHERE e.deleted = false" : "";
            List<Object[]> result = entityManager
                    .createQuery("SELECT e.code, " + selected + " FROM " + type.getEntityName() + " e" + where,
                                 Object[].class)
                    .getResultList();
            Map<String, String> texts = new HashMap<>(result.size());
            for (Object[] r : result) {
                if (r[1] != null) {
                    texts.put((String) r[0], (String) r[1]);
                }
            }
            return texts;
        }

        private String message(String code) {
            String packageCode = code.substring(0, code.indexOf(TranslationEntityType.CODE_SEPARATOR));
            if (ownMessages != null && packageCode.equals(rulPackage.getCode())) {
                return ownMessages.get(code);
            }
            Map<String, String> messages = cache.computeIfAbsent(TranslationEntityType.MESSAGE.name() + "." + packageCode,
                    k -> loadMessages(packageCode));
            return messages.get(code);
        }

        private Map<String, String> loadMessages(String packageCode) {
            List<Object[]> result = entityManager.createQuery(
                    "SELECT t.entityCode, t.textValue FROM rul_translation t JOIN t.rulPackage p"
                            + " WHERE p.code = :code AND t.entityType = :type AND t.languageId = p.languageId",
                    Object[].class)
                    .setParameter("code", packageCode)
                    .setParameter("type", TranslationEntityType.MESSAGE.name())
                    .getResultList();
            Map<String, String> texts = new HashMap<>(result.size());
            for (Object[] r : result) {
                texts.put((String) r[0], (String) r[1]);
            }
            return texts;
        }
    }
}
