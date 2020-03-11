// --
import PropTypes from 'prop-types';

import React from 'react';
import { connect } from 'react-redux';
import { AbstractReactComponent, AddRemoveList, i18n, Icon, StoreHorizontalLoader, Tabs } from 'components/shared';
import { getIdsList } from 'stores/app/utils.jsx';
import { joinGroups, leaveGroup, usersUserDetailFetchIfNeeded } from 'actions/admin/user.jsx';
import { modalDialogShow } from 'actions/global/modalDialog.jsx';
import { renderGroupItem } from 'components/admin/adminRenderUtils.jsx';
import './UserDetail.scss';
import FundsPermissionPanel from './FundsPermissionPanel';
import ScopesPermissionPanel from './ScopesPermissionPanel';
import AdvancedPermissionPanel from './AdvancedPermissionPanel';
import SelectItemsForm from './SelectItemsForm';
import GroupField from './GroupField';
import AdminRightsContainer from './AdminRightsContainer';
import { WebApi } from '../../actions/WebApi';
import DetailHeader from '../shared/detail/DetailHeader';

/**
 * Detail uživatele s nastavením oprávnění.
 */
class UserDetail extends AbstractReactComponent {
    static propTypes = {
        userDetail: PropTypes.object.isRequired,
        userCount: PropTypes.number.isRequired,
    };

    static TAB_FUNDS = 0;
    static TAB_SCOPES = 1;
    static TAB_ADVANCED = 2;

    static tabItems = [
        { id: UserDetail.TAB_FUNDS, title: i18n('admin.perms.tabs.funds') },
        { id: UserDetail.TAB_SCOPES, title: i18n('admin.perms.tabs.scopes') },
        { id: UserDetail.TAB_ADVANCED, title: i18n('admin.perms.tabs.advanced') },
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
            selectedTabItem: UserDetail.tabItems[UserDetail.TAB_FUNDS],
        };
    }

    componentDidMount() {
        this.props.dispatch(usersUserDetailFetchIfNeeded());
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const userId = this.props.userDetail.id;
        const nextUserId = nextProps.userDetail.id;

        // Reset selected permissions
        if (userId !== nextUserId) {
            this.selectedFund = this.defaultSelectedItem;
            this.selectedScope = this.defaultSelectedItem;
        }

        this.props.dispatch(usersUserDetailFetchIfNeeded());
    }

    handleRemoveGroup = (group, index) => {
        const { userDetail } = this.props;
        console.log('remove group', group);
        this.props.dispatch(leaveGroup(userDetail.id, group.id));
    };

    handleAddGroups = () => {
        const { userDetail } = this.props;
        this.props.dispatch(modalDialogShow(this, i18n('admin.user.group.add.title'),
            <SelectItemsForm
                onSubmitForm={(groups) => {
                    this.props.dispatch(joinGroups(userDetail.id, getIdsList(groups)));
                }}
                fieldComponent={GroupField}
                renderItem={renderGroupItem}
            />,
        ));
    };

    handleTabSelect = (item) => {
        this.setState({ selectedTabItem: item });
    };

    renderTabContent = () => {
        const { userDetail } = this.props;
        const { selectedTabItem } = this.state;

        switch (selectedTabItem.id) {
            case UserDetail.TAB_FUNDS:
                return <FundsPermissionPanel
                    userId={userDetail.id}
                    onAddPermission={perm => WebApi.addUserPermission(userDetail.id, perm)}
                    onDeletePermission={perm => WebApi.deleteUserPermission(userDetail.id, perm)}
                    onDeleteFundPermission={fundId => WebApi.deleteUserFundPermission(userDetail.id, fundId)}
                    onSelectItem={(item, index) => {
                        this.selectedFund = { index, id: item.id };
                    }}
                    selectedPermission={this.selectedFund}
                />;
            case UserDetail.TAB_SCOPES:
                return <ScopesPermissionPanel
                    userId={userDetail.id}
                    onAddPermission={perm => WebApi.addUserPermission(userDetail.id, perm)}
                    onDeletePermission={perm => WebApi.deleteUserPermission(userDetail.id, perm)}
                    onDeleteScopePermission={scopeId => WebApi.deleteUserScopePermission(userDetail.id, scopeId)}
                    onSelectItem={(item, index) => {
                        this.selectedScope = { index, id: item.id };
                    }}
                    selectedPermission={this.selectedScope}
                />;
            case UserDetail.TAB_ADVANCED:
                return <AdvancedPermissionPanel
                    userId={userDetail.id}
                    onAddPermission={perm => WebApi.addUserPermission(userDetail.id, perm)}
                    onDeletePermission={perm => WebApi.deleteUserPermission(userDetail.id, perm)}
                />;
            default:
                return null;
        }
    };

    render() {
        const { userDetail, userCount } = this.props;
        const { selectedTabItem } = this.state;

        if (userDetail.id === null) {
            return <div className='user-detail-container'>
                <div className="unselected-msg">
                    <div
                        className="title">{userCount > 0 ? i18n('admin.user.noSelection.title') : i18n('admin.user.emptyList.title')}</div>
                    <div
                        className="message">{userCount > 0 ? i18n('admin.user.noSelection.message') : i18n('admin.user.emptyList.message')}</div>
                </div>
            </div>;
        }

        return <div className="detail-container">
            <StoreHorizontalLoader store={userDetail}/>
            {userDetail.fetched && <AdminRightsContainer
                header={<DetailHeader
                    icon={<Icon glyph="fa-user"/>}
                    title={userDetail.party.accessPoint.record}
                    rowFlagColor={userDetail.active ? 'success' : 'warning'}
                    flagLeft={userDetail.active ? i18n('admin.user.title.active') : i18n('admin.user.title.nonactive')}
                    subtitle={userDetail.username}
                />}
                left={<AddRemoveList
                    label={<h4>{i18n('admin.user.title.groups')}</h4>}
                    addInLabel
                    items={userDetail.groups}
                    onAdd={this.handleAddGroups}
                    onRemove={this.handleRemoveGroup}
                    addTitle="admin.user.group.action.add"
                    removeTitle="admin.user.group.action.delete"
                    renderItem={renderGroupItem}
                />}
            >
                <div className="permissions-container">
                    <h4>{i18n('admin.user.title.permissions')}</h4>
                    <Tabs.Container>
                        <Tabs.Tabs items={UserDetail.tabItems}
                                   activeItem={selectedTabItem}
                                   onSelect={this.handleTabSelect}
                        />
                        <Tabs.Content>
                            {this.renderTabContent()}
                        </Tabs.Content>
                    </Tabs.Container>
                </div>
            </AdminRightsContainer>}
        </div>;
    }
}

export default connect()(UserDetail);

