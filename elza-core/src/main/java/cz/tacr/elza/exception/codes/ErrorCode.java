package cz.tacr.elza.exception.codes;

/**
 * Rozhraní chybového kódu.
 *
 */
public interface ErrorCode {

    /**
     * Return type of error code
     * 
     * @return
     */
    default String getType() {
        return getClass().getSimpleName();
    }

    default String getCode() {
        return toString();
    }

}
