package cz.tacr.elza.exception.codes;

public enum BulkActionCode
        implements ErrorCode {
    /**
     * Bulk action configuration error
     * 
     * Parameters:
     * - code: code of bulk action
     */
    INCORRECT_CONFIG,
    
    /**
     * UnitId is not sealed
     *
     * Parameters:
     * - unitId: unitId which should be sealed but is not
     */
    UNITID_NOT_SEALED,

    /**
     * Prefix of the generated structured object cannot be determined
     *
     * The source item is missing, is filled more than once, has no specification
     * or its specification has no prefix defined in the action configuration.
     *
     * Parameters:
     * - nodeId: level which cannot be processed
     * - itemType: code of the item type used as the prefix source
     * - itemSpec: code of the specification without prefix mapping (optional)
     * - count: number of found source items (optional)
     */
    PREFIX_VALUE_NOT_FOUND
}
