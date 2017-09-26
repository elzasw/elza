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
            // Mapa fundId na mapu hodnot oprávnění pro danou položku AS, pokud se jedná o položku all, má id ALL_ID
            const permFundMap = {};

            nextProps.userPermissions.data.permissions.forEach(p => {
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
            permission: permission.id === FundsPermissionPanel.ALL_ID ? FundsPermissionPanel.permCodesMapRev[permCode] : permCode,
            fund: permission.fund
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

        let permission;
        let permCodes;
        if (selectedPermissionIndex !== null) {
            permission = permissions[selectedPermissionIndex];
            permCodes = [...Object.values(FundsPermissionPanel.permCodesMap)];
            if (permission.id !== FundsPermissionPanel.ALL_ID) {
                permCodes.push(perms.FUND_VER_WR);
            }

        }

        return <div>
            {userPermissions.isFetching && <HorizontalLoader/>}
            <ListBox
                items={permissions}
                renderItemContent={(item, isActive, index, onCheckItem) => <div>{item.id === FundsPermissionPanel.ALL_ID ? i18n("admin.user.tabs.funds.items.fundAll") : item.fund.name}</div>}
                activeIndex={selectedPermissionIndex}
                onFocus={(item, index) => {
                    this.setState({selectedPermissionIndex: index})
                }}
            />
            <div>
                {selectedPermissionIndex !== null && <PermissionCheckboxsForm
                    permCodes={permCodes}
                    onChangePermission={this.changePermission}
                    labelPrefix="admin.user.tabs.funds.perm."
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


export default connect(mapStateToProps)(FundsPermissionPanel);
