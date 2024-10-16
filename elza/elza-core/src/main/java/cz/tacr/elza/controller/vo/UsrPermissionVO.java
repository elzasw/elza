package cz.tacr.elza.controller.vo;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
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

    /** JP, ke které se vztahuje oprávnění. */
    private ArrNodeVO node;

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
        if (srcPerm.getNode() != null) {
            node = ArrNodeVO.newInstance(srcPerm.getNode());
        }

        groupId = srcPerm.getGroupId();
        if (inheritedPermission) {
            inherited = true;
            Validate.notNull(groupId);
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

    public ArrNodeVO getNode() {
        return node;
    }

    public void setNode(final ArrNodeVO node) {
        this.node = node;
    }

    public UsrPermission createEntity(StaticDataProvider staticData) {
        UsrPermission entity = new UsrPermission();

        entity.setPermissionId(id);
        if (fund != null) {
            Validate.notNull(fund.getId());

            entity.setFundId(fund.getId());
        }
        entity.setPermission(permission);
        if (scope != null) {
            Validate.notNull(scope.getId());

            entity.setScopeId(scope.getId());
        }
        if (groupControl != null) {
            Validate.notNull(groupControl.getId());

            entity.setGroupControlId(groupControl.getId());
        }
        if (userControl != null) {
            Validate.notNull(userControl.getId());

            entity.setUserControlId(userControl.getId());
        }
        if (issueList != null) {
            Validate.notNull(issueList.getId());

            entity.setIssueListId(issueList.getId());
        }
        if (node != null) {
            Validate.notNull(node.getId());

            entity.setNodeId(node.getId());
        }
        return entity;
    }

    public static UsrPermissionVO newInstance(UsrPermission srcPerm, boolean inheritedPermission,
                                              StaticDataProvider staticData) {
        return new UsrPermissionVO(srcPerm, inheritedPermission, staticData);
    }
}
