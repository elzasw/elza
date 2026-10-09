package cz.tacr.elza.domain.vo;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.CoreMessage;
import cz.tacr.elza.core.data.ValidationMessage;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.DataValidationResult.ValidationResultType;
import jakarta.annotation.Nullable;

/**
 * Object for validation results
 *
 */
public class DataValidationResults {

	/**
	 * List of results
	 */
	List<DataValidationResult> results = new LinkedList<>();

    private Set<ArrDescItem> impossibleItems = new HashSet<>();
    private Set<RulItemType> requiredTypes = new HashSet<>();

    /** Code of the package of the rules being executed, see {@link #setPackageCode(String)}. */
    private String packageCode;


	public List<DataValidationResult> getResults() {
		return results;
	}

    /**
     * Create error description. Check error duplicity
     *
     * @param item     error item
     * @param errorMsg error message
     * @return error description or null, if it is duplicate error
     */
    public DataValidationResult createErrorImpossible(final ArrDescItem item,
                                                      final String errorMsg,
                                                      final String policyTypeCode) {
        if (!impossibleItems.contains(item)) {
            impossibleItems.add(item);
            return createError(item, errorMsg, policyTypeCode);
        } else {
            return null;
        }
    }


	/**
	 * Create error description
	 * @param item 		Item with error
	 * @param errorMsg	Error description
	 * @return Object with error description
	 */
    public DataValidationResult createError(final ArrDescItem item, final String errorMsg,
                                            @Nullable final String policyTypeCode) {
        Validate.notNull(item);
        Validate.notNull(errorMsg);

        DataValidationResult result = new DataValidationResult(ValidationResultType.ERROR);
        result.setDescItem(item);
        result.setMessage(errorMsg);
        result.setDescItemId(item.getItemId());
        result.setPolicyTypeCode(policyTypeCode);

        results.add(result);
        return result;
    }

	/**
	 * Create error description
	 * @param descItemId 	Item with error
	 * @param errorMsg		Error description
     * @param policyTypeCode kód typu kontroly
	 * @return Object with error description
	 */
    public DataValidationResult createError(final Integer descItemId,
                                            final String errorMsg,
                                            final String policyTypeCode){
        DataValidationResult result = new DataValidationResult(ValidationResultType.ERROR);
        result.setDescItemId(descItemId);
        result.setMessage(errorMsg);
        result.setPolicyTypeCode(policyTypeCode);

        results.add(result);
        return result;
    }

    /**
     * Create error if item value of type is missing. Check error duplicity
     *
     * @param type missing type
     * @param spec missing specification
     * @return error description or null, if it is duplicate error
     */
    public DataValidationResult createMissingRequired(final RulItemType type,
                                                      @Nullable final RulItemSpec spec,
                                                      final String policyTypeCode) {

        RulItemType inType = spec == null ? type : null;

        if (!requiredTypes.contains(inType)) {
            requiredTypes.add(inType);
            return createMissing(type, spec, policyTypeCode);
        }

        return null;
    }

    public DataValidationResult createMissing(final RulItemType type,
                                              final RulItemSpec spec,
                                              final String policyTypeCode)
    {
        DataValidationResult result = new DataValidationResult(ValidationResultType.MISSING);
        result.setType(type);
        result.setSpec(spec);
        result.setTypeCode(type.getCode());
        result.setPolicyTypeCode(policyTypeCode);

        if (spec == null) {
            result.setMessage(CoreMessage.ARR_MISSING_ITEM.with(
                    ValidationMessage.itemType(type.getCode())).encode());
        } else {
            result.setMessage(CoreMessage.ARR_MISSING_SPEC.with(
                    ValidationMessage.itemType(type.getCode()), ValidationMessage.itemSpec(spec.getCode())).encode());
        }

        results.add(result);
        return result;
    }

    /**
     * Missing item reported by a rule with a translatable message, see
     * {@link ValidationMessage}: the key is qualified by the package of the rule, the text is shown
     * where no translation into the language of the reader exists.
     *
     * @param key
     *            key of the message within the package, or a qualified key ({@code OTHER/KEY})
     * @param text
     *            {@link java.text.MessageFormat} pattern in the language of the package
     */
    public DataValidationResult createMissing(final String typeCode,
                                              final String key,
                                              final String text,
                                              final String policyTypeCode,
                                              final Object... args) {
        return createMissing(typeCode, message(key, text, args), policyTypeCode);
    }

    /**
     * Error reported by a rule with a translatable message, see
     * {@link #createMissing(String, String, String, String, Object...)}.
     */
    public DataValidationResult createError(final Integer descItemId,
                                            final String key,
                                            final String text,
                                            final String policyTypeCode,
                                            final Object... args) {
        return createError(descItemId, message(key, text, args), policyTypeCode);
    }

    private String message(final String key, final String text, final Object... args) {
        return ValidationMessage.of(ValidationMessage.qualify(packageCode, key), text, args).encode();
    }

    /**
     * @param packageCode
     *            code of the package of the rules being executed; qualifies the keys of their
     *            messages
     */
    public void setPackageCode(final String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public DataValidationResult createMissing(final String typeCode,
                                              final String message,
                                              final String policyTypeCode){
        DataValidationResult result = new DataValidationResult(ValidationResultType.MISSING);
        result.setTypeCode(typeCode);
        result.setMessage(message);
        result.setPolicyTypeCode(policyTypeCode);

        results.add(result);

        return result;
    }
}
