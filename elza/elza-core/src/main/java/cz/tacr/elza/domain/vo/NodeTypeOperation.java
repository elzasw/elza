package cz.tacr.elza.domain.vo;

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
     * Odpojení uzlu se od stejného rodiče.
     */
    DISCONNECT_NODE_LOCAL,

    /**
     * Připojení uzlu ke stejnému rodiči.
     */
    CONNECT_NODE_LOCAL,

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
