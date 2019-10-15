package cz.tacr.elza.exception;

import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Výjimka pro neautorizovaný přístup.
 *
 * @since 27.04.2016
 */
public class AccessDeniedException extends AbstractException {

    public AccessDeniedException(final String message, final UsrPermission.Permission[] permission) {
        super(message, BaseCode.INSUFFICIENT_PERMISSIONS);
        set("permission", permission);
    }

}
