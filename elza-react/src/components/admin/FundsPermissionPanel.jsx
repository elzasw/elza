// --
import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, HorizontalLoader, i18n} from 'components/shared';
import * as perms from './../../actions/user/Permission.jsx';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as adminPermissions from './../../actions/admin/adminPermissions';
import PermissionCheckboxsForm from './PermissionCheckboxsForm';
import AdminRightsContainer from './AdminRightsContainer';
import {modalDialogHide, modalDialogShow} from '../../actions/global/modalDialog';
import SelectItemsForm from './SelectItemsForm';
import FundField from './FundField';
import {renderFundItem} from './adminRenderUtils';
import getMapFromList from '../../shared/utils/getMapFromList';
import AddRemoveListBox from '../shared/listbox/AddRemoveListBox';
import './PermissionsPanel.scss';

/**
 * Panel spravující oprávnění na archivní soubory.
 */
class FundsPermissionPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: [],
            selectedPermission: props.selectedPermission,
        };
    }

    static permCodesMap = {
        [perms.FUND_RD_ALL]: perms.FUND_RD,
        [perms.FUND_ARR_ALL]: perms.FUND_ARR,
        [perms.FUND_OUTPUT_WR_ALL]: perms.FUND_OUTPUT_WR,
        [perms.FUND_EXPORT_ALL]: perms.FUND_EXPORT,
        [perms.FUND_BA_ALL]: perms.FUND_BA,
        [perms.FUND_CL_VER_WR_ALL]: perms.FUND_CL_VER_WR,
    };

    static permCodesMapRev = {
        [perms.FUND_RD]: perms.FUND_RD_ALL,
        [perms.FUND_ARR]: perms.FUND_ARR_ALL,
        [perms.FUND_OUTPUT_WR]: perms.FUND_OUTPUT_WR_ALL,
        [perms.FUND_EXPORT]: perms.FUND_EXPORT_ALL,
        [perms.FUND_BA]: perms.FUND_BA_ALL,
        [perms.FUND_CL_VER_WR]: perms.FUND_CL_VER_WR_ALL,
    };

    static ALL_ID = adminPermissions.ALL_ID;

    static defaultProps = {
        selectedPermission: {
            id: null,
            index: 0,
        },
    };

    buildPermission = (currObj, permission) => {
        let obj = currObj || {groupIds: {}};

        if (permission.inherited) {
            // je zděděné ze skupiny
            obj.groupIds[permission.groupId] = permission.fund ? permission.fund.id : true;
            obj.checked = obj.checked || false;
        } else {
            // je přímo přiřazen
            obj.id = permission.id;
            obj.checked = true;
        }

        return obj;
    };

    buildNodePermission = (currObj, permission) => {
        let obj = currObj || {groupIds: {}};

        if (permission.groupId) {
            // je zděděné ze skupiny
            if (!obj.groupIds[permission.groupId]) {
                obj.groupIds[permission.groupId] = {
                    [permission.fund.id]: [],
                };
            }
            obj.groupIds[permission.groupId][permission.fund.id].push(permission.node.id);
        } else {
            // je přímo přiřazen
            if (!obj.ids) {
                obj.ids = [];
            }
            obj.ids.push(permission.node.id);
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

    UNSAFE_componentWillReceiveProps(nextProps) {
        let newState = {};
        let permissions = [...this.state.permissions];

        // Request new data, when user or group selection changes.
        if (this.props.userId !== nextProps.userId || this.props.groupId !== nextProps.groupId) {
            if (nextProps.userId) {
                this.props.dispatch(adminPermissions.fetchUser(nextProps.userId));
            } else {
                this.props.dispatch(adminPermissions.fetchGroup(nextProps.groupId));
            }
        }

        if (this.props.entityPermissions.isFetching && !nextProps.entityPermissions.isFetching) {
            // Mapa fundId na mapu hodnot oprávnění pro danou položku AS, pokud se jedná o položku all, má id ALL_ID
            const permFundMap = {};
            permFundMap[FundsPermissionPanel.ALL_ID] = {id: FundsPermissionPanel.ALL_ID, groupIds: {}};

            nextProps.entityPermissions.data.permissions.forEach(p => {
                let id = null;
                let permissionCode;
                let node;
                switch (p.permission) {
                    case perms.FUND_ARR_ALL:
                    case perms.FUND_EXPORT_ALL:
                    case perms.FUND_RD_ALL:
                    case perms.FUND_BA_ALL:
                    case perms.FUND_OUTPUT_WR_ALL:
                    case perms.FUND_CL_VER_WR_ALL:
                        id = FundsPermissionPanel.ALL_ID;
                        permissionCode = FundsPermissionPanel.permCodesMap[p.permission];
                        break;
                    case perms.FUND_ARR:
                    case perms.FUND_EXPORT:
                    case perms.FUND_RD:
                    case perms.FUND_BA:
                    case perms.FUND_OUTPUT_WR:
                    case perms.FUND_CL_VER_WR:
                    case perms.FUND_VER_WR:
                    case perms.FUND_ISSUE_ADMIN:
                        id = p.fund.id;
                        permissionCode = p.permission;
                        break;
                    case perms.FUND_ARR_NODE:
                        id = p.fund.id;
                        node = p.node.id;
                        permissionCode = p.permission;
                        break;
                    default:
                        break;
                }

                if (id !== null) {
                    if (!permFundMap[id]) {
                        permFundMap[id] = {};
                    }
                    if (node) {
                        permFundMap[id][permissionCode] = this.buildNodePermission(permFundMap[id][permissionCode], p);
                    } else {
                        permFundMap[id][permissionCode] = this.buildPermission(permFundMap[id][permissionCode], p);
                    }
                    permFundMap[id].fund = p.fund;
                    permFundMap[id].id = id;
                }
            });

            permissions = Object.values(permFundMap);
        }

        this.sortPermissions(permissions);

        // Creates new selected id from props. Uses state, if the selected permission in props didn't change.
        let newSelectedId =
            nextProps.selectedPermission.index >= 0
                ? nextProps.selectedPermission.id
                : this.state.selectedPermission.id;
        let newSelectedIndex = this.getIndexById(newSelectedId, permissions);

        // Selects the first item, if the index is not found for the selected id.
        if (newSelectedIndex === -1) {
            newSelectedIndex = 0;
        }

        newState = {
            ...this.state,
            permissions,
        };

        let permission = permissions[newSelectedIndex] || {id: null};

        this.selectItem(permission, newSelectedIndex);

        this.setState(newState);
    }

    /*
     * Sorts the funds by their names. Puts the 'ALL' permissions at the beginning.
     */
    sortPermissions(permissions) {
        permissions.sort((a, b) => {
            if (a.id === FundsPermissionPanel.ALL_ID) {
                return -1;
            } else if (b.id === FundsPermissionPanel.ALL_ID) {
                return 1;
            } else if (a.fund.name && b.fund.name) {
                return a.fund.name.localeCompare(b.fund.name);
            } else {
                return 1;
            }
        });
    }

    removeNodePermission = (fundId, node) => {
        const {onDeletePermission, entityPermissions, userId, groupId} = this.props;

        entityPermissions.data.permissions.forEach(perm => {
            if (perm.permission === 'FUND_ARR_NODE' && perm.fund.id === fundId && perm.node.id === node.id) {
                onDeletePermission(perm).then(data => {
                    if (userId) {
                        this.props.dispatch(adminPermissions.fetchUser(userId));
                    } else {
                        this.props.dispatch(adminPermissions.fetchGroup(groupId));
                    }
                });
            }
        });
    };

    addNodePermission = (fundId, nodes) => {
        const {onAddPermission, userId, groupId} = this.props;
        const permission = this.getPermission();

        const usrPermissions = nodes.map(node => {
            return {
                fund: permission.fund,
                permission: 'FUND_ARR_NODE',
                node: {
                    id: node.id,
                    version: node.version,
                },
            };
        });
        if (usrPermissions.length > 0) {
            onAddPermission(usrPermissions).then(data => {
                this.props.dispatch(modalDialogHide());
                if (userId) {
                    this.props.dispatch(adminPermissions.fetchUser(userId));
                } else {
                    this.props.dispatch(adminPermissions.fetchGroup(groupId));
                }
            });
        }
    };

    changePermission = (e, permCode) => {
        const {onAddPermission, onDeletePermission} = this.props;
        const value = e.target.checked;
        const {selectedPermission, permissions} = this.state;
        const permission = this.getPermission();

        const newPermission = {
            ...permission,
        };

        const obj = newPermission[permCode] || {groupIds: {}};

        const newObj = {
            ...obj,
            checked: value,
        };
        newPermission[permCode] = newObj;

        const newPermissions = [
            ...permissions.slice(0, selectedPermission.index),
            newPermission,
            ...permissions.slice(selectedPermission.index + 1),
        ];

        const usrPermission = {
            id: obj.id,
            permission:
                permission.id === FundsPermissionPanel.ALL_ID
                    ? FundsPermissionPanel.permCodesMapRev[permCode]
                    : permCode,
            fund: permission.fund,
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
        const {onDeleteFundPermission} = this.props;
        const {selectedPermission, permissions} = this.state;

        // Prepare new permissions
        let newPermissions = [...permissions];

        // Pokud má nějaké právo zděděné, musí položka po smazání zůstat, ale jen pokud je právo zděděné ze skupina přímo na daný fund
        const permission = this.getPermission();
        let hasInheritRight = false;
        Object.values(FundsPermissionPanel.permCodesMap).forEach(permCode => {
            if (permission[permCode] && Object.keys(permission[permCode].groupIds).length > 0) {
                // má nějaké zděděné právo
                // Test, zda je na tento fund
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
            if (selectedPermission.index >= newPermissions.length) {
                newSelectedPermissionIndex = newPermissions.length - 1;
            }
        } else {
            // Removes all permissions that are not inherited.
            let newPermission = {...permission};

            Object.values(FundsPermissionPanel.permCodesMap).forEach(permCode => {
                if (newPermission[permCode]) {
                    newPermission[permCode] = {
                        ...newPermission[permCode],
                        id: null,
                        checked: false,
                    };
                }
            });

            newPermissions[selectedPermission.index] = newPermission;
        }

        onDeleteFundPermission(item.id).then(data => {
            this.setState({
                permissions: newPermissions,
                selectedPermission: {
                    ...selectedPermission,
                    index: newSelectedPermissionIndex,
                },
            });
        });
    };

    /*
     * Gets index of item, that has the specified id, from the specified array. If no index is found, returns -1.
     */
    getIndexById = (id, permissions) => {
        return permissions.findIndex(item => item.id === id);
    };

    handleAdd = () => {
        const {selectedPermission} = this.state;

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('admin.perms.tabs.funds.add.title'),
                <SelectItemsForm
                    onSubmitForm={funds => {
                        const {permissions} = this.state;
                        const permissionsMap = getMapFromList(permissions);
                        const newPermissions = [...permissions];
                        let newSelectedPermission = {...selectedPermission};

                        // Change the currently selected item, only if items have been added
                        if (funds.length > 0) {
                            let newSelectedId = null;

                            funds.forEach(fund => {
                                if (!permissionsMap[fund.id]) {
                                    // jen pokud ještě přidaný není
                                    // Select the first added item.
                                    newSelectedId = newSelectedId === null ? fund.id : newSelectedId;
                                    newPermissions.push({
                                        id: fund.id,
                                        fund: fund,
                                    });
                                }
                            });

                            this.sortPermissions(newPermissions);

                            let newIndex = this.getIndexById(newSelectedId, newPermissions);

                            newSelectedPermission = {
                                id: newSelectedId,
                                index: newIndex,
                            };
                        }

                        this.setState({
                            permissions: newPermissions,
                            selectedPermission: newSelectedPermission,
                        });

                        this.props.dispatch(modalDialogHide());
                    }}
                    fieldComponent={FundField}
                    renderItem={({item}) => item?.name}
                />,
            ),
        );
    };

    renderItem = (props, onCheckItem) => {
        const {item} = props;
        //console.log("fund perms panel", item);
        if (item.id === FundsPermissionPanel.ALL_ID) {
            return <div>{i18n('admin.perms.tabs.funds.items.fundAll')}</div>;
        } else if (item.fund) {
            return <div>{item.fund.name + (item.fund.internalCode? " [" + item.fund.internalCode + "]":"")}</div>;
        }
    };

    selectItem = (item, index) => {
        const {onSelectItem} = this.props;
        this.setState({
            selectedPermission: {
                index: index,
                id: item.id,
            },
        });
        onSelectItem && onSelectItem(item, index);
    };

    /*
     * Returns permissions for fund
     */
    getPermission = () => {
        const {selectedPermission, permissions} = this.state;
        const {fundId} = this.props;

        let permission;
        if (fundId) {
            permission = permissions.find((permission) => permission.id.toString() === fundId.toString())
                || {
                    id: fundId,
                    fund: {
                        id: fundId,
                    },
                };
        } else if (selectedPermission.index != null) {
                permission = permissions[selectedPermission.index];
        }
        return permission;
    };

    render() {
        const {selectedPermission, permissions} = this.state;
        const {fundId, entityPermissions, groupId} = this.props;

        if (!entityPermissions.fetched) {
            return <HorizontalLoader />;
        }

        let permission = this.getPermission();

        let permCodes;
        if (permission) {
            permCodes = [...Object.values(FundsPermissionPanel.permCodesMap)];
            if (permission.id !== FundsPermissionPanel.ALL_ID) {
                permCodes.push(perms.FUND_VER_WR);
                permCodes.push(perms.FUND_ISSUE_ADMIN);
            }
        }
        let permissionAll = permissions[this.getIndexById(FundsPermissionPanel.ALL_ID, permissions)];

        let left;
        if (!fundId) {
            left = (
                <AddRemoveListBox
                    items={permissions}
                    activeIndex={selectedPermission.index}
                    renderItemContent={this.renderItem}
                    onAdd={this.handleAdd}
                    onRemove={this.handleRemove}
                    canDeleteItem={(item, index) => item.id !== FundsPermissionPanel.ALL_ID}
                    onFocus={this.selectItem}
                    className="no-hover alternating-rows"
                />
            );
        }

        return (
            <AdminRightsContainer className="permissions-panel" left={left}>
                {permission && (
                    <PermissionCheckboxsForm
                        permCodes={permCodes}
                        onChangePermission={this.changePermission}
                        onAddNodePermission={this.addNodePermission}
                        onRemoveNodePermission={this.removeNodePermission}
                        labelPrefix="admin.perms.tabs.funds.perm."
                        permission={permission}
                        groups={entityPermissions.data.groups}
                        permissionAll={permission.id !== FundsPermissionPanel.ALL_ID ? permissionAll : null}
                        permissionAllTitle="admin.perms.tabs.funds.items.fundAll"
                        fundId={selectedPermission.id === FundsPermissionPanel.ALL_ID ? null : selectedPermission.id}
                        groupId={groupId}
                    />
                )}
            </AdminRightsContainer>
        );
    }
}

function mapStateToProps(state) {
    return {
        entityPermissions: storeFromArea(state, adminPermissions.ENTITY_PERMISSIONS),
    };
}

export default connect(mapStateToProps)(FundsPermissionPanel);
