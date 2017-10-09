package cz.tacr.elza.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import cz.tacr.elza.domain.UsrPermission;

/**
 * Oprávnění uživatele.
 *
 * @author Martin Šlapa
 * @since 26.04.2016
 */
public class UserPermission {

    /**
     * Typ oprávnění.
     */
    private UsrPermission.Permission permission;

    /**
     * Seznam identifikátorů AS, ke kterým se vztahuje oprávnění.
     */
    private Set<Integer> fundIds = new HashSet<>();

    /**
     * Seznam identifikátorů spravovaných uživatelů, ke kterým se vztahuje oprávnění.
     */
    private Set<Integer> controlUserIds = new HashSet<>();

    /**
     * Seznam identifikátorů spravovaných skupin, ke kterým se vztahuje oprávnění.
     */
    private Set<Integer> controlGroupIds = new HashSet<>();

    /**
     * Seznam identifikátorů scopů, ke kterým se vztahuje oprávnění.
     */
    private Set<Integer> scopeIds = new HashSet<>();

    public UserPermission(final UsrPermission.Permission permission) {
        this.permission = permission;
    }

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPermission that = (UserPermission) o;
        return permission == that.permission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission);
    }

    public void addFundId(final Integer fundId) {
        fundIds.add(fundId);
    }

    public void addControlGroupId(final Integer groupId) {
        controlGroupIds.add(groupId);
    }

    public void addControlUserId(final Integer userId) {
        controlUserIds.add(userId);
    }

    public void addScopeId(final Integer scopeId) {
        scopeIds.add(scopeId);
    }

    public Set<Integer> getControlUserIds() {
        return controlUserIds;
    }

    public Set<Integer> getControlGroupIds() {
        return controlGroupIds;
    }
}
