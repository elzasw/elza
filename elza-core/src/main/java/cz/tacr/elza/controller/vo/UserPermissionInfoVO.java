package cz.tacr.elza.controller.vo;

import java.util.*;

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

	/**
	 * Seznam identifikátorů protokolů, ke kterým se vztahuje oprávnění.
	 */
	private List<Integer> issueListIds = new ArrayList<>();

    /**
     * Default constructor for deserialization
     * 
     * Should not be used in other cases
     */
    public UserPermissionInfoVO() {

    }

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

	public List<Integer> getIssueListIds() {
		return issueListIds;
	}

	public void setIssueListIds(final List<Integer> issueListIds) {
		this.issueListIds = issueListIds;
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
	 * @param ids
	 */
	private void addFundIds(Collection<Integer> ids) {
		this.fundIds.addAll(ids);
	}

	/**
	 * Add all issue list ids
	 *
	 * @param ids
	 */
	private void addIssueListIds(Collection<Integer> ids) {
		this.issueListIds.addAll(ids);
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
		result.addIssueListIds(perm.getIssueListIds());
		return result;
	}
}
