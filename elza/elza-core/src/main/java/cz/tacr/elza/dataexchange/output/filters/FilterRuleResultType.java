package cz.tacr.elza.dataexchange.output.filters;

/**
 * Type of result
 */
public enum FilterRuleResultType {
    /**
     * Continue with next rule
     */
    RESULT_CONTINUE,

    /**
     * Break evaluation of rules
     */
    RESULT_BREAK
}
