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
    UNITID_NOT_SEALED
}
