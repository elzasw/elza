package cz.tacr.elza.security;

import java.util.*;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrPermission.PermissionType;

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

    /**
     * Seznam identifikátorů JP, ke kterým se vztahuje oprávnění.
     */
    private Map<Integer, Set<Integer>> fundNodeIds = new HashMap<>();

    /**
     * Seznam identifikátorů protokolů, ke kterým se vztahuje oprávnění.
     */
    private Set<Integer> issueListIds = new HashSet<>();

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

    public Set<Integer> getIssueListIds() {
        return issueListIds;
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

    public void addNodeId(final Integer fundId, final Integer nodeId) {
        Set<Integer> nodeIds = fundNodeIds.computeIfAbsent(fundId, k -> new HashSet<>());
        nodeIds.add(nodeId);
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

    public void addIssueListId(final Integer issueListId) {
        issueListIds.add(issueListId);
    }

    public Map<Integer, Set<Integer>> getFundNodeIds() {
        return fundNodeIds;
    }

    public Set<Integer> getNodeIdsByFund(final Integer fundId) {
        Set<Integer> nodeIds = fundNodeIds.get(fundId);
        return nodeIds == null ? Collections.emptySet() : nodeIds;
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
	 * Check specific fund related permission
	 * @param perm
	 * @param fundId
	 * @return
	 */
	public boolean hasFundPermission(Permission perm, Integer fundId) {
		Validate.isTrue(perm.getType()==PermissionType.FUND);
		
		if (this.permission != perm) {
			return false;
		}
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

    /**
     * Check if this permission grants given permission
     * 
     * @param usrPermission
     * @return
     */
    public boolean hasPermission(UsrPermission usrPermission) {
        if (!permission.isEqualOrHigher(usrPermission.getPermission())) {
            return false;
        }

        PermissionType permType = permission.getType();
        switch (permType) {
        case ALL:
            return true;
        case FUND:
            if (fundIds.contains(usrPermission.getFundId())) {
                return true;
            }
            break;
        case USER:
            if (controlUserIds.contains(usrPermission.getUserControlId())) {
                return true;
            }
            break;
        case GROUP:
            if (controlGroupIds.contains(usrPermission.getGroupControlId())) {
                return true;
            }
            break;
        case SCOPE:
            if (scopeIds.contains(usrPermission.getScopeId())) {
                return true;
            }
            break;
        case ISSUE_LIST:
            if (issueListIds.contains(usrPermission.getIssueListId())) {
                return true;
            }
        case NODE:
            Set<Integer> nodeIds = fundNodeIds.get(usrPermission.getFundId());
            if (nodeIds != null && nodeIds.contains(usrPermission.getNodeId())) {
                return true;
            }
            break;
        default:
            throw new UnsupportedOperationException("Neimplementovaný typ oprvánění: " + permission.getType());
        }
        return false;
    }

    /**
     * Check scope specific permission
     * 
     * @param perm
     * @param scopeId
     * @return
     */
    public boolean hasScopePermission(Permission perm, Integer scopeId) {
        Validate.isTrue(perm.getType() == PermissionType.SCOPE);

        if (this.permission != perm) {
            return false;
        }
        if (!scopeIds.contains(scopeId)) {
            return false;
        }
        return true;
    }

    /**
     * Check issue list specific permission
     */
    public boolean hasIssueListPermission(Permission perm, Integer issueListId) {
        Validate.isTrue(perm.getType() == PermissionType.ISSUE_LIST);

        if (this.permission != perm) {
            return false;
        }
        if (!issueListIds.contains(issueListId)) {
            return false;
        }
        return true;
    }
}
