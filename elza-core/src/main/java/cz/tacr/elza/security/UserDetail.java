package cz.tacr.elza.security;

import java.util.ArrayList;
import java.util.Collection;

import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.UsrPermission.Permission;
import cz.tacr.elza.domain.UsrUser;

/**
 * Detail uživatele v session.
 * Použití pro zjištění základních informací o přihlášeném uživateli.
 *
 */
public class UserDetail {

    /**
     * Uživatelské jméno.
     */
    private String username;

    /**
     * Identifikátor uživatele.
     */
    private Integer id;

    /**
     * Je uživatel aktivní?
     */
    private Boolean active;

    /**
     * Seznam oprávnění uživatele.
     */
    private Collection<UserPermission> userPermission;

    public UserDetail(final UsrUser user, final Collection<UserPermission> userPermission) {
        this.id = user.getUserId();
        this.username = user.getUsername();
        this.active = user.getActive();
        this.userPermission = new ArrayList<>(userPermission);
    }

	/**
	 * Constructor for ADMIN
	 * 
	 * @param systemUser
	 */
    public UserDetail(final String systemUser) {
        this.username = systemUser;
        this.active = true;
        this.userPermission = new ArrayList<>();
        this.userPermission.add(new UserPermission(UsrPermission.Permission.ADMIN));
    }

    public String getUsername() {
        return username;
    }

	/**
	 * Return DB id of user.
	 * 
	 * @return Return DB id of user. Return null for admin.
	 */
    public Integer getId() {
        return id;
    }

    public Boolean getActive() {
        return active;
    }

	/**
	 * Return collection of user permission
	 * 
	 * Return always valid object.
	 * 
	 * @return
	 */
    public Collection<UserPermission> getUserPermission() {
        return userPermission;
    }

	public void setUserPermission(Collection<UserPermission> perms) {
	    // update current permissions with new object
	    this.userPermission = new ArrayList<>(perms);
	}

	/**
	 * Check if user has given permission
	 * 
	 * @param usrPerm
	 * @return
	 */
	public boolean hasPermission(Permission usrPerm) {
		for (UserPermission userPermission : userPermission) {
			if (userPermission.getPermission().equals(usrPerm) ||
			        userPermission.getPermission().equals(UsrPermission.Permission.ADMIN)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if user has given permission
	 * 
	 * @param usrPerm
	 * @return
	 */
	public boolean hasPermission(Permission permission, Integer entityId) {
        for (UserPermission userPermission : this.userPermission) {
			if (userPermission.getPermission().equals(permission)) {

				if (userPermission.getPermission().equals(UsrPermission.Permission.ADMIN)) {
					return true;
				}

				switch (permission.getType()) {
				case FUND:
					if (userPermission.getFundIds().contains(entityId)) {
						return true;
					}
					break;
				case USER:
					if (userPermission.getControlUserIds().contains(entityId)) {
						return true;
					}
					break;
				case GROUP:
					if (userPermission.getControlGroupIds().contains(entityId)) {
						return true;
					}
					break;
				case SCOPE:
					if (userPermission.getScopeIds().contains(entityId)) {
						return true;
					}
					break;
				case ISSUE_LIST:
					if (userPermission.getIssueListIds().contains(entityId)) {
						return true;
					}
					break;
				default:
					throw new IllegalStateException(permission.getType().toString());
				}
				break;
			}
		}
		return false;
	}

    public boolean isControllsUser(Integer userId) {
        for (UserPermission userPermission : userPermission) {
            if (userPermission.isControllsUser(userId)) {                
                return true;
            }
        }
        return false;
    }

    public boolean isControllsGroup(Integer groupId) {
        for (UserPermission userPermission : userPermission) {
            if (userPermission.isControllsGroup(groupId)) {                
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(UsrPermission usrPermission) {
        UsrPermission.Permission permission = usrPermission.getPermission();
        
        for (UserPermission currPerm: userPermission) {
            // check if same type
            if (currPerm.hasPermission(usrPermission)) {
                return true;
            }
        }
        return false;
    }
}
