package cz.tacr.elza.api;

/**
 * How an action ended for one AIP.
 *
 * SKIPPED is the reason this is recorded at all: an action that cannot apply to an AIP used to
 * pass over it without a word, so the user saw a request that reported success and changed
 * nothing. A skipped item names what was missing instead.
 */
public enum DaAipActionItemState {

    /** Not carried out yet. */
    WAITING,

    /** Being carried out, or waiting for the answer of the digital archive. */
    RUNNING,

    /** Carried out. */
    FINISHED,

    /** Attempted and failed; the message says why. */
    ERROR,

    /** Not attempted, because the action cannot apply to this AIP; the message says why. */
    SKIPPED;

    /** True once the item is done, whatever the outcome. */
    public boolean isTerminal() {
        return this == FINISHED || this == ERROR || this == SKIPPED;
    }
}
