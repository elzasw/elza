package cz.tacr.elza.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;

/**
 * Oprávnění uživatele.
 *
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

    public Set<Integer> getFundIds() {
        return fundIds;
    }

    public Set<Integer> getScopeIds() {
        return scopeIds;
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

	public boolean hasPermission(Permission perm, ArrFundVersion fundVersion) {
		if (this.permission != perm) {
			return false;
		}
		Integer fundId = fundVersion.getFundId();
		if (!fundIds.contains(fundId)) {
			return false;
		}
		return true;
	}

	/**
	 * Check if permission has given type
	 * 
	 * Note this check only compare permission type and does not check further
	 * conditions.
	 * 
	 * @param permissionType
	 * @return
	 */
	public boolean isPermissionType(Permission permissionType) {
		if (this.permission == permissionType) {
			return true;
		}
		return false;
	}

	/**
	 * Check if user in controlled user set
	 * 
	 * @param userId
	 * @return
	 */
	public boolean isControllsUser(Integer userId) {
		if (controlUserIds.contains(userId)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if group in controlled group set
	 * 
	 * @param groupId
	 * @return
	 */
	public boolean isControllsGroup(Integer groupId) {
		if (controlGroupIds.contains(groupId)) {
			return true;
		}
		return false;
	}
}
