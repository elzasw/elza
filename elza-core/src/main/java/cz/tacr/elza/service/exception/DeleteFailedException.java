package cz.tacr.elza.service.exception;

import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Výjimka pro mazání záznamů, které kvůli závislosti nejdou smazat.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.04.2016
 */
public class DeleteFailedException extends AbstractException {

    public DeleteFailedException(final String message, final Throwable cause) {
        super(message, cause, BaseCode.SYSTEM_ERROR);
    }

}
