// --
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, AddRemoveList, Icon, StoreHorizontalLoader, Tabs} from 'components/shared';
import { FormattedMessage, defineMessages, injectIntl } from 'react-intl';

// Id jsou převzatá z legacy katalogu beze změny.
const messages = defineMessages({
    tabsFunds: { id: 'admin.perms.tabs.funds', defaultMessage: 'Archivní soubory' },
    tabsScopes: { id: 'admin.perms.tabs.scopes', defaultMessage: 'Oblasti entit' },
    tabsAdvanced: { id: 'admin.perms.tabs.advanced', defaultMessage: 'Pokročilé' },
    tabsAccessKeys: { id: 'admin.perms.tabs.accessKeys', defaultMessage: 'Přístupové klíče API' },
    groupAddTitle: { id: 'admin.user.group.add.title', defaultMessage: 'Zařazení uživatele do skupin' },
    noSelectionTitle: { id: 'admin.user.noSelection.title', defaultMessage: 'Není vybrán uživatel' },
    noSelectionMessage: {
        id: 'admin.user.noSelection.message',
        defaultMessage: 'Prosím vyberte uživatele ze seznamu nebo vytvořte nového',
    },
    emptyListTitle: { id: 'admin.user.emptyList.title', defaultMessage: 'Žádní uživatelé' },
    emptyListMessage: {
        id: 'admin.user.emptyList.message',
        defaultMessage: 'V systému nejsou zadáni žádní uživatelé',
    },
    titleActive: { id: 'admin.user.title.active', defaultMessage: 'Aktivní uživatel' },
    titleNonactive: { id: 'admin.user.title.nonactive', defaultMessage: 'Neaktivní uživatel' },
    titleGroups: { id: 'admin.user.title.groups', defaultMessage: 'Členství ve skupinách' },
    groupAdd: {
        id: 'admin.user.group.action.add',
        defaultMessage: 'Zařadit uživatele do skupin',
    },
    groupRemove: {
        id: 'admin.user.group.action.delete',
        defaultMessage: 'Odebrat zařazení uživatele do skupiny',
    },
    titlePermissions: { id: 'admin.user.title.permissions', defaultMessage: 'Oprávnění uživatele' },
});
import {getIdsList} from 'stores/app/utils';
import {joinGroups, leaveGroup, usersUserDetailFetchIfNeeded} from 'actions/admin/user';
import {modalDialogShow} from 'actions/global/modalDialog';
import {renderGroupItem} from 'components/admin/adminRenderUtils';
import './UserDetail.scss';
import FundsPermissionPanel from './FundsPermissionPanel';
import ScopesPermissionPanel from './ScopesPermissionPanel';
import AdvancedPermissionPanel from './AdvancedPermissionPanel';
import AccessKeysPanel from './AccessKeysPanel';
import SelectItemsForm from './SelectItemsForm';
import GroupField from './GroupField';
import AdminRightsContainer from './AdminRightsContainer';
import {WebApi} from '../../actions/WebApi';
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
    static TAB_ACCESS_KEYS = 3;

    /** Skládá se až při renderu, aby popisky reagovaly na přepnutí jazyka. */
    getTabItems = () => [
        {id: UserDetail.TAB_FUNDS, title: this.props.intl.formatMessage(messages.tabsFunds)},
        {id: UserDetail.TAB_SCOPES, title: this.props.intl.formatMessage(messages.tabsScopes)},
        {id: UserDetail.TAB_ADVANCED, title: this.props.intl.formatMessage(messages.tabsAdvanced)},
        {id: UserDetail.TAB_ACCESS_KEYS, title: this.props.intl.formatMessage(messages.tabsAccessKeys)},
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
            selectedTabItem: {id: UserDetail.TAB_FUNDS},
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
        const {userDetail} = this.props;
        console.log('remove group', group);
        this.props.dispatch(leaveGroup(userDetail.id, group.id));
    };

    handleAddGroups = () => {
        const {userDetail} = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                this.props.intl.formatMessage(messages.groupAddTitle),
                <SelectItemsForm
                    onSubmitForm={groups => {
                        this.props.dispatch(joinGroups(userDetail.id, getIdsList(groups)));
                    }}
                    fieldComponent={GroupField}
                    renderItem={renderGroupItem}
                />,
            ),
        );
    };

    handleTabSelect = item => {
        this.setState({selectedTabItem: {id: Number(item)}});
    };

    renderTabContent = () => {
        const {userDetail} = this.props;
        const {selectedTabItem} = this.state;
        console.log('selectedTabItem', selectedTabItem);
        switch (selectedTabItem.id) {
            case UserDetail.TAB_FUNDS:
                return (
                    <FundsPermissionPanel
                        userId={userDetail.id}
                        onAddPermission={perm => WebApi.addUserPermission(userDetail.id, perm)}
                        onDeletePermission={perm => WebApi.deleteUserPermission(userDetail.id, perm)}
                        onDeleteFundPermission={fundId => WebApi.deleteUserFundPermission(userDetail.id, fundId)}
                        onSelectItem={(item, index) => {
                            this.selectedFund = {index, id: item.id};
                        }}
                        selectedPermission={this.selectedFund}
                    />
                );
            case UserDetail.TAB_SCOPES:
                return (
                    <ScopesPermissionPanel
                        userId={userDetail.id}
                        onAddPermission={perm => WebApi.addUserPermission(userDetail.id, perm)}
                        onDeletePermission={perm => WebApi.deleteUserPermission(userDetail.id, perm)}
                        onDeleteScopePermission={scopeId => WebApi.deleteUserScopePermission(userDetail.id, scopeId)}
                        onSelectItem={(item, index) => {
                            this.selectedScope = {index, id: item.id};
                        }}
                        selectedPermission={this.selectedScope}
                    />
                );
            case UserDetail.TAB_ADVANCED:
                return (
                    <AdvancedPermissionPanel
                        userId={userDetail.id}
                        onAddPermission={perm => WebApi.addUserPermission(userDetail.id, perm)}
                        onDeletePermission={perm => WebApi.deleteUserPermission(userDetail.id, perm)}
                    />
                );
            case UserDetail.TAB_ACCESS_KEYS:
                return <AccessKeysPanel userId={userDetail.id} />;
            default:
                return null;
        }
    };

    render() {
        const {userDetail, userCount} = this.props;
        const {selectedTabItem} = this.state;

        if (userDetail.id === null) {
            return (
                <div className="user-detail-container">
                    <div className="unselected-msg">
                        <div className="title">
                            <FormattedMessage {...(userCount > 0 ? messages.noSelectionTitle : messages.emptyListTitle)} />
                        </div>
                        <div className="message">
                            {userCount > 0
                                ? <FormattedMessage {...messages.noSelectionMessage} />
                                : <FormattedMessage {...messages.emptyListMessage} />}
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div className="detail-container">
                <StoreHorizontalLoader store={userDetail}/>
                {userDetail.fetched && (
                    <AdminRightsContainer
                        header={
                            <DetailHeader
                                icon={<Icon glyph="fa-user"/>}
                                title={userDetail.accessPoint.name}
                                rowFlagColor={userDetail.active ? 'success' : 'warning'}
                                flagLeft={
                                    userDetail.active
                                        ? <FormattedMessage {...messages.titleActive} />
                                        : <FormattedMessage {...messages.titleNonactive} />
                                }
                                subtitle={userDetail.username}
                            />
                        }
                        left={
                            <AddRemoveList
                                label={<h4><FormattedMessage {...messages.titleGroups} /></h4>}
                                addInLabel
                                items={userDetail.groups}
                                onAdd={this.handleAddGroups}
                                onRemove={this.handleRemoveGroup}
                                addTitle={messages.groupAdd}
                                removeTitle={messages.groupRemove}
                                renderItem={renderGroupItem}
                                className="no-hover alternating-rows"
                            />
                        }
                    >
                        <div className="permissions-container">
                            <h4><FormattedMessage {...messages.titlePermissions} /></h4>
                            <Tabs.Container>
                                <Tabs.Tabs
                                    items={this.getTabItems()}
                                    activeItem={selectedTabItem}
                                    onSelect={this.handleTabSelect}
                                    asTabs
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

export default connect()(injectIntl(UserDetail));
