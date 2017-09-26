// --
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import * as perms from './../../actions/user/Permission.jsx';
import {HorizontalLoader} from "../shared/index";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as userPermissions from "./../../actions/admin/userPermissions";
import {WebApi} from "../../actions/WebApi";
import PermissionCheckboxsForm from "./PermissionCheckboxsForm";

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
        perms.USR_PERM,
        perms.INTERPI_MAPPING_WR,
    ];

    static ALL_ID = "ALL_ID";

    buildPermission = (currObj, permission) => {
        let obj = currObj || {groupIds: {}};

        if (permission.groupId) {   // je ze skupiny
            obj.groupIds[permission.groupId] = true;
            obj.checked = obj.checked || false;
        } else {    // je přímo přiřazen
            obj.id = permission.id;
            obj.checked = true;
        }

        return obj;
    };

    componentDidMount() {
        this.props.dispatch(userPermissions.fetch(this.props.userId));
    }

    componentWillReceiveProps(nextProps) {
        if (this.props.userPermissions.isFetching && !nextProps.userPermissions.isFetching) {
            const permission = {
                id: AdvancedPermissionPanel.ALL_ID
            };

            nextProps.userPermissions.data.permissions.forEach(p => {
                switch (p.permission) {
                    case perms.ADMIN:
                    case perms.USR_PERM:
                    case perms.INTERPI_MAPPING_WR:
                    case perms.FUND_ADMIN:
                        permission[p.permission] = this.buildPermission(permission[p.permission], p);
                        break;
                }
            });

            this.setState({permission});
        }
    }

    changePermission = (e, permCode) => {
        const {userId} = this.props;
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
            WebApi.addUserPermission(userId, usrPermission).then(data => {
                newObj.id = data.id;
                this.setState({permission: newPermission});
            });
        } else {
            WebApi.deleteUserPermission(userId, usrPermission).then(data => {
                newObj.id = null;
                this.setState({permission: newPermission});
            });
        }
    };

    render() {
        const {permission} = this.state;
        const {userPermissions} = this.props;

        return <div>
            {userPermissions.isFetching && <HorizontalLoader/>}
            <div>
                {permission && <PermissionCheckboxsForm
                    permCodes={AdvancedPermissionPanel.permCodes}
                    onChangePermission={this.changePermission}
                    labelPrefix="admin.user.tabs.advanced.perm."
                    permission={permission}
                    groups={userPermissions.data.groups}
                />}
            </div>
        </div>
    }
}

function mapStateToProps(state) {
    return {
        userPermissions: storeFromArea(state, userPermissions.USER_PERMISSIONS)
    }
}


export default connect(mapStateToProps)(AdvancedPermissionPanel);
