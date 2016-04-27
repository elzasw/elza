package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.UsrPermission;

import java.util.HashSet;
import java.util.Set;

/**
 * Oprávnění uživatele.
 *
 * @author Pavel Stánek
 * @since 27.04.2016
 */
public class UserPermissionVO {
    /** Typ oprávnění. */
    private UsrPermission.Permission permission;

    /** Seznam identifikátorů AS, ke kterým se vztahuje oprávnění. */
    private Set<Integer> fundIds = new HashSet<>();

    /** Seznam identifikátorů scopů, ke kterým se vztahuje oprávnění. */
    private Set<Integer> scopeIds = new HashSet<>();

    public UsrPermission.Permission getPermission() {
        return permission;
    }

    public void setPermission(UsrPermission.Permission permission) {
        this.permission = permission;
    }

    public Set<Integer> getFundIds() {
        return fundIds;
    }

    public void setFundIds(Set<Integer> fundIds) {
        this.fundIds = fundIds;
    }

    public Set<Integer> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(Set<Integer> scopeIds) {
        this.scopeIds = scopeIds;
    }
}
