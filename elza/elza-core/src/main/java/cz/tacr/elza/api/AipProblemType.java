package cz.tacr.elza.api;

/**
 * Problem that prevents an AIP from being processed or from being mapped onto the
 * archival description.
 *
 * The type is what tells the problems apart; everything else recorded about a problem - its
 * description, its technical detail and the file it is about - is the same for every type,
 * so a newly detected kind of problem is a new constant here and nothing else.
 */
public enum AipProblemType {

    /**
     * The package could not be downloaded from the digital archive - nothing of it was
     * received. The download is retried with a growing delay, and the next successful
     * download replaces this problem with whatever the received package says.
     */
    DOWNLOAD_ERROR(true),

    /**
     * The metadata package could not be processed - it is missing an expected file or its
     * content does not match the expected structure.
     */
    METADATA_ERROR(true),

    /**
     * The fund the AIP belongs to was not found. The AIP cannot be mapped onto the archival
     * description until the fund exists.
     */
    UNKNOWN_FUND(false),

    /**
     * The institution of the AIP was not found. The AIP is still usable when its fund was
     * resolved - the institution is descriptive only.
     */
    UNKNOWN_INSTITUTION(false);

    private final boolean processingFailure;

    AipProblemType(boolean processingFailure) {
        this.processingFailure = processingFailure;
    }

    /**
     * True for the problems the processing of the package ends with. They are recorded as they
     * are detected, because nothing can derive them again later, and they outrank the problems
     * that are derived - the package has to be processed before anything read out of it can be
     * mapped onto the archival description.
     *
     * False for the problems derived from the state of the AIP whenever it changes.
     */
    public boolean isProcessingFailure() {
        return processingFailure;
    }
}
