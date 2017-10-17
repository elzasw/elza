// --
import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, fetching} from 'components/shared';
import * as perms from './../../actions/user/Permission.jsx';
import storeFromArea from "../../shared/utils/storeFromArea";
import * as adminPermissions from "./../../actions/admin/adminPermissions";
import {WebApi} from "../../actions/WebApi";
import PermissionCheckboxsForm from "./PermissionCheckboxsForm";
import AdminRightsContainer from "./AdminRightsContainer";
import {modalDialogShow, modalDialogHide} from "../../actions/global/modalDialog";
import SelectItemsForm from "./SelectItemsForm";
import FundField from "./FundField";
import {renderFundItem, renderGroupItem} from "./adminRenderUtils";
import getMapFromList from "../../shared/utils/getMapFromList";
import AddRemoveListBox from "../shared/listbox/AddRemoveListBox";

/**
 * Panel spravující oprávnění na archivní soubory.
 */
class FundsPermissionPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: [],
            selectedPermissionIndex: null
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

    static ALL_ID = "ALL_ID";

    buildPermission = (currObj, permission) => {
        const {userId} = this.props;
        let obj = currObj || {groupIds: {}};

        if (permission.inherited) {   // je zděděné ze skupiny
            obj.groupIds[permission.groupId] = permission.fund ? permission.fund.id : true;
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
            // Mapa fundId na mapu hodnot oprávnění pro danou položku AS, pokud se jedná o položku all, má id ALL_ID
            const permFundMap = {};
            permFundMap[FundsPermissionPanel.ALL_ID] = {id: FundsPermissionPanel.ALL_ID, groupIds: {}};

            nextProps.entityPermissions.data.permissions.forEach(p => {
                let id = null;
                let permissionCode;
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
                        id = p.fund.id;
                        permissionCode = p.permission;
                        break;
                }

                if (id !== null) {
                    if (!permFundMap[id]) {
                        permFundMap[id] = {};
                    }
                    permFundMap[id][permissionCode] = this.buildPermission(permFundMap[id][permissionCode], p);
                    permFundMap[id].fund = p.fund;
                    permFundMap[id].id = id;
                }
            });

            const permissions = Object.values(permFundMap);
            permissions.sort((a, b) => {
                if (a.id === FundsPermissionPanel.ALL_ID) {
                    return -1;
                } else if (b.id === FundsPermissionPanel.ALL_ID) {
                    return 1;
                } else {
                    return a.fund.name.localeCompare(b.fund.name);
                }
            });

            this.setState({permissions});
        }
    }

    changePermission = (e, permCode) => {
        const {onAddPermission, onDeletePermission} = this.props;
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
            permission: permission.id === FundsPermissionPanel.ALL_ID ? FundsPermissionPanel.permCodesMapRev[permCode] : permCode,
            fund: permission.fund
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
        const {selectedPermissionIndex, permissions} = this.state;

        // Pokud má nějaké právo zděděné, musí položka po smazání zůstat, ale jen pokud je právo zděděné ze skupina přímo na daný fund
        let newPermissions;
        const permission = permissions[selectedPermissionIndex];
        let hasInheritRight = false;
        Object.values(FundsPermissionPanel.permCodesMap).forEach(permCode => {
            if (permission[permCode] && Object.keys(permission[permCode].groupIds).length > 0) {  // má nějaké zděděné právo
                // Test, zda je na tento fund
                Object.values(permission[permCode].groupIds).forEach(v => {
                    if (v !== true && v === item.id) {
                        hasInheritRight = true;
                    }
                });
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
            Object.values(FundsPermissionPanel.permCodesMap).forEach(permCode => {
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

        onDeleteFundPermission(item.id).then(data => {
            this.setState({
                permissions: newPermissions,
                selectedPermissionIndex: newSelectedPermissionIndex
            });
        });
    };

    handleAdd = () => {
        this.props.dispatch(modalDialogShow(this, i18n('admin.perms.tabs.funds.add.title'),
            <SelectItemsForm
                onSubmitForm={(funds) => {
                    const {permissions} = this.state;
                    const permissionsMap = getMapFromList(permissions);
                    const newPermissions = [...permissions];

                    funds.forEach(fund => {
                        if (!permissionsMap[fund.id]) { // jen pokud ještě přidaný není
                            newPermissions.push({
                                id: fund.id,
                                fund: fund,
                            });
                        }
                    });

                    this.setState({permissions: newPermissions});

                    this.props.dispatch(modalDialogHide());
                }}
                fieldComponent={FundField}
                renderItem={renderFundItem}
            />
        ));
    };

    renderItem = (item, isActive, index, onCheckItem) => {
        if (item.id === FundsPermissionPanel.ALL_ID) {
            return <div>{i18n("admin.perms.tabs.funds.items.fundAll")}</div>;
        } else {
            return <div>
                {item.fund.name}
            </div>;
        }
    };

    render() {
        const {selectedPermissionIndex, permissions} = this.state;
        const {entityPermissions} = this.props;

        let permission;
        let permCodes;
        if (selectedPermissionIndex !== null) {
            permission = permissions[selectedPermissionIndex];
            permCodes = [...Object.values(FundsPermissionPanel.permCodesMap)];
            if (permission.id !== FundsPermissionPanel.ALL_ID) {
                permCodes.push(perms.FUND_VER_WR);
            }
        }
        let permissionAll = permissions[permissions.findIndex(x => x.id === FundsPermissionPanel.ALL_ID)];

        return <AdminRightsContainer
            left={<AddRemoveListBox
                items={permissions}
                activeIndex={selectedPermissionIndex}
                renderItemContent={this.renderItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                canDeleteItem={(item, index) => item.id !== FundsPermissionPanel.ALL_ID}
                onFocus={(item, index) => {
                    this.setState({selectedPermissionIndex: index})
                }}
            />}
            >
            {selectedPermissionIndex !== null && <PermissionCheckboxsForm
                permCodes={permCodes}
                onChangePermission={this.changePermission}
                labelPrefix="admin.perms.tabs.funds.perm."
                permission={permission}
                groups={entityPermissions.data.groups}
                permissionAll={permission.id !== FundsPermissionPanel.ALL_ID ? permissionAll : null}
                permissionAllTitle="admin.perms.tabs.funds.items.fundAll"
            />}
        </AdminRightsContainer>;
    }
}

function mapStateToProps(state) {
    return {
        entityPermissions: storeFromArea(state, adminPermissions.ENTITY_PERMISSIONS)
    }
}


export default connect(mapStateToProps)(FundsPermissionPanel);
