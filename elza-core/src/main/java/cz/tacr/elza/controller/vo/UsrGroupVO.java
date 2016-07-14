package cz.tacr.elza.controller.vo;

import java.util.List;
import java.util.Objects;

/**
 * VO skupiny oprávnění.
 *
 * @author Pavel Stánek
 * @since 16.06.2016
 */
public class UsrGroupVO {
    /** Identifikátor. */
    private Integer id;
    /** Kód. */
    private String code;
    /** Název. */
    private String name;
    /** Popis. */
    private String description;
    /** Oprávnění. */
    private List<UsrPermissionVO> permissions;
    /** Uživatelé přiřazení do skupiny. */
    private List<UsrUserVO> users;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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

    public List<UsrUserVO> getUsers() {
        return users;
    }

    public void setUsers(final List<UsrUserVO> users) {
        this.users = users;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsrGroupVO that = (UsrGroupVO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(permissions, that.permissions) &&
                Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, description, permissions, users);
    }
}
