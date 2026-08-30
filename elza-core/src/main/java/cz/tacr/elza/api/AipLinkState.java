package cz.tacr.elza.api;

/**
 * How much of an AIP hangs on the archival description.
 *
 * The content of an AIP is the files of its package, and a link may be made to the package as a
 * whole, to a level of its logical structure or to one file. A link to something that contains
 * files therefore attaches those files too, so how much of the package is attached cannot be read
 * off the links alone - it is a question about what the links reach. That is why this is worked out
 * when the links change and kept on the AIP, rather than asked whenever the list is read.
 */
public enum AipLinkState {

    /** Nothing of the AIP hangs on the archival description. */
    NOT_LINKED,

    /** Some of the files of the AIP are attached, the rest are not. */
    PARTIALLY_LINKED,

    /**
     * The whole package is attached, either directly or because everything it contains is.
     */
    FULLY_LINKED
}
