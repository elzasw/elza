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
	static class AuthCheckFundVersion extends AuthCheck {

		ArrFundVersion fundVersion;

		AuthCheckFundVersion(Permission perm, ArrFundVersion fundVersion) {
			super(perm);
			this.fundVersion = fundVersion;
		}

		public boolean matches(Collection<UserPermission> perms) {
			for (UserPermission up : perms) {
				if (up.hasPermission(perm, fundVersion)) {
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
		Validate.isTrue(perm == Permission.ADMIN || perm == Permission.FUND_ARR_ALL);

		checkList.add(new AuthCheck(perm));
		return this;
	}

	public AuthorizationRequest or(Permission perm, ArrFundVersion fundVersion) {
		// only some permission type checks are supported
		Validate.isTrue(perm == Permission.FUND_ARR);

		checkList.add(new AuthCheckFundVersion(perm, fundVersion));
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
