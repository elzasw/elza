package cz.tacr.elza.api;

/**
 * Action a user asked to be carried out over a set of AIPs.
 *
 * Some of the actions are carried out by ELZA alone, others need the digital archive and are
 * therefore only requested here and finished later, when the archive answers. Which of the two
 * it is decides where the action is executed, not how it is recorded - both kinds are recorded
 * the same way, so the user is told about them the same way.
 */
public enum DaAipActionType {

    /** Request the metadata of the AIP from the digital archive. */
    LOAD_METADATA(true),

    /** Drop the digital entities built from the metadata; keeps the AIP itself. */
    DELETE_METADATA(false),

    /** Request the complete AIP, not just its metadata, from the digital archive. */
    LOAD_COMPLETE_AIP(true),

    /** Drop the complete AIP and keep only its metadata. */
    DELETE_COMPLETE_AIP(true),

    /** Download whatever is loaded for the AIP again. */
    DOWNLOAD_UPDATE(true),

    /** Rebuild the digital entities from the package already stored in ELZA. */
    DB_UPDATE(false),

    /** As {@link #DB_UPDATE}, but removes the entities attached to a unit of description too. */
    FORCE_UPDATE(false),

    /** Resolve the institution and the fund of the AIP again. */
    REMAP_REFERENCES(false),

    /** Send the AIP to the digital archive. */
    EXPORT(true);

    private final boolean usesDigitalArchive;

    DaAipActionType(boolean usesDigitalArchive) {
        this.usesDigitalArchive = usesDigitalArchive;
    }

    /**
     * True when the action is finished only once the digital archive has answered, so it is
     * carried out through the synchronization queue rather than at the moment it is requested.
     */
    public boolean usesDigitalArchive() {
        return usesDigitalArchive;
    }
}
