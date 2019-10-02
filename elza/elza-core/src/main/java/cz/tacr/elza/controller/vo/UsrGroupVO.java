package cz.tacr.elza.controller.vo;

import java.util.List;

import cz.tacr.elza.domain.UsrGroup;

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

    public UsrGroupVO() {

    }

    public UsrGroupVO(UsrGroup group) {
        id = group.getGroupId();
        code = group.getCode();
        name = group.getName();
        description = group.getDescription();
        // users in group has to be mapped manually
    }

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

    public UsrGroup createEntity() {
        UsrGroup entity = new UsrGroup();
        entity.setGroupId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(description);
        // users in group has to be mapped manually
        return entity;
    }

    public static UsrGroupVO newInstance(UsrGroup group) {
        UsrGroupVO vo = new UsrGroupVO(group);
        return vo;
    }
}
