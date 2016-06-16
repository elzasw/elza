package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.UsrPermission;

/**
 * VO objektu jednoho oprávnění uživatele nebo skupiny.
 *
 * @author Pavel Stánek
 * @since 16.06.2016
 */
public class UsrPermissionVO {
    /** Identifikátor. */
    private Integer id;
    /** Typ oprávnění. */
    private UsrPermission.Permission permission;
    /** Identifikátor AS, ke kterému se vztahuje oprávnění. */
    private Integer fundId;
    /** Identifikátor scope, ke kter0mu se vztahuje oprávnění. */
    private Integer scopeId;

    /** Typ oprávnění. */
    public UsrPermission.Permission getPermission() {
        return permission;
    }

    public void setPermission(final UsrPermission.Permission permission) {
        this.permission = permission;
    }

    public Integer getFundId() {
        return fundId;
    }

    public void setFundId(final Integer fundId) {
        this.fundId = fundId;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }
}
