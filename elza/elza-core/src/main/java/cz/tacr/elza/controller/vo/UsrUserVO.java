package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.domain.UsrAuthentication;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.security.UserDetail;

/**
 * Vo objekt uživatele, obsahuje informace o osobě.
 */
public class UsrUserVO {
    /** Uživatelské jméno. */
    private String username;
    /** Identifikátor uživatele. */
    private Integer id;
    /** Je aktivní. */
    private boolean active;
    /** Popis. */
    private String description;
    /** Přístupový bod. */
    private ApAccessPointVO accessPoint;
	/**
	 * Oprávnění.
	 */
	//TODO: Should be moved to other object
    private List<UsrPermissionVO> permissions;
    /** Seznam skupin. */
	//TODO: Should be moved to other object
    private List<UsrGroupVO> groups;

    private List<UsrAuthentication.AuthType> authTypes;

	/**
	 * Empty constructor
	 */
	public UsrUserVO() {

	}

	public UsrUserVO(UsrUser user, ApAccessPointVO accessPoint) {
		this.username = user.getUsername();
		this.id = user.getUserId();
		this.active = user.getActive();
		this.description = user.getDescription();
		this.accessPoint = accessPoint;
	}

	/**
	 * Prepare simplified user info
	 *
	 * @param userDetail
	 */
	public UsrUserVO(UserDetail userDetail) {
		this.username = userDetail.getUsername();
		this.id = userDetail.getId();
		this.active = userDetail.getActive();
		this.authTypes = userDetail.getAuthTypes();
	}

	public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<UsrPermissionVO> getPermissions() {
        return permissions;
    }

    public void setPermissions(final List<UsrPermissionVO> permissions) {
        this.permissions = permissions;
    }

    public List<UsrAuthentication.AuthType> getAuthTypes() {
        return authTypes;
    }

    public void setAuthTypes(final List<UsrAuthentication.AuthType> authTypes) {
        this.authTypes = authTypes;
    }

    public List<UsrGroupVO> getGroups() {
        return groups;
    }

    public void setGroups(final List<UsrGroupVO> groups) {
        this.groups = groups;
    }

    public ApAccessPointVO getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPointVO accessPoint) {
        this.accessPoint = accessPoint;
    }

    public UsrUser createEntity() {
        UsrUser entity = new UsrUser();
        entity.setUserId(id);
        entity.setActive(active);
        entity.setUsername(username);
        entity.setDescription(description);
        // party is not set
        return entity;
    }

    public static UsrUserVO newInstance(UsrUser user) {
        UsrUserVO vo = new UsrUserVO(user, null);
        return vo;
    }
}
