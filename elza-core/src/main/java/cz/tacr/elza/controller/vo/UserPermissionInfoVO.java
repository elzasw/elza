package cz.tacr.elza.controller.vo;

import java.util.HashSet;
import java.util.Set;

import cz.tacr.elza.domain.UsrPermission;

/**
 * Informace o oprávnění uživatele.
 *
 * @author Pavel Stánek
 * @since 27.04.2016
 */
public class UserPermissionInfoVO {
    /** Typ oprávnění. */
    private UsrPermission.Permission permission;

    /** Seznam identifikátorů AS, ke kterým se vztahuje oprávnění. */
    private Set<Integer> fundIds = new HashSet<>();

    /** Seznam identifikátorů scopů, ke kterým se vztahuje oprávnění. */
    private Set<Integer> scopeIds = new HashSet<>();

    public UsrPermission.Permission getPermission() {
        return permission;
    }

    public void setPermission(final UsrPermission.Permission permission) {
        this.permission = permission;
    }

    public Set<Integer> getFundIds() {
        return fundIds;
    }

    public void setFundIds(final Set<Integer> fundIds) {
        this.fundIds = fundIds;
    }

    public Set<Integer> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(final Set<Integer> scopeIds) {
        this.scopeIds = scopeIds;
    }
}
