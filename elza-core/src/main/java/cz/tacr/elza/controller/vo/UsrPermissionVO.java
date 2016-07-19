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

    /** AS, ke kterému se vztahuje oprávnění. */
    private ArrFundBaseVO fund;

    /** Scope, ke kterému se vztahuje oprávnění. */
    private RegScopeVO scope;

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

    public RegScopeVO getScope() {
        return scope;
    }

    public void setScope(final RegScopeVO scope) {
        this.scope = scope;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }
}
