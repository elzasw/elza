package cz.tacr.elza.exception.codes;

/**
 * Kódy pro interakci s externím systémy (export/import...).
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 21. 12. 2016
 */
public enum ExternalCode implements ErrorCode {

    /** Záznam nebyl nalezen. */
    RECORD_NOT_FOUND,

    /** Selhání importu. */
    IMPORT_FAIL,

    /** Nebyl nalezen vyexportovaný rejstřík s id {id} */
    EXTERNAL_RECORD_NOT_FOUND,

    /**
     * Chyba z externího systému.
     */
    EXTERNAL_SYSTEM_ERROR,

    /** Záznam byl již importován. */
    ALREADY_IMPORTED;
}
