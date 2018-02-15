package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.UsrPermission;

/**
 * VO objektu jednoho oprávnění uživatele nebo skupiny.
 *
 * @author Pavel Stánek
 * @since 16.06.2016
 */
public class UsrPermissionVO {
    /** Identifikátor. */
    private Integer id;

    /**
     * Je právo zděděné ze skupiny?
     */
    private Boolean inherited;

    /** Pokud je právo zděděné, je zde id skupiny. */
    private Integer groupId;

    /** Typ oprávnění. */
    private UsrPermission.Permission permission;

    /** AS, ke kterému se vztahuje oprávnění. */
    private ArrFundBaseVO fund;

    /** Skupina, ke které se vztahuje oprávnění. */
    private UsrGroupVO groupControl;

    /** Uživatel, ke kterému se vztahuje oprávnění. */
    private UsrUserVO userControl;

    /** Scope, ke kterému se vztahuje oprávnění. */
    private ApScopeVO scope;

    /** Typ oprávnění. */
    public UsrPermission.Permission getPermission() {
        return permission;
    }

    public void setPermission(final UsrPermission.Permission permission) {
        this.permission = permission;
    }

    public ArrFundBaseVO getFund() {
        return fund;
    }

    public void setFund(final ArrFundBaseVO fund) {
        this.fund = fund;
    }

    public ApScopeVO getScope() {
        return scope;
    }

    public void setScope(final ApScopeVO scope) {
        this.scope = scope;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Boolean getInherited() {
        return inherited;
    }

    public void setInherited(Boolean inherited) {
        this.inherited = inherited;
    }

    public UsrGroupVO getGroupControl() {
        return groupControl;
    }

    public void setGroupControl(UsrGroupVO groupControl) {
        this.groupControl = groupControl;
    }

    public UsrUserVO getUserControl() {
        return userControl;
    }

    public void setUserControl(UsrUserVO userControl) {
        this.userControl = userControl;
    }
}
