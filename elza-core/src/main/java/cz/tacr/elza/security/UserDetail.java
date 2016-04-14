package cz.tacr.elza.security;

import cz.tacr.elza.domain.UsrUser;

/**
 * Detail uživatele v session.
 * Použití pro zjištění základních informací o přihlášeném uživateli.
 *
 * @author Martin Šlapa
 * @since 13.04.2016
 */
public class UserDetail {

    private String username;

    private Integer id;

    private Boolean active;

    public UserDetail(final UsrUser user) {
        this.id = user.getUserId();
        this.username = user.getUsername();
        this.active = user.getActive();
    }

    public UserDetail(final String systemUser) {
        this.username = systemUser;
        this.active = true;
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

}
