// --
import React from 'react';
import {connect} from 'react-redux'
import {HorizontalLoader, AbstractReactComponent, Icon, i18n, fetching} from 'components/shared';
import * as adminPermissions from "../../actions/admin/adminPermissions";
import storeFromArea from "../../shared/utils/storeFromArea";
import {modalDialogShow, modalDialogHide} from "../../actions/global/modalDialog";
import AdminRightsContainer from "./AdminRightsContainer";
import AddRemoveListBox from "../shared/listbox/AddRemoveListBox";
import {renderUserItem} from "./adminRenderUtils";
import FundsPermissionPanel from "./FundsPermissionPanel";
import {WebApi} from "../../actions/WebApi";
import SelectItemsForm from "./SelectItemsForm";
import getMapFromList from "../../shared/utils/getMapFromList";
import UserField from "./UserField";
import {changeUsersForFund} from "../../actions/admin/adminPermissions";

class FundUsersPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: [],
            selectedPermission: props.selectedPermission
        };
    }

    static defaultProps = {
        selectedPermission: {
            id: null,
            index: 0
        }
    }

    componentDidMount() {
        const {fundId} = this.props;
        this.props.dispatch(adminPermissions.fetchUsersByFund(fundId));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        let newState = {};
        let permissions = [...this.state.permissions];
        let propsSelectedPermissionChanged = false;

        // Request new data, when fund selection changes.
        if (this.props.fundId !== nextProps.fundId) {
            this.props.dispatch(adminPermissions.fetchUsersByFund(nextProps.fundId));
        }

        // Check if the selected permission in props changed.
        if(this.props.selectedPermission !== nextProps.selectedPermission){
            propsSelectedPermissionChanged = true;
        }

        // Copy the permissions from props, when successfully fetched.
        if (this.props.users.isFetching && !nextProps.users.isFetching) {
            permissions = [...nextProps.users.rows];
        }

        this.sortPermissions(permissions);

        // Creates new selected id from props. Uses state, if the selected permission in props didn't change.
        let newSelectedId = propsSelectedPermissionChanged ? nextProps.selectedPermission.id : this.state.selectedPermission.id;
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
     * Sorts the users by their names.
     */
    sortPermissions(permissions){
        permissions.sort((a, b) => {
            return a.party.accessPoint.record.localeCompare(b.party.accessPoint.record);
        });
    }

    handleRemove = (item, index) => {
        const {fundId, onSelectItem} = this.props;
        const {selectedPermission, permissions} = this.state;

        // Prepare new permissions
        let newPermissions = [...permissions];

        let newSelectedPermissionIndex = selectedPermission.index;

        const api = fundId === FundsPermissionPanel.ALL_ID ? WebApi.deleteUserFundAllPermission(item.id) : WebApi.deleteUserFundPermission(item.id, fundId);
        api.then(data => {
            // Remove selected item
            newPermissions.splice(index, 1);

            // Decrements index, if selected item is last.
            if(selectedPermission.index >= newPermissions.length){
                newSelectedPermissionIndex = newPermissions.length - 1;
            }
            this.setState({
                permissions: newPermissions,
                selectedPermission: {
                    index: newSelectedPermissionIndex,
                    id: null
                }
            });
            //this.props.dispatch(changeUsersForFund(fundId, newPermissions));
        });
    };

    /*
     * Gets index of item, that has the specified id, from the specified array. If no index is found, returns -1.
     */
    getIndexById = (id, permissions) => {
        return permissions.findIndex(item => item.id === id)
    }

    handleAdd = () => {
        //const {fundId} = this.props;
        const {selectedPermission} = this.state;

        this.props.dispatch(modalDialogShow(this, i18n('admin.perms.fund.tabs.users.add.title'),
            <SelectItemsForm
                onSubmitForm={(users) => {
                    const {permissions} = this.state;
                    const permissionsMap = getMapFromList(permissions);
                    const newPermissions = [...permissions];
                    let newSelectedPermission = {...selectedPermission};

                    // Change the currently selected item, only if items have been added
                    if(users.length > 0){
                        let newSelectedId = null;

                        users.forEach(user => {
                            if (!permissionsMap[user.id]) { // jen pokud ještě přidaný není
                                // Select the first added item.
                                newSelectedId = newSelectedId === null ? user.id : newSelectedId;
                                newPermissions.push({
                                    ...user,
                                    permissions: [],
                                    groups: []
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

                    //this.props.dispatch(changeUsersForFund(fundId, newPermissions));
                    this.setState({
                        permissions: newPermissions,
                        selectedPermission: newSelectedPermission
                    });

                    this.props.dispatch(modalDialogHide());
                }}
                fieldComponent={UserField}
                renderItem={renderUserItem}
            />
        ));
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

    render() {
        const {fundId, users} = this.props;
        const {selectedPermission, permissions} = this.state;

        if (!users.fetched) {
            return <HorizontalLoader/>
        }

        const user = selectedPermission.index !== null ? permissions[selectedPermission.index] : null;

        return <AdminRightsContainer
            className="permissions-panel"
            left={<AddRemoveListBox
                items={permissions}
                activeIndex={selectedPermission.index}
                renderItemContent={renderUserItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                onFocus={this.selectItem}
            />}
        >
            {user && <FundsPermissionPanel
                fundId={fundId}
                userId={user.id}
                onAddPermission={perm => WebApi.addUserPermission(user.id, perm)}
                onDeletePermission={perm => WebApi.deleteUserPermission(user.id, perm)}
                onDeleteFundPermission={fundId => WebApi.deleteUserFundPermission(user.id, fundId)}
            />}
        </AdminRightsContainer>
    }
}

function mapStateToProps(state) {
    return {
        users: storeFromArea(state, adminPermissions.USERS_PERMISSIONS_BY_FUND),
    }
}

export default connect(mapStateToProps)(FundUsersPanel);
