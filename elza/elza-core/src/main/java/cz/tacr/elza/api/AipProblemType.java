package cz.tacr.elza.api;

/**
 * Problem that prevents an AIP from being processed or from being mapped onto the
 * archival description.
 *
 * An AIP may hit more than one problem at a time; the state carries the most severe one
 * and the description names all of them.
 */
public enum AipProblemType {

    /**
     * The metadata package could not be processed - it is missing an expected file or its
     * content does not match the expected structure.
     */
    METADATA_ERROR,

    /**
     * The fund the AIP belongs to was not found. The AIP cannot be mapped onto the archival
     * description until the fund exists.
     */
    UNKNOWN_FUND,

    /**
     * The institution of the AIP was not found. The AIP is still usable when its fund was
     * resolved - the institution is descriptive only.
     */
    UNKNOWN_INSTITUTION,

}
