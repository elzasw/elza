package cz.tacr.elza.api;

/**
 * Výčet externích systémů pro rejstříky/osoby.
 *
 * @since 23. 11. 2016
 */
public enum ApExternalSystemType {

    CAM,
    /**
     * Same as CAM except UUID is preferred as ID
     */
    CAM_UUID,
    /**
     * Automatické přebírání nových záznamů z CAMu
     */
    CAM_COMPLETE

}
