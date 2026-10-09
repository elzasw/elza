package cz.tacr.elza.core.data;

import cz.tacr.elza.domain.TranslationEntityType;

/**
 * Messages of the core shown to users: the catalog of the keys {@code CORE/<name>} with the text
 * the developer wrote (any language; the fallback when no translation exists) and the number of
 * arguments.
 *
 * <p>The texts shown come from the translation files shipped with the core,
 * {@code elza-core/src/main/resources/translations/<tag>.xml}, in the format of the translation
 * files of a rules package ({@code <t type="MESSAGE" code="CORE/AP_MISSING_REQUIRED_ITEM"
 * field="text">}); a rules package overrides a text with a row of the same code. Every message
 * has a row in every shipped language ({@code CoreMessagesTest}).
 *
 * <p>A message is built with {@link #with(Object...)} and stored or rendered as a
 * {@link ValidationMessage}. Arguments naming an item type, a specification or a part type are
 * passed as the objects themselves or as {@link ValidationMessage#itemType(String)} and its
 * siblings; a message in the place of an argument ({@link #UNDEFINED_VALUE}) is rendered as its
 * text.
 */
public enum CoreMessage {

    // entity validation (RuleService): {0} part type, {1} item type code, {2} item type
    AP_MISSING_REQUIRED_ITEM(3, "The part {0} is missing the required item {1} - {2}"),
    AP_IMPOSSIBLE_ITEM(3, "The part {0} contains the forbidden item {1} - {2}"),
    AP_ITEM_NOT_REPEATABLE(3, "The part {0} contains the item {1} - {2} more than once."),
    // {0} part type, {1} specification code, {2} specification
    AP_IMPOSSIBLE_SPEC(3, "The part {0} contains the forbidden specification {1} - {2}"),
    AP_RELATION_NOT_REPEATABLE(3, "The part {0} contains the relation {1} - {2} more than once."),
    AP_IDENT_NOT_REPEATABLE(3, "The part {0} contains the external identifier {1} - {2} more than once."),
    // {0} specification code, {1} specification
    AP_ENTITY_RELATION_NOT_REPEATABLE(2, "The entity contains the relation {0} - {1} more than once."),
    // {0} part type, {1} index type, {2} index value
    AP_DUPLICATE_INDEX(3, "The part {0} contains a duplicate index of type {1} with the value {2}"),
    // {0} part type
    AP_INVALID_ENTITY_REF(1, "The part {0} refers to an invalidated entity"),
    AP_DUPLICATE_KEY_VALUE(0, "Duplicate key value of the entity."),

    // fund validation (Validator, DataValidationResults): {0} item type
    ARR_MISSING_ITEM(1, "The item {0} must be filled in."),
    ARR_ITEM_IMPOSSIBLE(1, "The item {0} cannot be used at this unit of description."),
    ARR_ITEM_NOT_ALLOWED(1, "The item {0} is not possible at this unit of description."),
    ARR_ITEM_NOT_REPEATABLE(1, "The item {0} is not repeatable."),
    // {0} item type, {1} specification
    ARR_MISSING_SPEC(2, "The item {0} with the specification {1} must be filled in."),
    ARR_SPEC_IMPOSSIBLE(2, "The item {0} with the specification {1} cannot be used at this unit of description."),
    // {0} specification
    ARR_SPEC_NOT_REPEATABLE(1, "The specification {0} is not repeatable."),
    // {0} item type, {1} the value (UNDEFINED_VALUE)
    ARR_UNDEFINED_NOT_ALLOWED(2, "The item {0} cannot have the value “{1}”."),
    // {0} item type, {1} id of the entity
    ARR_DELETED_ENTITY_REF(2, "The item {0} refers to an invalidated entity (id: {1})."),

    // terms used as arguments of other messages
    /** The word for a value marked as undefined ({@code ArrangementService.UNDEFINED}). */
    UNDEFINED_VALUE(0, "undefined");

    /** Code of the "package" of the core in the keys of its messages. */
    public static final String PACKAGE = ValidationMessage.CORE;

    private final int arity;

    private final String text;

    CoreMessage(int arity, String text) {
        this.arity = arity;
        this.text = text;
    }

    /**
     * @return qualified key, {@code CORE/<name>}
     */
    public String key() {
        return PACKAGE + TranslationEntityType.CODE_SEPARATOR + name();
    }

    /**
     * @return the text the developer wrote, a {@link java.text.MessageFormat} pattern; shown
     *         when no translation exists
     */
    public String text() {
        return text;
    }

    /**
     * @return number of arguments the message takes
     */
    public int arity() {
        return arity;
    }

    /**
     * The message with its arguments, see {@link ValidationMessage#of(String, String, Object...)}.
     */
    public ValidationMessage with(Object... args) {
        return ValidationMessage.of(key(), text, args);
    }
}
