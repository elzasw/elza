package cz.tacr.elza.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;

public class AuthorizationRequest {

	static class AuthCheck {
		UsrPermission.Permission perm;

		AuthCheck(UsrPermission.Permission perm) {
			this.perm = perm;
		}

		UsrPermission.Permission getPermission() {
			return perm;
		}
	}

	static class AuthCheckFundVersion extends AuthCheck {

		ArrFundVersion fundVersion;

		AuthCheckFundVersion(Permission perm, ArrFundVersion fundVersion) {
			super(perm);
			this.fundVersion = fundVersion;
		}

	}

	List<AuthCheck> checkList = new ArrayList<>();

	AuthorizationRequest() {

	}

	public AuthorizationRequest or(Permission perm) {
		Validate.isTrue(perm == Permission.ADMIN || perm == Permission.FUND_ARR_ALL);

		checkList.add(new AuthCheck(perm));
		return this;
	}

	public AuthorizationRequest or(Permission perm, ArrFundVersion fundVersion) {
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

	public boolean matches(Collection<UserPermission> perms) {
		// TODO Auto-generated method stub
		return false;
	}

}
