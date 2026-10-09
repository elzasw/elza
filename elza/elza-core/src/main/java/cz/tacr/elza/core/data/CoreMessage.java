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
    UNDEFINED_VALUE(0, "undefined"),

    // headers of the CSV exports of a fund (ArrIOService) and of its issues (IssueService)
    EXPORT_COL_RECORD_NUMBER(0, "Record number"),
    EXPORT_COL_NODE_NUMBER(0, "Unit of description number"),
    EXPORT_COL_ITEM_TYPE(0, "Item"),
    EXPORT_COL_SPEC(0, "Specification"),
    EXPORT_COL_VALUE(0, "Value"),
    EXPORT_COL_ENTITY_ID(0, "Entity ID"),
    ISSUES_COL_NUMBER(0, "Number"),
    ISSUES_COL_TYPE(0, "Type"),
    ISSUES_COL_STATE(0, "State"),
    ISSUES_COL_USER(0, "User"),
    ISSUES_COL_USER_NAME(0, "User name"),
    ISSUES_COL_DATE(0, "Date"),
    ISSUES_COL_DESCRIPTION(0, "Description"),
    ISSUES_COL_COMMENTS(0, "Comments"),
    STRUCT_COL_ID(0, "ID"),
    STRUCT_COL_COMPLEMENT(0, "Complement"),
    STRUCT_COL_STATE(0, "State"),
    STRUCT_STATE_OPEN(0, "Open"),
    STRUCT_STATE_CLOSED(0, "Closed"),

    // refused API-key authentication (ApiKeyFailure), the body of the 401 response
    API_KEY_MALFORMED_TOKEN(0, "The token is malformed; use the value issued by the server."),
    API_KEY_UNKNOWN_KEY(0, "No key with this identifier exists."),
    API_KEY_INVALID_SECRET(0, "The secret part of the token does not match."),
    API_KEY_EXPIRED(0, "The key has expired. Create a new one."),
    API_KEY_REVOKED(0, "The key has been revoked. Create a new one."),
    API_KEY_USER_INACTIVE(0, "The user is inactive. Contact the administrator."),

    // why a proposal of the AI assistant cannot be applied (AiProposalService): {0} item type,
    // {1} specification code or value
    AI_PROPOSAL_RULES_FAILED(0, "The rules of the unit of description could not be evaluated."),
    AI_PROPOSAL_NO_OPERATION(0, "The proposal contains no operation."),
    AI_PROPOSAL_UNKNOWN_OPERATION(0, "Unknown kind of proposal operation."),
    AI_PROPOSAL_INCOMPLETE_OPERATION(0, "Incomplete proposal operation."),
    AI_PROPOSAL_UNKNOWN_ITEM_TYPE(1, "Unknown item type: {0}."),
    AI_PROPOSAL_ITEM_NOT_ALLOWED(1, "The item “{0}” cannot be used at this unit of description."),
    AI_PROPOSAL_ITEM_NOT_REPEATABLE(1, "The item “{0}” is not repeatable and the unit of description already has its value."),
    AI_PROPOSAL_TYPE_CHANGE(0, "The proposal changes the type of the item – such a change cannot be made."),
    AI_PROPOSAL_UNKNOWN_CHANGED_ITEM_TYPE(0, "Unknown type of the changed item."),
    AI_PROPOSAL_ITEM_GONE(0, "The changed item no longer exists at the unit of description."),
    AI_PROPOSAL_ITEM_CHANGED_MEANWHILE(0, "The item has been changed in the meantime – the proposal does not match its current value."),
    AI_PROPOSAL_ITEM_READ_ONLY(0, "The item is read-only."),
    AI_PROPOSAL_DATA_TYPE_NOT_PROPOSABLE(1, "The item “{0}” of this data type cannot be changed by a proposal."),
    AI_PROPOSAL_SPEC_MISSING(1, "The proposal does not give the specification of the item “{0}”."),
    AI_PROPOSAL_SPEC_INVALID(2, "Invalid specification “{1}” of the item “{0}”."),
    AI_PROPOSAL_ENTITY_REF_MISSING(1, "The proposal does not give the referenced entity of the item “{0}”."),
    AI_PROPOSAL_ENTITY_NOT_FOUND(0, "The referenced entity was not found."),
    AI_PROPOSAL_VALUE_NOT_STORABLE(2, "The value “{1}” cannot be stored in the item “{0}”."),
    AI_PROPOSAL_VALUE_MISSING(1, "The proposal contains no value of the item “{0}”."),
    AI_PROPOSAL_NODE_MISSING(0, "The proposal does not name a unit of description."),
    AI_PROPOSAL_NODE_NOT_FOUND(0, "The unit of description was not found."),
    AI_PROPOSAL_NODE_UNAVAILABLE(0, "The unit of description is not available.");

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
