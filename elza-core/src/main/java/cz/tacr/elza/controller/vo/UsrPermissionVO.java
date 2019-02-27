package cz.tacr.elza.controller.vo;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.UsrPermission;

/**
 * VO objektu jednoho oprávnění uživatele nebo skupiny.
 *
 */
public class UsrPermissionVO {
    /** Identifikátor. */
    private Integer id;

    /**
     * Je právo zděděné ze skupiny?
     * TODO: inherited a groupId jsou zjevně redundantní
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

    /** Protokol, ke kterému se vztahuje oprávnění. */
    private WfIssueListBaseVO issueList;

    public UsrPermissionVO() {

    }

    public UsrPermissionVO(UsrPermission srcPerm, boolean inheritedPermission, StaticDataProvider staticData) {
        id = srcPerm.getPermissionId();
        permission = srcPerm.getPermission();
        if (srcPerm.getFund() != null) {
            fund = ArrFundBaseVO.newInstance(srcPerm.getFund());
        }
        if (srcPerm.getScope() != null) {
            scope = ApScopeVO.newInstance(srcPerm.getScope(), staticData);
        }
        if (srcPerm.getGroupControl() != null) {
            groupControl = UsrGroupVO.newInstance(srcPerm.getGroupControl());
        }
        if (srcPerm.getUserControl() != null) {
            userControl = UsrUserVO.newInstance(srcPerm.getUserControl());
        }
        if (srcPerm.getIssueList() != null) {
            issueList = WfIssueListBaseVO.newInstance(srcPerm.getIssueList());
        }

        groupId = srcPerm.getGroupId();
        if (inheritedPermission) {
            inherited = true;
        }
    }

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

    public WfIssueListBaseVO getIssueList() {
        return issueList;
    }

    public void setIssueList(WfIssueListBaseVO issueList) {
        this.issueList = issueList;
    }

    public UsrPermission createEntity(StaticDataProvider staticData) {
        UsrPermission entity = new UsrPermission();

        entity.setPermissionId(id);
        if (fund != null) {
            entity.setFund(fund.createEntity());
        }
        entity.setPermission(permission);
        if (scope != null) {
            entity.setScope(scope.createEntity(staticData));
        }
        if (groupControl != null) {
            entity.setGroupControl(groupControl.createEntity());
        }
        if (userControl != null) {
            entity.setUserControl(userControl.createEntity());
        }
        if (issueList != null) {
            entity.setIssueList(issueList.createEntity());
        }
        return entity;
    }

    public static UsrPermissionVO newInstance(UsrPermission srcPerm, boolean inheritedPermission,
                                              StaticDataProvider staticData) {
        UsrPermissionVO vo = new UsrPermissionVO(srcPerm, inheritedPermission, staticData);
        return vo;
    }
}
