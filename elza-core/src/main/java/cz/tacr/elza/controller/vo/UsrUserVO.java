package cz.tacr.elza.controller.vo;

import java.util.List;
import java.util.Objects;

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
    /** Osoba. */
    private ParPartyVO party;
	/**
	 * Oprávnění.
	 */
	//TODO: Should be moved to other object
    private List<UsrPermissionVO> permissions;
    /** Seznam skupin. */
	//TODO: Should be moved to other object
    private List<UsrGroupVO> groups;

	/**
	 * Empty constructor
	 */
	public UsrUserVO() {

	}

	protected UsrUserVO(UsrUser user, ParPartyVO party) {
		this.username = user.getUsername();
		this.id = user.getUserId();
		this.active = user.getActive();
		this.description = user.getDescription();
		this.party = party;
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

    public ParPartyVO getParty() {
        return party;
    }

    public void setParty(final ParPartyVO party) {
        this.party = party;
    }

    public List<UsrPermissionVO> getPermissions() {
        return permissions;
    }

    public void setPermissions(final List<UsrPermissionVO> permissions) {
        this.permissions = permissions;
    }

    public List<UsrGroupVO> getGroups() {
        return groups;
    }

    public void setGroups(final List<UsrGroupVO> groups) {
        this.groups = groups;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsrUserVO usrUserVO = (UsrUserVO) o;
        return active == usrUserVO.active &&
                Objects.equals(username, usrUserVO.username) &&
                Objects.equals(id, usrUserVO.id) &&
                Objects.equals(description, usrUserVO.description) &&
                Objects.equals(party, usrUserVO.party) &&
                Objects.equals(permissions, usrUserVO.permissions) &&
                Objects.equals(groups, usrUserVO.groups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, id, active, description, party, permissions, groups);
    }
}
