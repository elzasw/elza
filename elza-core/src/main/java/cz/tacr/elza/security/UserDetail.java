package cz.tacr.elza.security;

import cz.tacr.elza.api.UsrPermission;
import cz.tacr.elza.domain.UsrUser;

import java.util.Collection;
import java.util.HashSet;

/**
 * Detail uživatele v session.
 * Použití pro zjištění základních informací o přihlášeném uživateli.
 *
 * @author Martin Šlapa
 * @since 13.04.2016
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
        this.userPermission = userPermission;
    }

    public UserDetail(final String systemUser) {
        this.username = systemUser;
        this.active = true;
        this.userPermission = new HashSet<>();
        this.userPermission.add(new UserPermission(UsrPermission.Permission.ADMIN));
    }

    public String getUsername() {
        return username;
    }

    public Integer getId() {
        return id;
    }

    public Boolean getActive() {
        return active;
    }

    public Collection<UserPermission> getUserPermission() {
        return userPermission;
    }
}
