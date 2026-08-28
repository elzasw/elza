package cz.tacr.elza.api;

/**
 * Action taken automatically when a digital archive (DA) repository reports
 * a newly received AIP.
 */
public enum DaOnReceivedAction {

    /**
     * No automatic processing.
     */
    NONE,

    /**
     * Download the AIP metadata, look up a matching node and attach the AIP to it.
     */
    DOWNLOAD_METADATA,

}
