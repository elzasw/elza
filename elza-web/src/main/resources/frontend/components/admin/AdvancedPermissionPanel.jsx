// --
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import * as perms from './../../actions/user/Permission.jsx';
import {HorizontalLoader} from "../shared/index";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as adminPermissions from "./../../actions/admin/adminPermissions";
import {WebApi} from "../../actions/WebApi";
import PermissionCheckboxsForm from "./PermissionCheckboxsForm";
import AdminRightsContainer from "./AdminRightsContainer";
import ControlledEntitiesPanel from "./ControlledEntitiesPanel";
import "./PermissionsPanel.less";

/**
 * Panel spravující pokročilá oprávnění.
 */
class AdvancedPermissionPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permission: null
        };
    }

    static permCodes = [
        perms.ADMIN,
        perms.FUND_ADMIN,
        perms.FUND_ISSUE_ADMIN_ALL,
        perms.FUND_CREATE,
        perms.USR_PERM,
        perms.INTERPI_MAPPING_WR,
    ];

    static ALL_ID = "ALL_ID";

    buildPermission = (currObj, permission) => {
        const {userId} = this.props;
        let obj = currObj || {groupIds: {}};

        if (permission.inherited) {   // je zděděné ze skupiny
            obj.groupIds[permission.groupId] = true;
            obj.checked = obj.checked || false;
        } else {    // je přímo přiřazen
            obj.id = permission.id;
            obj.checked = true;
        }

        return obj;
    };

    componentDidMount() {
        const {userId, groupId} = this.props;
        if (userId) {
            this.props.dispatch(adminPermissions.fetchUser(userId));
        } else {
            this.props.dispatch(adminPermissions.fetchGroup(groupId));
        }
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.entityPermissions.isFetching && !nextProps.entityPermissions.isFetching) {
            const permission = {
                id: AdvancedPermissionPanel.ALL_ID
            };

            nextProps.entityPermissions.data.permissions.forEach(p => {
                switch (p.permission) {
                    case perms.ADMIN:
                    case perms.USR_PERM:
                    case perms.INTERPI_MAPPING_WR:
                    case perms.FUND_ADMIN:
                    case perms.FUND_CREATE:
                        permission[p.permission] = this.buildPermission(permission[p.permission], p);
                        break;
                }
            });

            this.setState({permission});
        }
    }

    changePermission = (e, permCode) => {
        const {onAddPermission, onDeletePermission} = this.props;
        const value = e.target.checked;
        const {permission} = this.state;

        const newPermission = {
            ...permission
        };

        const obj = newPermission[permCode] || {groupIds: {}};

        const newObj = {
            ...obj,
            checked: value
        };
        newPermission[permCode] = newObj;

        const usrPermission = {
            id: obj.id,
            permission: permCode,
            scope: permission.scope
        };

        if (value) {
            onAddPermission([usrPermission]).then(data => {
                newObj.id = data[0].id;
                this.setState({permission: newPermission});
            });
        } else {
            onDeletePermission(usrPermission).then(data => {
                newObj.id = null;
                this.setState({permission: newPermission});
            });
        }
    };

    render() {
        const {permission} = this.state;
        const {onAddPermission, onDeletePermission, entityPermissions} = this.props;

        return <AdminRightsContainer className="permissions-panel">
                {permission && <PermissionCheckboxsForm
                    permCodes={AdvancedPermissionPanel.permCodes}
                    onChangePermission={this.changePermission}
                    labelPrefix="admin.perms.tabs.advanced.perm."
                    permission={permission}
                    groups={entityPermissions.data.groups}
                />}
                {entityPermissions.fetched && <div className="controlled-entities-container">
                    <ControlledEntitiesPanel
                        className="controlled-entities"
                        permissions={entityPermissions.data.permissions}
                        onAddPermission={onAddPermission}
                        onDeletePermission={onDeletePermission}
                    />
                </div>}
        </AdminRightsContainer>;
    }
}

function mapStateToProps(state) {
    return {
        entityPermissions: storeFromArea(state, adminPermissions.ENTITY_PERMISSIONS)
    }
}


export default connect(mapStateToProps)(AdvancedPermissionPanel);
