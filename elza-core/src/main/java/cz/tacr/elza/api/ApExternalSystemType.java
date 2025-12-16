package cz.tacr.elza.api;

/**
 * Výčet externích systémů pro rejstříky/osoby.
 *
 * @since 23. 11. 2016
 */
public enum ApExternalSystemType {

    CAM,
    CAM_V2,
    /**
     * Same as CAM except UUID is preferred as ID
     */
    CAM_UUID,
    CAM_UUID_V2,
    /**
     * Automatické přebírání nových záznamů z CAMu
     */
    CAM_COMPLETE,
    CAM_COMPLETE_V2,
}
