package cz.tacr.elza.api;

/**
 * How far an action over a set of AIPs has got. Derived from the states of its items - the
 * action is finished when none of them is outstanding, and failed when any of them failed.
 */
public enum DaAipActionState {

    /** No item has been carried out yet. */
    WAITING,

    /** Some of the items are done, the rest are still outstanding. */
    RUNNING,

    /** Every item is done and none of them failed. */
    FINISHED,

    /** Every item is done and at least one of them failed. */
    ERROR
}
