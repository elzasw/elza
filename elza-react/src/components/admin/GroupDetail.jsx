// --
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n, Icon, Tabs} from 'components/shared';
import {getIdsList} from 'stores/app/utils';
import {groupsGroupDetailFetchIfNeeded, joinUsers, leaveUser} from 'actions/admin/group';
import {modalDialogShow} from 'actions/global/modalDialog';
import {renderUserItem} from 'components/admin/adminRenderUtils';
import SelectItemsForm from './SelectItemsForm';
import UserField from './UserField';
import StoreHorizontalLoader from '../shared/loading/StoreHorizontalLoader';
import AdminRightsContainer from './AdminRightsContainer';
import AddRemoveList from '../shared/list/AddRemoveList';
import FundsPermissionPanel from './FundsPermissionPanel';
import ScopesPermissionPanel from './ScopesPermissionPanel';
import AdvancedPermissionPanel from './AdvancedPermissionPanel';
import {WebApi} from '../../actions/WebApi';
import DetailHeader from '../shared/detail/DetailHeader';

import './GroupDetail.scss';

class GroupDetail extends AbstractReactComponent {
    static TAB_FUNDS = 0;
    static TAB_SCOPES = 1;
    static TAB_ADVANCED = 2;

    static tabItems = [
        {id: GroupDetail.TAB_FUNDS, title: i18n('admin.perms.tabs.funds')},
        {id: GroupDetail.TAB_SCOPES, title: i18n('admin.perms.tabs.scopes')},
        {id: GroupDetail.TAB_ADVANCED, title: i18n('admin.perms.tabs.advanced')},
    ];

    /*
     * Template for selected items
     */
    defaultSelectedItem = {
        id: null,
        index: 0,
    };
    /*
     * Last selected fund item.
     */
    selectedFund = this.defaultSelectedItem;
    /*
     * Last selected scope item.
     */
    selectedScope = this.defaultSelectedItem;

    constructor(props) {
        super(props);

        this.state = {
            selectedTabItem: GroupDetail.tabItems[GroupDetail.TAB_FUNDS],
        };
    }

    componentDidMount() {
        this.props.dispatch(groupsGroupDetailFetchIfNeeded());
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const groupId = this.props.groupDetail.id;
        const nextGroupId = nextProps.groupDetail.id;

        // Reset selected permissions
        if (groupId !== nextGroupId) {
            this.selectedFund = this.defaultSelectedItem;
            this.selectedScope = this.defaultSelectedItem;
        }

        this.props.dispatch(groupsGroupDetailFetchIfNeeded());
    }

    handleRemoveUser = (user, index) => {
        const {groupDetail} = this.props;
        this.props.dispatch(leaveUser(groupDetail.id, user.id));
    };

    handleAddUsers = () => {
        const {groupDetail} = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('admin.group.user.add.title'),
                <SelectItemsForm
                    onSubmitForm={users => {
                        this.props.dispatch(joinUsers(groupDetail.id, getIdsList(users)));
                    }}
                    fieldComponent={UserField}
                    fieldComponentProps={{excludedGroupId: groupDetail.id}}
                    renderItem={renderUserItem}
                />,
            ),
        );
    };

    handleTabSelect = item => {
        this.setState({selectedTabItem: {id: Number(item)}});
    };

    renderTabContent = () => {
        const {groupDetail} = this.props;
        const {selectedTabItem} = this.state;

        switch (selectedTabItem.id) {
            case GroupDetail.TAB_FUNDS:
                return (
                    <FundsPermissionPanel
                        groupId={groupDetail.id}
                        onAddPermission={perm => WebApi.addGroupPermission(groupDetail.id, perm)}
                        onDeletePermission={perm => WebApi.deleteGroupPermission(groupDetail.id, perm)}
                        onDeleteFundPermission={fundId => WebApi.deleteGroupFundPermission(groupDetail.id, fundId)}
                        onSelectItem={(item, index) => {
                            this.selectedFund = {index, id: item.id};
                        }}
                        selectedPermission={this.selectedFund}
                    />
                );
            case GroupDetail.TAB_SCOPES:
                return (
                    <ScopesPermissionPanel
                        groupId={groupDetail.id}
                        onAddPermission={perm => WebApi.addGroupPermission(groupDetail.id, perm)}
                        onDeletePermission={perm => WebApi.deleteGroupPermission(groupDetail.id, perm)}
                        onDeleteScopePermission={scopeId => WebApi.deleteGroupScopePermission(groupDetail.id, scopeId)}
                        onSelectItem={(item, index) => {
                            this.selectedScope = {index, id: item.id};
                        }}
                        selectedPermission={this.selectedScope}
                    />
                );
            case GroupDetail.TAB_ADVANCED:
                return (
                    <AdvancedPermissionPanel
                        groupId={groupDetail.id}
                        onAddPermission={perm => WebApi.addGroupPermission(groupDetail.id, perm)}
                        onDeletePermission={perm => WebApi.deleteGroupPermission(groupDetail.id, perm)}
                    />
                );
            default:
                break;
        }
    };

    render() {
        const {groupDetail, groupCount} = this.props;
        const {selectedTabItem} = this.state;

        if (groupDetail.id === null) {
            return (
                <div className="group-detail-container">
                    <div className="unselected-msg">
                        <div className="title">
                            {groupCount > 0
                                ? i18n('admin.group.noSelection.title')
                                : i18n('admin.group.emptyList.title')}
                        </div>
                        <div className="msg-text">
                            {groupCount > 0
                                ? i18n('admin.group.noSelection.message')
                                : i18n('admin.group.emptyList.message')}
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div className="detail-container">
                <StoreHorizontalLoader store={groupDetail} />
                {groupDetail.fetched && (
                    <AdminRightsContainer
                        header={
                            <DetailHeader
                                icon={<Icon glyph="fa-group" />}
                                title={groupDetail.name}
                                subtitle={groupDetail.code}
                                flagLeft={i18n('admin.group.title')}
                            />
                        }
                        left={
                            <AddRemoveList
                                label={<h4>{i18n('admin.group.title.users')}</h4>}
                                addInLabel
                                items={groupDetail.users}
                                onAdd={this.handleAddUsers}
                                onRemove={this.handleRemoveUser}
                                addTitle="admin.user.user.action.add"
                                removeTitle="admin.user.user.action.delete"
                                renderItem={renderUserItem}
                                className="no-hover alternating-rows"
                            />
                        }
                    >
                        <div className="permissions-container">
                            <h4>{i18n('admin.group.title.permissions')}</h4>
                            <Tabs.Container>
                                <Tabs.Tabs
                                    asTabs
                                    items={GroupDetail.tabItems}
                                    activeItem={selectedTabItem}
                                    onSelect={this.handleTabSelect}
                                />
                                <Tabs.Content>{this.renderTabContent()}</Tabs.Content>
                            </Tabs.Container>
                        </div>
                    </AdminRightsContainer>
                )}
            </div>
        );
    }
}

GroupDetail.propTypes = {
    groupDetail: PropTypes.object.isRequired,
    groupCount: PropTypes.number.isRequired,
};

function mapStateToProps(state) {
    return {};
}

export default connect(mapStateToProps)(GroupDetail);
