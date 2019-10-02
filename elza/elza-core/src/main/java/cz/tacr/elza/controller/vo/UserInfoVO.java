package cz.tacr.elza.controller.vo;

import java.util.ArrayList;
import java.util.Collection;

import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;

/**
 * Informace o přihlášeném uživateli, jako jsou uživatelské jméno, oprávnění a případně další nastavení.
 *
 */
public class UserInfoVO extends UsrUserVO {
	/**
	 * Preferred user name
	 */
	String preferredName;

    /** Oprávnění uživatele. */
    private Collection<UserPermissionInfoVO> userPermissions;

    private Collection<UISettingsVO> settings;

    /**
     * Default empty constructor for deserialization
     */
    public UserInfoVO() {

    }

    protected UserInfoVO(final String preferredName, final UserDetail userDetail) {
		super(userDetail);
		this.preferredName = preferredName;
	}

	public String getPreferredName() {
		return preferredName;
	}

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public Collection<UserPermissionInfoVO> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(final Collection<UserPermissionInfoVO> userPermissions) {
        this.userPermissions = userPermissions;
    }

    public Collection<UISettingsVO> getSettings() {
        return settings;
    }

    public void setSettings(final Collection<UISettingsVO> settings) {
        this.settings = settings;
    }

	/**
	 * Prepare new instance of UserInfoVO
	 * 
	 * @param userDetail
	 * @param prefferedName
	 * @return
	 */
	public static UserInfoVO newInstance(UserDetail userDetail, String preferredName) {
		UserInfoVO result = new UserInfoVO(preferredName, userDetail);

		// convert permissions
		Collection<UserPermission> perms = userDetail.getUserPermission();
		if (perms != null) {
			result.userPermissions = new ArrayList<>(perms.size());
			perms.forEach(perm -> {
				UserPermissionInfoVO permVO = UserPermissionInfoVO.newInstance(perm);
				result.userPermissions.add(permVO);
			});
		}
		return result;
	}
}
