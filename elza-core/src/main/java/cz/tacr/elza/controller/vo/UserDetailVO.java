package cz.tacr.elza.controller.vo;

import java.util.Collection;

/**
 * Informace o přihlášeném uživateli, jako jsou uživatelské jméno, oprávnění a případně další nastavení.
 *
 * @author Pavel Stánek
 * @since 27.04.2016
 */
public class UserDetailVO extends UserVO {
    /** Oprávnění uživatele. */
    private Collection<UserPermissionVO> userPermissions;

    public Collection<UserPermissionVO> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(final Collection<UserPermissionVO> userPermissions) {
        this.userPermissions = userPermissions;
    }
}
