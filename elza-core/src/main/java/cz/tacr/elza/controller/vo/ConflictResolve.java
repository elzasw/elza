package cz.tacr.elza.controller.vo;

/**
 * @since 17.07.2017
 */
public enum ConflictResolve {

    /**
     * Použít entity z cílového souboru.
     */
    USE_TARGET,

    /**
     * Zkopírovat entity ze zdrojového souboru a přejmenovat v cílovém.
     */
    COPY_AND_RENAME

}
