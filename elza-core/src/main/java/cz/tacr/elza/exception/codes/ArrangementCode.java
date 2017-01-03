package cz.tacr.elza.exception.codes;

/**
 * Kódy pro pořádání.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public enum ArrangementCode implements ErrorCode {

    /**
     * Verze AS je již uzavřena.
     */
    VERSION_ALREADY_CLOSED,

    /**
     * Nelze smazat obaly, protože existují navázané entity.
     */
    PACKET_DELETE_ERROR,

    /**
     * Archivní fond neexistuje.
     */
    FUND_NOT_FOUND,

    /**
     * Verze archivního fondu neexistuje.
     */
    FUND_VERSION_NOT_FOUND,

    /**
     * Nelze uzavřít verzi, protože běží hromadná akce.
     */
    VERSION_CANNOT_CLOSE_ACTION,

    /**
     * Nelze uzavřít verzi, protože běží validace.
     */
    VERSION_CANNOT_CLOSE_VALIDATION,

    /**
     * Jednotuka popisu neexistuje.
     */
    NODE_NOT_FOUND,

    /**
     * Existuje novější změna v AS/JP.
     */
    EXISTS_NEWER_CHANGE,

    /**
     * Neplatný počet externích systémů daného typu - musí být právě jeden.
     */
    ILLEGAL_COUNT_EXTERNAL_SYSTEM,

    /**
     * Entita již byla přidána (obvykle v nějaké množině).
     */
    ALREADY_ADDED,

    /**
     * Entita již byla odstraněna (obvykle z nějaké množiny).
     */
    ALREADY_REMOVED,

    /**
     * Entitu nelze odeslat.
     */
    CANT_SEND,

    /**
     * Neplatná verze.
     */
    INVALID_VERSION,

    /**
     * Neplatný stav požadavku.
     */
    REQUEST_INVALID_STATE,

    /**
     * Požadavek nenalezen ve frontě.
     */
    REQUEST_NOT_FOUND_IN_QUEUE,

    /**
     * Existuje blokující změna v JP - obecně např. hromadná změna, import AS, atd.
     */
    EXISTS_BLOCKING_CHANGE,

    /**
     * Typ formy jména neexistuje.
     */
    PARTY_NAME_FORM_TYPE_NOT_FOUND

}
