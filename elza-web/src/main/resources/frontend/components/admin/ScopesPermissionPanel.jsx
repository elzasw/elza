// --
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import ListBox from "./../../components/shared/listbox/ListBox";
import * as perms from './../../actions/user/Permission.jsx';
import {HorizontalLoader} from "../shared/index";
import storeFromArea from "../../shared/utils/storeFromArea";
import {modalDialogShow, modalDialogHide} from "../../actions/global/modalDialog";
import * as userPermissions from "./../../actions/admin/userPermissions";
import {WebApi} from "../../actions/WebApi";
import PermissionCheckboxsForm from "./PermissionCheckboxsForm";
import AdminRightsContainer from "./AdminRightsContainer";
import AddRemoveListBox from "../shared/listbox/AddRemoveListBox";
import SelectItemsForm from "./SelectItemsForm";
import getMapFromList from "../../shared/utils/getMapFromList";
import {renderScopeItem} from "./adminRenderUtils";
import ScopeField from "./ScopeField";
import indexById from "../../shared/utils/indexById";
import {requestScopesIfNeeded} from "../../actions/refTables/scopesData";

/**
 * Panel spravující oprávnění na třídy rejstříků.
 */
class ScopesPermissionPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: [],
            selectedPermissionIndex: null,
            scopes: this.getScopes(props.scopesData)
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
        const scopes = this.getScopes(nextProps.scopesData);

        if (this.props.userPermissions.isFetching && !nextProps.userPermissions.isFetching) {
            // Mapa scopeId na mapu hodnot oprávnění pro danou třídu rejstříků, pokud se jedná o položku all, má id ALL_ID
            const permScopeMap = {};
            permScopeMap[ScopesPermissionPanel.ALL_ID] = {id: ScopesPermissionPanel.ALL_ID, groupIds: {}};

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

            permissions.sort((a, b) => {
                if (a.id === ScopesPermissionPanel.ALL_ID) {
                    return -1;
                } else if (b.id === ScopesPermissionPanel.ALL_ID) {
                    return 1;
                } else {
                    return a.scope.name.localeCompare(b.scope.name);
                }
            });

            this.setState({scopes, permissions});
        } else {
            this.setState({scopes});
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

    renderItem = (item, isActive, index, onCheckItem) => {
        if (item.id === ScopesPermissionPanel.ALL_ID) {
            return <div>{i18n("admin.user.tabs.scopes.items.scopeAll")}</div>;
        } else {
            return <div>
                {item.scope.name}
            </div>;
        }
    };

    /**
     * Získá pole dostupných scope ze store. Pokud ve store žádný neexistuje, načte je ze serveru.
     * @param {object} scopesData
     * @return {array}
     */
    getScopes = (scopesData) => {
        const versionId = -1;
        if (!scopesData.scopes){
            this.props.dispatch(requestScopesIfNeeded(versionId));
        }
        const scopeIndex = indexById(scopesData.scopes, versionId, 'versionId');
        let scopes;
        if (scopeIndex !== null) {
            scopes = scopesData.scopes[scopeIndex].scopes;
        } else {
            scopes = [];
        }
        return scopes;
    };

    handleRemove = (item, index) => {
        const {userId} = this.props;
        const {selectedPermissionIndex, permissions} = this.state;

        // Pokud má nějaké právo zděděné, musí položka po smazání zůstat
        let newPermissions;
        let permissionAll = permissions[permissions.findIndex(x => x.id === ScopesPermissionPanel.ALL_ID)];
        const permission = permissions[selectedPermissionIndex];
        let hasInheritRight = false;
        Object.values(ScopesPermissionPanel.permCodesMap).forEach(permCode => {
            if (permissionAll[permCode] && Object.keys(permissionAll[permCode].groupIds).length > 0) {
                hasInheritRight = true;
            }
        });
        let newSelectedPermissionIndex;
        if (!hasInheritRight) {
            newSelectedPermissionIndex = null;
            newPermissions = [
                ...permissions.slice(0, selectedPermissionIndex),
                ...permissions.slice(selectedPermissionIndex + 1)
            ];
        } else {
            newSelectedPermissionIndex = selectedPermissionIndex;

            const newPermission = {...permission};
            Object.values(ScopesPermissionPanel.permCodesMap).forEach(permCode => {
                if (newPermission[permCode]) {
                    newPermission[permCode] = {
                        ...newPermission[permCode],
                        id: null,
                        checked: false
                    };
                }
            });

            newPermissions = [
                ...permissions.slice(0, selectedPermissionIndex),
                newPermission,
                ...permissions.slice(selectedPermissionIndex + 1)
            ];
        }

        WebApi.deleteUserScopePermission(userId, item.id).then(data => {
            this.setState({
                permissions: newPermissions,
                selectedPermissionIndex: newSelectedPermissionIndex
            });
        });
    };

    handleAdd = () => {
        const {scopes} = this.state;

        this.props.dispatch(modalDialogShow(this, i18n('admin.user.tabs.scopes.add.title'),
            <SelectItemsForm
                onSubmitForm={(scopes) => {
                    const {permissions} = this.state;
                    const permissionsMap = getMapFromList(permissions);
                    const newPermissions = [...permissions];

                    scopes.forEach(scope => {
                        if (!permissionsMap[scope.id]) { // jen pokud ještě přidaný není
                            newPermissions.push({
                                id: scope.id,
                                scope: scope,
                            });
                        }
                    });

                    this.setState({permissions: newPermissions});

                    this.props.dispatch(modalDialogHide());
                }}
                fieldComponent={ScopeField}
                fieldComponentProps={{scopes}}
                renderItem={renderScopeItem}
            />
        ));
    };

    render() {
        const {selectedPermissionIndex, permissions} = this.state;
        const {userPermissions} = this.props;

        const permission = permissions[selectedPermissionIndex];
        let permissionAll = permissions[permissions.findIndex(x => x.id === ScopesPermissionPanel.ALL_ID)];

        return <AdminRightsContainer
            left={<AddRemoveListBox
                items={permissions}
                activeIndex={selectedPermissionIndex}
                renderItemContent={this.renderItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                onFocus={(item, index) => {
                    this.setState({selectedPermissionIndex: index})
                }}
            />}
            >
            {selectedPermissionIndex !== null && <PermissionCheckboxsForm
                permCodes={Object.values(ScopesPermissionPanel.permCodesMap)}
                onChangePermission={this.changePermission}
                labelPrefix="admin.user.tabs.scopes.perm."
                permission={permission}
                groups={userPermissions.data.groups}
                permissionAll={permission.id !== ScopesPermissionPanel.ALL_ID ? permissionAll : null}
                permissionAllTitle="admin.user.tabs.scopes.items.scopeAll"
            />}
        </AdminRightsContainer>
    }
}

function mapStateToProps(state) {
    return {
        userPermissions: storeFromArea(state, userPermissions.USER_PERMISSIONS),
        scopesData: state.refTables.scopesData,
    }
}


export default connect(mapStateToProps)(ScopesPermissionPanel);
