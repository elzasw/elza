package cz.tacr.elza.controller.vo;

import java.util.Collection;

/**
 * Informace o přihlášeném uživateli, jako jsou uživatelské jméno, oprávnění a případně další nastavení.
 *
 * @author Pavel Stánek
 * @since 27.04.2016
 */
public class UserInfoVO extends UsrUserVO {
    /** Oprávnění uživatele. */
    private Collection<UserPermissionInfoVO> userPermissions;

    private Collection<UISettingsVO> settings;

    public Collection<UserPermissionInfoVO> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(final Collection<UserPermissionInfoVO> userPermissions) {
        this.userPermissions = userPermissions;
    }

    public Collection<UISettingsVO> getSettings() {
        return settings;
    }

    public void setSettings(final Collection<UISettingsVO> settings) {
        this.settings = settings;
    }
}
