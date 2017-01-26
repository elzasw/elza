package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Neplatný lucene dotaz.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.04.2016
 */
public class InvalidQueryException extends AbstractException {

    public InvalidQueryException(final Throwable cause) {
        super(cause.getMessage(), cause, BaseCode.SYSTEM_ERROR);
    }
}
