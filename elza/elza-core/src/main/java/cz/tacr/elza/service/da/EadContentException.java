package cz.tacr.elza.service.da;

/**
 * A value of the inherent archival description (EAD) of a package that is not written the way
 * the profile of EAD for the National Archives requires, so it cannot be taken over without
 * the risk of reading it differently than its author meant it.
 *
 * The message is the reason in the words the user reads; it names the attribute or element
 * and the value, but not the file or the unit of description - the caller knows those and
 * adds them.
 */
class EadContentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    EadContentException(String message) {
        super(message);
    }

    EadContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
