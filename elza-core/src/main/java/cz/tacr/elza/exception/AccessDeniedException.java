package cz.tacr.elza.exception;

import java.util.Collection;

import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.security.UserPermission;

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

	public AccessDeniedException(String message, Collection<UserPermission> userPermission) {
        super(message, BaseCode.INSUFFICIENT_PERMISSIONS);
        set("permission", userPermission);
	}
}
