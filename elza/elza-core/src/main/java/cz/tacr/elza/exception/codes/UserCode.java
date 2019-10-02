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
    USER_DELETE_ERROR,

    /**
     * Uživatel neexistuje.
     */
    USER_NOT_FOUND,

    /**
     * Oprávnění neexistuje.
     */
    PERM_NOT_EXIST,

    /**
     * Neplatný vstup oprávnění.
     */
    PERM_ILLEGAL_INPUT,

    /**
     * Uživatel je již ve skupině.
     */
    ALREADY_IN_GROUP,

    /**
     * Uživatel není ve skupině.
     */
    NOT_IN_GROUP,

    /**
     * Skupina s kódem {code} již existuje.
     */
    GROUP_CODE_EXISTS,

    /**
     * Uživatelské jméno již existuje.
     */
    USERNAME_EXISTS,

    /**
     * Původní heslo se neshoduje.
     */
    PASSWORD_NOT_MATCH,

    /**
     * Je třeba změnit i heslo.
     */
    NEED_CHANGE_PASSWORD,

    /**
     * Uživatel není přihlášen.
     */
    USER_NOT_LOGGED

}
