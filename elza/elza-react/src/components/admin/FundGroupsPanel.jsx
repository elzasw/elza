// --
import React from 'react';
import { connect } from 'react-redux';
import { AbstractReactComponent, HorizontalLoader, i18n } from 'components/shared';
import * as adminPermissions from '../../actions/admin/adminPermissions';
import storeFromArea from '../../shared/utils/storeFromArea';
import { modalDialogHide, modalDialogShow } from '../../actions/global/modalDialog';
import AdminRightsContainer from './AdminRightsContainer';
import AddRemoveListBox from '../shared/listbox/AddRemoveListBox';
import { renderGroupItem } from './adminRenderUtils';
import FundsPermissionPanel from './FundsPermissionPanel';
import { WebApi } from '../../actions/WebApi';
import SelectItemsForm from './SelectItemsForm';
import getMapFromList from '../../shared/utils/getMapFromList';
import GroupField from './GroupField';

class FundGroupsPanel extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            permissions: [],
            selectedPermission: props.selectedPermission,
        };
    }

    static defaultProps = {
        selectedPermission: {
            id: null,
            index: 0,
        },
    };

    componentDidMount() {
        const { fundId } = this.props;
        this.props.dispatch(adminPermissions.fetchGroupsByFund(fundId));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        let newState = {};
        let permissions = [...this.state.permissions];
        let propsSelectedPermissionChanged = false;

        // Request new data, when fund selection changes.
        if (this.props.fundId !== nextProps.fundId) {
            this.props.dispatch(adminPermissions.fetchGroupsByFund(nextProps.fundId));
        }

        // Check if the selected permission in props changed.
        if (this.props.selectedPermission !== nextProps.selectedPermission) {
            propsSelectedPermissionChanged = true;
        }

        // Copy the permissions from props, when successfully fetched.
        if (this.props.groups.isFetching && !nextProps.groups.isFetching) {
            permissions = [...nextProps.groups.rows];
        }

        this.sortPermissions(permissions);

        // Creates new selected id from props. Uses state, if the selected permission in props didn't change.
        let newSelectedId = propsSelectedPermissionChanged ? nextProps.selectedPermission.id : this.state.selectedPermission.id;
        let newSelectedIndex = this.getIndexById(newSelectedId, permissions);

        // Selects the first item, if the index is not found for the selected id.
        if (newSelectedIndex === -1) {
            newSelectedIndex = 0;
        }

        newState = {
            ...this.state,
            permissions,
        };

        let permission = permissions[newSelectedIndex] || { id: null };

        this.selectItem(permission, newSelectedIndex);

        this.setState(newState);
    }

    /*
     * Sorts the groups by their names.
     */
    sortPermissions(permissions) {
        permissions.sort((a, b) => {
            return a.name.localeCompare(b.name);
        });
    }

    handleRemove = (item, index) => {
        const { fundId } = this.props;
        const { selectedPermission, permissions } = this.state;

        let newPermissions = [...permissions];

        let newSelectedPermissionIndex = selectedPermission.index;

        const api = fundId === FundsPermissionPanel.ALL_ID ? WebApi.deleteGroupFundAllPermission(item.id) : WebApi.deleteGroupFundPermission(item.id, fundId);
        api.then(data => {
            // Remove selected item
            newPermissions.splice(index, 1);

            // Decrements index, if selected item is last.
            if (selectedPermission.index >= newPermissions.length) {
                newSelectedPermissionIndex = newPermissions.length - 1;
            }
            this.setState({
                permissions: newPermissions,
                selectedPermission: {
                    index: newSelectedPermissionIndex,
                    id: null,
                },
            });
            //this.props.dispatch(changeGroupsForFund(fundId, newPermissions));
        });
    };

    /*
     * Gets index of item, that has the specified id, from the specified array. If no index is found, returns -1.
     */
    getIndexById = (id, permissions) => {
        return permissions.findIndex(item => item.id === id);
    };

    handleAdd = () => {
        //const {fundId} = this.props;
        const { selectedPermission } = this.state;

        this.props.dispatch(modalDialogShow(this, i18n('admin.perms.fund.tabs.groups.add.title'),
            <SelectItemsForm
                onSubmitForm={(groups) => {
                    const { permissions } = this.state;
                    const permissionsMap = getMapFromList(permissions);
                    const newPermissions = [...permissions];
                    let newSelectedPermission = { ...selectedPermission };

                    // Change the currently selected item only if items have been added
                    if (groups.length > 0) {
                        let newSelectedId = null;

                        groups.forEach(group => {
                            if (!permissionsMap[group.id]) { // jen pokud ještě přidaný není
                                // Select the first added item.
                                newSelectedId = newSelectedId === null ? group.id : newSelectedId;
                                newPermissions.push({
                                    ...group,
                                    permissions: [],
                                    users: [],
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

                    //this.props.dispatch(changeGroupsForFund(fundId, newPermissions));
                    this.setState({
                        permissions: newPermissions,
                        selectedPermission: newSelectedPermission,
                    });

                    this.props.dispatch(modalDialogHide());
                }}
                fieldComponent={GroupField}
                renderItem={renderGroupItem}
            />,
        ));
    };

    selectItem = (item, index) => {
        const { onSelectItem } = this.props;
        this.setState({
            selectedPermission: {
                index: index,
                id: item.id,
            },
        });
        onSelectItem && onSelectItem(item, index);
    };

    render() {
        const { fundId, groups } = this.props;
        const { selectedPermission, permissions } = this.state;

        if (!groups.fetched) {
            return <HorizontalLoader/>;
        }

        const group = selectedPermission.index !== null ? permissions[selectedPermission.index] : null;

        return <AdminRightsContainer
            className="permissions-panel"
            left={<AddRemoveListBox
                items={permissions}
                activeIndex={selectedPermission.index}
                renderItemContent={renderGroupItem}
                onAdd={this.handleAdd}
                onRemove={this.handleRemove}
                onFocus={this.selectItem}
            />}
        >
            {group && <FundsPermissionPanel
                fundId={fundId}
                groupId={group.id}
                onAddPermission={perm => WebApi.addGroupPermission(group.id, perm)}
                onDeletePermission={perm => WebApi.deleteGroupPermission(group.id, perm)}
                onDeleteFundPermission={fundId => WebApi.deleteGroupFundPermission(group.id, fundId)}
            />}
        </AdminRightsContainer>;
    }
}

function mapStateToProps(state) {
    return {
        groups: storeFromArea(state, adminPermissions.GROUPS_PERMISSIONS_BY_FUND),
    };
}

export default connect(mapStateToProps)(FundGroupsPanel);
