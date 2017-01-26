package cz.tacr.elza.exception.codes;

/**
 * Kódy pro výstupy.
 *
 * @author Martin Šlapa
 * @since 23.10.2017
 */
public enum OutputCode implements ErrorCode {

    /**
     * Nelze smazat již smazaný výstup.
     */
    ALREADY_DELETED,

    /**
     * Nelze změnit stav.
     */
    CANNOT_CHANGE_STATE,

    /**
     * Nelze provést v tomto stavu.
     */
    NOT_PROCESS_IN_STATE,

    /**
     * Nelze klonovat smazaný výstup.
     */
    CANNOT_CLONE_DELETED,

    /**
     * Výstup je již uzamčen.
     */
    LOCKED,

    /**
     * Tento atribut je počítán automaticky a nemůže být ručně editován
     */
    ITEM_TYPE_CALC,

    /**
     * Nebyl nalezen výstup.
     */
    OUTPUT_NOT_EXISTS,

    /**
     * Nemůže být odstraněn v tomto stavu.
     */
    CANNOT_DELETED_IN_STATE

}
