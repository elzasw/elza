package cz.tacr.elza.exception;

import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Výjimka pro neautorizovaný přístup.
 *
 * @author Martin Šlapa
 * @since 27.04.2016
 */
public class AccessDeniedException extends AbstractException {

    public AccessDeniedException(final UsrPermission.Permission[] permission) {
        super(BaseCode.INSUFFICIENT_PERMISSIONS);
        set("permission", permission);
    }

}
