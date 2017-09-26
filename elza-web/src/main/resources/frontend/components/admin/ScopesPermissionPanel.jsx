// --
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import ListBox from "./../../components/shared/listbox/ListBox";
import * as perms from './../../actions/user/Permission.jsx';
import {HorizontalLoader} from "../shared/index";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as userPermissions from "./../../actions/admin/userPermissions";
import {WebApi} from "../../actions/WebApi";
import PermissionCheckboxsForm from "./PermissionCheckboxsForm";

/**
 * Panel spravující oprávnění na třídy rejstříků.
 */
class ScopesPermissionPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: [],
            selectedPermissionIndex: null
        };
    }

    static permCodesMap = {
        [perms.REG_SCOPE_RD_ALL]: perms.REG_SCOPE_RD,
        [perms.REG_SCOPE_WR_ALL]: perms.REG_SCOPE_WR,
    };

    static permCodesMapRev = {
        [perms.REG_SCOPE_RD]: perms.REG_SCOPE_RD_ALL,
        [perms.REG_SCOPE_WR]: perms.REG_SCOPE_WR_ALL,
    };

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
            // Mapa scopeId na mapu hodnot oprávnění pro danou třídu rejstříků, pokud se jedná o položku all, má id ALL_ID
            const permScopeMap = {};

            nextProps.userPermissions.data.permissions.forEach(p => {
                let id = null;
                let permissionCode;
                switch (p.permission) {
                    case perms.REG_SCOPE_RD_ALL:
                    case perms.REG_SCOPE_WR_ALL:
                        id = ScopesPermissionPanel.ALL_ID;
                        permissionCode = ScopesPermissionPanel.permCodesMap[p.permission];
                        break;
                    case perms.REG_SCOPE_RD:
                    case perms.REG_SCOPE_WR:
                        id = p.scope.id;
                        permissionCode = p.permission;
                        break;
                }

                if (id !== null) {
                    if (!permScopeMap[id]) {
                        permScopeMap[id] = {};
                    }
                    permScopeMap[id][permissionCode] = this.buildPermission(permScopeMap[id][permissionCode], p);
                    permScopeMap[id].scope = p.scope;
                    permScopeMap[id].id = id;
                }
            });

            const permissions = Object.values(permScopeMap);
            this.setState({permissions});
        }
    }

    changePermission = (e, permCode) => {
        const {userId} = this.props;
        const value = e.target.checked;
        const {selectedPermissionIndex, permissions} = this.state;
        const permission = permissions[selectedPermissionIndex];

        const newPermission = {
            ...permission
        };

        const obj = newPermission[permCode] || {groupIds: {}};

        const newObj = {
            ...obj,
            checked: value
        };
        newPermission[permCode] = newObj;

        const newPermissions = [
            ...permissions.slice(0, selectedPermissionIndex),
            newPermission,
            ...permissions.slice(selectedPermissionIndex + 1)
        ];

        const usrPermission = {
            id: obj.id,
            permission: permission.id === ScopesPermissionPanel.ALL_ID ? ScopesPermissionPanel.permCodesMapRev[permCode] : permCode,
            scope: permission.scope
        };

        if (value) {
            WebApi.addUserPermission(userId, usrPermission).then(data => {
                newObj.id = data.id;
                this.setState({
                    permissions: newPermissions,
                });
            });
        } else {
            WebApi.deleteUserPermission(userId, usrPermission).then(data => {
                newObj.id = null;
                this.setState({
                    permissions: newPermissions,
                });
            });
        }
    };

    render() {
        const {selectedPermissionIndex, permissions} = this.state;
        const {userPermissions} = this.props;

        return <div>
            {userPermissions.isFetching && <HorizontalLoader/>}
            <ListBox
                items={permissions}
                renderItemContent={(item, isActive, index, onCheckItem) => <div>{item.id === ScopesPermissionPanel.ALL_ID ? i18n("admin.user.tabs.scopes.items.scopeAll") : item.scope.name}</div>}
                activeIndex={selectedPermissionIndex}
                onFocus={(item, index) => {
                    this.setState({selectedPermissionIndex: index})
                }}
            />
            <div>
                {selectedPermissionIndex !== null && <PermissionCheckboxsForm
                    permCodes={Object.values(ScopesPermissionPanel.permCodesMap)}
                    onChangePermission={this.changePermission}
                    labelPrefix="admin.user.tabs.scopes.perm."
                    permission={permissions[selectedPermissionIndex]}
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


export default connect(mapStateToProps)(ScopesPermissionPanel);
