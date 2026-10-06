package cz.tacr.elza.domain;

import java.util.Set;

/**
 * Kinds of package-provided texts that can be translated ({@code rul_translation.entity_type}).
 *
 * Entity kinds name the JPA entity whose {@code code} identifies the translated row and list the
 * text fields that may be translated. {@link #TYPE_GROUP} and {@link #MESSAGE} have no entity:
 * type groups live in the UI settings of a rule set (code {@code <RULE_SET>/<GROUP>}), messages
 * are defined by the source-language translation file of their package (code
 * {@code <PACKAGE>/<KEY>}).
 */
public enum TranslationEntityType {

    ITEM_TYPE("rul_item_type", TranslationEntityType.NAME, TranslationEntityType.SHORTCUT, TranslationEntityType.DESCRIPTION),
    ITEM_SPEC("rul_item_spec", TranslationEntityType.NAME, TranslationEntityType.SHORTCUT, TranslationEntityType.DESCRIPTION),
    RULE_SET("rul_rule_set", TranslationEntityType.NAME),
    AP_TYPE("ap_type", TranslationEntityType.NAME),
    PART_TYPE("rul_part_type", TranslationEntityType.NAME),
    STRUCTURED_TYPE("rul_structured_type", TranslationEntityType.NAME),
    POLICY_TYPE("rul_policy_type", TranslationEntityType.NAME),
    OUTPUT_TYPE("rul_output_type", TranslationEntityType.NAME),
    TEMPLATE("rul_template", TranslationEntityType.NAME),
    ARRANGEMENT_EXTENSION("rul_arrangement_extension", TranslationEntityType.NAME),
    ISSUE_TYPE("wf_issue_type", TranslationEntityType.NAME),
    ISSUE_STATE("wf_issue_state", TranslationEntityType.NAME),
    TYPE_GROUP(null, TranslationEntityType.NAME),
    MESSAGE(null, TranslationEntityType.TEXT);

    public static final String NAME = "name";
    public static final String SHORTCUT = "shortcut";
    public static final String DESCRIPTION = "description";
    public static final String TEXT = "text";

    /**
     * Separator of the qualified codes of {@link #TYPE_GROUP} and {@link #MESSAGE}.
     */
    public static final String CODE_SEPARATOR = "/";

    private final String entityName;

    private final Set<String> fields;

    TranslationEntityType(String entityName, String... fields) {
        this.entityName = entityName;
        this.fields = Set.of(fields);
    }

    /**
     * @return JPA entity name of the translated entity, null for kinds without an entity
     */
    public String getEntityName() {
        return entityName;
    }

    public Set<String> getFields() {
        return fields;
    }

    public boolean isFieldAllowed(String field) {
        return fields.contains(field);
    }

    public static TranslationEntityType fromCode(String code) {
        for (TranslationEntityType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
