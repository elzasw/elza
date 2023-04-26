package cz.tacr.elza.print.format;

/**
 * Expression builder
 *
 */
public final class ExpressionBuilder {

    /**
     * Check if value exists
     * 
     * @param itemType
     * @return
     */
    public static Expression hasValue(final String itemType) {
        return new ExpressionHasValue(itemType);
    }

}
