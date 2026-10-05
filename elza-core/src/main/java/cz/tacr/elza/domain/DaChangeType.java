package cz.tacr.elza.domain;

public enum DaChangeType {

    AIP_CREATE,
    AIP_UPDATE,
    /**
     * The digital archive invalidated the AIP: its state, digital entities and links to the
     * archival description are closed by this change.
     */
    AIP_INVALIDATE
}
