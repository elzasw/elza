package cz.tacr.elza.service.importnodes.vo;

/**
 * Změna hloubky při průchodem stromu.
 *
 * @since 19.07.2017
 */
public enum ChangeDeep {
    /**
     * Reset - volá se při inicializaci.
     */
    RESET,

    /**
     * Bez posunu v hloupce.
     */
    NONE,

    /**
     * O úroveň výše.
     */
    UP,

    /**
     * O úroveň níže.
     */
    DOWN
}
