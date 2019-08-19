// --
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import ListBox from "./../../components/shared/listbox/ListBox";
import * as perms from './../../actions/user/Permission.jsx';
import {HorizontalLoader} from "../shared/index";
import storeFromArea from "../../shared/utils/storeFromArea";
import {modalDialogShow, modalDialogHide} from "../../actions/global/modalDialog";
import * as adminPermissions from "./../../actions/admin/adminPermissions";
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
            selectedPermission: props.selectedPermission,
            scopes: this.getScopes(props.scopesData)
        };
    }

    static permCodesMap = {
        [perms.AP_SCOPE_RD_ALL]: perms.AP_SCOPE_RD,
        [perms.AP_SCOPE_WR_ALL]: perms.AP_SCOPE_WR,
        [perms.AP_CONFIRM_ALL]: perms.AP_CONFIRM,
        [perms.AP_EDIT_CONFIRMED_ALL]: perms.AP_EDIT_CONFIRMED,
    };

    static permCodesMapRev = {
        [perms.AP_SCOPE_RD]: perms.AP_SCOPE_RD_ALL,
        [perms.AP_SCOPE_WR]: perms.AP_SCOPE_WR_ALL,
        [perms.AP_CONFIRM]: perms.AP_CONFIRM_ALL,
        [perms.AP_EDIT_CONFIRMED]: perms.AP_EDIT_CONFIRMED_ALL,
    };

    static ALL_ID = "ALL_ID";

    static defaultProps = {
        selectedPermission: {
            id: null,
            index: 0
        }
    }

    buildPermission = (currObj, permission) => {
        const {userId} = this.props;
        let obj = currObj || {groupIds: {}};

        if (permission.inherited) {   // je zděděné ze skupiny
            obj.groupIds[permission.groupId] = permission.scope ? permission.scope.id : true;
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
        let newState = {};
        let permissions = [...this.state.permissions];

        if (this.props.entityPermissions.isFetching && !nextProps.entityPermissions.isFetching) {
            // Mapa scopeId na mapu hodnot oprávnění pro danou třídu rejstříků, pokud se jedná o položku all, má id ALL_ID
            const permScopeMap = {};
            permScopeMap[ScopesPermissionPanel.ALL_ID] = {id: ScopesPermissionPanel.ALL_ID, groupIds: {}};

            nextProps.entityPermissions.data.permissions.forEach(p => {
                let id = null;
                let permissionCode;
                switch (p.permission) {
                    case perms.AP_SCOPE_RD_ALL:
                    case perms.AP_SCOPE_WR_ALL:
                    case perms.AP_CONFIRM_ALL:
                    case perms.AP_EDIT_CONFIRMED_ALL:
                        id = ScopesPermissionPanel.ALL_ID;
                        permissionCode = ScopesPermissionPanel.permCodesMap[p.permission];
                        break;
                    case perms.AP_SCOPE_RD:
                    case perms.AP_SCOPE_WR:
                    case perms.AP_CONFIRM:
                    case perms.AP_EDIT_CONFIRMED:
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

            permissions = Object.values(permScopeMap);
        }

        this.sortPermissions(permissions);

        // Creates new selected id from props. Uses state, if the selected permission in props didn't change.
        let newSelectedId = nextProps.selectedPermission.index >= 0 ? nextProps.selectedPermission.id : this.state.selectedPermission.id;
        let newSelectedIndex = this.getIndexById(newSelectedId, permissions);

        // Selects the first item, if the index is not found for the selected id.
        if(newSelectedIndex === -1) {
            newSelectedIndex = 0;
        }

        newState = {
            ...this.state,
            permissions
        }

        let permission = permissions[newSelectedIndex] || {id: null};

        this.selectItem(permission, newSelectedIndex);

        this.setState(newState);
    }
    /*
     * Sorts the scopes by their names. Puts the 'ALL' permissions at the beginning.
     */
    sortPermissions(permissions){
        permissions.sort((a, b) => {
            if (a.id === ScopesPermissionPanel.ALL_ID) {
                return -1;
            } else if (b.id === ScopesPermissionPanel.ALL_ID) {
                return 1;
            } else {
                return a.scope.name.localeCompare(b.scope.name);
            }
        });
    }

    changePermission = (e, permCode) => {
        const {onAddPermission, onDeletePermission} = this.props;
        const value = e.target.checked;
        const {selectedPermission, permissions} = this.state;
        const permission = permissions[selectedPermission.index];

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
            ...permissions.slice(0, selectedPermission.index),
            newPermission,
            ...permissions.slice(selectedPermission.index + 1)
        ];

        const usrPermission = {
            id: obj.id,
            permission: permission.id === ScopesPermissionPanel.ALL_ID ? ScopesPermissionPanel.permCodesMapRev[permCode] : permCode,
            scope: permission.scope
        };

        if (value) {
            onAddPermission([usrPermission]).then(data => {
                newObj.id = data[0].id;
                this.setState({
                    permissions: newPermissions,
                });
            });
        } else {
            onDeletePermission(usrPermission).then(data => {
                newObj.id = null;
                this.setState({
                    permissions: newPermissions,
                });
            });
        }
    };

    handleRemove = (item, index) => {
        const {onDeleteScopePermission} = this.props;
        const {selectedPermission, permissions} = this.state;

        // Prepare new permissions
        let newPermissions = [...permissions];

        // Pokud má nějaké právo zděděné, musí položka po smazání zůstat, ale jen pokud je právo zděděné ze skupina přímo na daný scope
        const permission = permissions[selectedPermission.index];
        let hasInheritRight = false;
        Object.values(ScopesPermissionPanel.permCodesMap).forEach(permCode => {
            if (permission[permCode] && Object.keys(permission[permCode].groupIds).length > 0) {  // má nějaké zděděné právo
                // Test, zda je na tento scope
                Object.values(permission[permCode].groupIds).forEach(v => {
                    if (v !== true && v === item.id) {
                        hasInheritRight = true;
                    }
                });
            }
        });

        let newSelectedPermissionIndex = selectedPermission.index;

        if (!hasInheritRight) {
            // Remove selected item
            newPermissions.splice(selectedPermission.index, 1);

            // Decrements index, if selected item is last.
            if(selectedPermission.index >= newPermissions.length){
                newSelectedPermissionIndex = newPermissions.length - 1;
            }
        } else {
            // Removes all permissions that are not inherited.
            let newPermission = {...permission};

            Object.values(ScopesPermissionPanel.permCodesMap).forEach(permCode => {
                if (newPermission[permCode]) {
                    newPermission[permCode] = {
                        ...newPermission[permCode],
                        id: null,
                        checked: false
                    };
                }
            });

            newPermissions[selectedPermission.index] = newPermission;
        }

        onDeleteScopePermission(item.id).then(data => {
            this.setState({
                permissions: newPermissions,
                selectedPermission: {
                    ...selectedPermission,
                    index: newSelectedPermissionIndex
                }
            });
        });
    };

    /*
     * Gets index of item, that has the specified id, from the specified array. If no index is found, returns -1.
     */
    getIndexById = (id, permissions) => {
        return permissions.findIndex(item => item.id === id)
    }

    handleAdd = () => {
        const {scopesData} = this.props;
        const {selectedPermission} = this.state;
        let scopes = this.getScopes(scopesData);

        this.props.dispatch(modalDialogShow(this, i18n('admin.perms.tabs.scopes.add.title'),
            <SelectItemsForm
                onSubmitForm={(scopes) => {
                    const {permissions} = this.state;
                    const permissionsMap = getMapFromList(permissions);
                    const newPermissions = [...permissions];
                    let newSelectedPermission = {...selectedPermission};

                    // Change the currently selected item, only if items have been added
                    if(scopes.length > 0){
                        let newSelectedId = null;

                        scopes.forEach(scope => {
                            if (!permissionsMap[scope.id]) { // jen pokud ještě přidaný není
                                // Select the first added item.
                                newSelectedId = newSelectedId === null ? scope.id : newSelectedId;
                                newPermissions.push({
                                    id: scope.id,
                                    scope: scope,
                                });
                            }
                        });

                        this.sortPermissions(newPermissions);

                        let newIndex = this.getIndexById(newSelectedId, newPermissions);

                        newSelectedPermission = {
                            id: newSelectedId,
                            index: newIndex
                        }
                    }

                    this.setState({
                        permissions: newPermissions,
                        selectedPermission: newSelectedPermission
                    });

                    this.props.dispatch(modalDialogHide());
                }}
                fieldComponent={ScopeField}
                fieldComponentProps={{scopes}}
                renderItem={renderScopeItem}
            />
        ));
    };

    renderItem = (props, onCheckItem) => {
        const {item, index} = props;
        if (item.id === ScopesPermissionPanel.ALL_ID) {
            return <div>{i18n("admin.perms.tabs.scopes.items.scopeAll")}</div>;
        } else {
            return <div>
                {item.scope.name}
            </div>;
        }
    };

    selectItem = (item, index) => {
        const {onSelectItem} = this.props;
        this.setState({
            selectedPermission: {
                index: index,
                id: item.id
            }
        });
        onSelectItem && onSelectItem(item, index);
    }

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

    render() {
        const {selectedPermission, permissions} = this.state;
        const {entityPermissions, onSelectItem} = this.props;

        const permission = permissions[selectedPermission.index];
        let permissionAll = permissions[this.getIndexById(ScopesPermissionPanel.ALL_ID, permissions)];

        return <AdminRightsContainer
            className="permissions-panel"
            left={<AddRemoveListBox
                items={permissions}
                activeIndex={selectedPermission.index}
                renderItemContent={this.renderItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                canDeleteItem={(item, index) => item.id !== ScopesPermissionPanel.ALL_ID}
                onFocus={this.selectItem}
            />}
            >
            {permission && <PermissionCheckboxsForm
                permCodes={Object.values(ScopesPermissionPanel.permCodesMap)}
                onChangePermission={this.changePermission}
                labelPrefix="admin.perms.tabs.scopes.perm."
                permission={permission}
                groups={entityPermissions.data.groups}
                permissionAll={permission.id !== ScopesPermissionPanel.ALL_ID ? permissionAll : null}
                permissionAllTitle="admin.perms.tabs.scopes.items.scopeAll"
            />}
        </AdminRightsContainer>
    }
}

function mapStateToProps(state) {
    return {
        entityPermissions: storeFromArea(state, adminPermissions.ENTITY_PERMISSIONS),
        scopesData: state.refTables.scopesData,
    }
}


export default connect(mapStateToProps)(ScopesPermissionPanel);
