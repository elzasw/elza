package cz.tacr.elza.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;

/**
 * Class for request authorization
 * 
 */
public class AuthorizationRequest {

	/**
	 * Check if given permission exists.
	 *
	 * Check does not validate specific entity ids.
	 */
	static class AuthCheck {
		UsrPermission.Permission perm;

		AuthCheck(UsrPermission.Permission perm) {
			this.perm = perm;
		}

		UsrPermission.Permission getPermission() {
			return perm;
		}

		/**
		 * Check if has such permission
		 * 
		 * @param userDetail
		 * @return
		 */
		public boolean matches(UserDetail userDetail) {
			return userDetail.hasPermission(perm);
		}
	}

	/**
	 * Check if user has specific access rights for the fund
	 *
	 */
	static class AuthCheckFundId extends AuthCheck {

        final Integer fundId;

        AuthCheckFundId(Permission perm, final Integer fundId) {
			super(perm);
			this.fundId = fundId;
		}

		public boolean matches(Collection<UserPermission> perms) {
			for (UserPermission up : perms) {
				if (up.hasFundPermission(perm, fundId)) {
					return true;
				}
			}
			return false;
		}
	}

    static class AuthCheckScopeId extends AuthCheck {

        final Integer scopeId;

        AuthCheckScopeId(Permission perm, final Integer scopeId) {
            super(perm);
            this.scopeId = scopeId;
        }

        public boolean matches(Collection<UserPermission> perms) {
            for (UserPermission up : perms) {
                if (up.hasScopePermission(perm, scopeId)) {
                    return true;
                }
            }
            return false;
        }
    }

	List<AuthCheck> checkList = new ArrayList<>();

	AuthorizationRequest() {

	}

	public AuthorizationRequest or(Permission perm) {
		// only some permission type checks are supported
		Validate.isTrue(perm == Permission.ADMIN || perm == Permission.FUND_ARR_ALL
				|| perm == Permission.FUND_ADMIN
				|| perm == Permission.FUND_BA_ALL || perm == Permission.FUND_CL_VER_WR_ALL
				|| perm == Permission.FUND_EXPORT_ALL || perm == Permission.FUND_OUTPUT_WR_ALL
				|| perm == Permission.FUND_RD_ALL || perm == Permission.FUND_ISSUE_ADMIN_ALL
				|| perm == Permission.AP_SCOPE_RD_ALL || perm == Permission.AP_SCOPE_WR_ALL
				|| perm == Permission.USR_PERM);

		checkList.add(new AuthCheck(perm));
		return this;
	}

	public AuthorizationRequest or(Permission perm, ArrFundVersion fundVersion) {
		return or(perm, fundVersion.getFundId());
	}
	
	/**
	 * Generic check
	 * @param perm
	 * @param entityId
	 * @return
	 */
	public AuthorizationRequest or(Permission perm, Integer entityId) {
		// only some permission type checks are supported
		switch (perm) {
		case FUND_ARR:
		case FUND_BA:
		case FUND_CL_VER_WR:
		case FUND_EXPORT:
		case FUND_VER_WR:
		case FUND_RD:
		case FUND_ISSUE_ADMIN:
			checkList.add(new AuthCheckFundId(perm, entityId));
			break;
		case AP_SCOPE_RD:
		case AP_SCOPE_WR:
		case AP_CONFIRM:
		case AP_EDIT_CONFIRMED:
			checkList.add(new AuthCheckScopeId(perm, entityId));
			break;
		default:
			throw new IllegalStateException("Unsupported permission check, permission = " + perm);
		}
		return this;
	}

	public static AuthorizationRequest hasPermission(UsrPermission.Permission perm) {
		return new AuthorizationRequest().or(perm);

	}

	/**
	 * Return array of permissions
	 * 
	 * Returned permissions are without values
	 * 
	 * @return
	 */
	public UsrPermission.Permission[] getPermissions() {
		UsrPermission.Permission result[] = new UsrPermission.Permission[checkList.size()];
		int i = 0;
		for (AuthCheck authCheck : this.checkList) {
			result[i] = authCheck.getPermission();
			i++;
		}
		return result;
	}

	public boolean matches(UserDetail userDetail) {
		for (AuthCheck authCheck : checkList) {
			if (authCheck.matches(userDetail)) {
				return true;
			}
		}
		return false;
	}

}
