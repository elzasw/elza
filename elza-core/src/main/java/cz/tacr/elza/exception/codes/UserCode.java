package cz.tacr.elza.exception.codes;

/**
 * Uživatelské kódy.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public enum UserCode implements ErrorCode {

    /**
     * Osobu nelze smazat, kvůli navázaným uživatelům.
     */
    USER_DELETE_ERROR;

}
