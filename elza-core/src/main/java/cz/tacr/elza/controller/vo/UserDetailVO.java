package cz.tacr.elza.controller.vo;

import java.util.Collection;

/**
 * Informace o přihlášeném uživateli, jako jsou uživatelské jméno, oprávnění a případně další nastavení.
 *
 * @author Pavel Stánek
 * @since 27.04.2016
 */
public class UserDetailVO {
    /** Oprávnění uživatele. */
    private Collection<UserPermissionVO> userPermissions;
    /** Uživatelské jméno. */
    private String username;
    /** Identifikátor uživatele. */
    private Integer id;

    public Collection<UserPermissionVO> getUserPermissions() {
        return userPermissions;
    }

    public void setUserPermissions(final Collection<UserPermissionVO> userPermissions) {
        this.userPermissions = userPermissions;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
