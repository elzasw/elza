package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.security.UserPermission;

/**
 * Informace o oprávnění uživatele.
 *
 */
public class UserPermissionInfoVO {
    /** Typ oprávnění. */
    private UsrPermission.Permission permission;

    /** Seznam identifikátorů AS, ke kterým se vztahuje oprávnění. */
	private List<Integer> fundIds = new ArrayList<>();

    /** Seznam identifikátorů scopů, ke kterým se vztahuje oprávnění. */
	private List<Integer> scopeIds = new ArrayList<>();

	public UserPermissionInfoVO(Permission permission) {
		this.permission = permission;
	}

	public UsrPermission.Permission getPermission() {
        return permission;
    }

    public void setPermission(final UsrPermission.Permission permission) {
        this.permission = permission;
    }

	public List<Integer> getFundIds() {
        return fundIds;
    }

	public void setFundIds(final List<Integer> fundIds) {
        this.fundIds = fundIds;
    }

	public List<Integer> getScopeIds() {
        return scopeIds;
    }

	public void setScopeIds(final List<Integer> scopeIds) {
        this.scopeIds = scopeIds;
    }

	/**
	 * Add all scope ids
	 * 
	 * @param ids
	 */
	private void addScopeIds(Collection<Integer> ids) {
		this.scopeIds.addAll(ids);
	}

	/**
	 * Add all fund ids
	 * 
	 * @param fundIds
	 */
	private void addFundIds(Collection<Integer> ids) {
		this.fundIds.addAll(ids);
	}

	/**
	 * Create new instance of UserPermissionInfoVO
	 * 
	 * @param perm
	 * @return
	 */
	public static UserPermissionInfoVO newInstance(UserPermission perm) {
		UserPermissionInfoVO result = new UserPermissionInfoVO(perm.getPermission());
		result.addFundIds(perm.getFundIds());
		result.addScopeIds(perm.getScopeIds());

		return result;
	}
}
