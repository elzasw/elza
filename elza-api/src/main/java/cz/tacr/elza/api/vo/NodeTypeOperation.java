package cz.tacr.elza.api.vo;

/**
 * Typ operace s uzlem.
 *
 * @author Martin Šlapa
 * @since 27.11.2015
 */
public enum NodeTypeOperation {

    /**
     * Vytvoření uzlu.
     */
    CREATE_NODE,

    /**
     * Smazání uzlu.
     */
    DELETE_NODE,

    /**
     * Změna pozice uzlu.
     */
    CHANGE_NODE_POSITION,

    /**
     * Odpojení uzlu.
     */
    DISCONNECT_NODE,

    /**
     * Připojení uzlu.
     */
    CONNECT_NODE,

    /**
     * Změna atributů uzlu.
     */
    SAVE_DESC_ITEM;

}
