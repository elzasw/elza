// --
import React from 'react';
import {connect} from 'react-redux'
import {Icon, AbstractReactComponent, Tabs, NoFocusButton, AddRemoveList, i18n, StoreHorizontalLoader} from 'components/shared';
import {indexById, getIdsList} from 'stores/app/utils.jsx'
import {dateToString} from 'components/Utils.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {changeUserPermission} from 'actions/admin/permission.jsx'
import {refInstitutionsFetchIfNeeded} from 'actions/refTables/institutions.jsx'
import {refRuleSetFetchIfNeeded} from 'actions/refTables/ruleSet.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {usersUserDetailFetchIfNeeded} from 'actions/admin/user.jsx'
import Permissions from "./Permissions.jsx"
import {joinGroups, leaveGroup} from 'actions/admin/user.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {renderGroupItem} from "components/admin/adminRenderUtils.jsx"
import SelectGroupsForm from './SelectGroupsForm.jsx'

import './UserDetail.less';
import FundsPermissionPanel from "./FundsPermissionPanel";
import ScopesPermissionPanel from "./ScopesPermissionPanel";
import AdvancedPermissionPanel from "./AdvancedPermissionPanel";

/**
 * Detail uživatele s nastavením oprávnění.
 */
class UserDetail extends AbstractReactComponent {
    static PropTypes = {
        userDetail: React.PropTypes.object.isRequired,
        userCount: React.PropTypes.number.isRequired,
    };

    static TAB_FUNDS = 0;
    static TAB_SCOPES = 1;
    static TAB_ADVANCED = 2;

    static tabItems = [
        {id: UserDetail.TAB_FUNDS, title: i18n("admin.user.tabs.funds")},
        {id: UserDetail.TAB_SCOPES, title: i18n("admin.user.tabs.scopes")},
        {id: UserDetail.TAB_ADVANCED, title: i18n("admin.user.tabs.advanced")}
    ];


    constructor(props) {
        super(props);

        this.state = {
            selectedTabItem: UserDetail.tabItems[UserDetail.TAB_FUNDS]
        }
    }

    componentDidMount() {
        this.dispatch(usersUserDetailFetchIfNeeded())
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(usersUserDetailFetchIfNeeded())
    }

    handleSavePermissions = (data) => {
        const {userDetail} = this.props;
        this.dispatch(changeUserPermission(userDetail.id, data));
    };

    handleRemoveGroup = (group, index) => {
        const {userDetail} = this.props;
        this.dispatch(leaveGroup(userDetail.id, group.id));
    };

    handleAddGroups = () => {
        const {userDetail} = this.props;
        this.dispatch(modalDialogShow(this, i18n('admin.user.group.add.title'),
            <SelectGroupsForm onSubmitForm={(groups) => {
                this.dispatch(joinGroups(userDetail.id, getIdsList(groups)));
            }} />
        ))
    };

    handleTabSelect = (item) => {
        this.setState({selectedTabItem: item});
    };

    renderTabContent = () => {
        const {userDetail} = this.props;
        const {selectedTabItem} = this.state;

        switch (selectedTabItem.id) {
            case UserDetail.TAB_FUNDS:
                return <FundsPermissionPanel
                    userId={userDetail.id}
                    groups={userDetail.groups}
                />;
            case UserDetail.TAB_SCOPES:
                return <ScopesPermissionPanel
                    userId={userDetail.id}
                    groups={userDetail.groups}
                />;
            case UserDetail.TAB_ADVANCED:
                return <AdvancedPermissionPanel
                    userId={userDetail.id}
                    groups={userDetail.groups}
                />;
        }
    };

    render() {
        const {userDetail, focus, userCount} = this.props;
        const {selectedTabItem} = this.state;

        if (userDetail.id === null) {
            return(
                <div className='user-detail-container'>
                    <div className="unselected-msg">
                        <div className="title">{userCount > 0 ? i18n('admin.user.noSelection.title') : i18n('admin.user.emptyList.title')}</div>
                        <div className="message">{userCount > 0 ? i18n('admin.user.noSelection.message') : i18n('admin.user.emptyList.message')}</div>
                    </div>
                </div>);
        }

        return <div className="user-detail-container-wrapper">
            <StoreHorizontalLoader store={userDetail}/>
            {userDetail.fetched && <div className='user-detail-container admin-rights-container'>
                <div className="admin-rights-header">
                    <h1>{userDetail.party.record.record}</h1>
                    <div>{i18n("admin.user.label.username")}</div>
                    <div>{userDetail.username}</div>
                </div>
                <div className="admin-rights-content">
                    <AddRemoveList
                        className="left-container"
                        label={<h2>{i18n("admin.user.title.groups")}</h2>}
                        addInLabel
                        items={userDetail.groups}
                        onAdd={this.handleAddGroups}
                        onRemove={this.handleRemoveGroup}
                        addTitle="admin.user.group.action.add"
                        removeTitle="admin.user.group.action.delete"
                        renderItem={renderGroupItem}
                    />
                    <div className="right-container">
                        <h2>{i18n("admin.user.title.permissions")}</h2>
                        <Permissions
                            area="USER"
                            initData={{permissions: userDetail.permissions}}
                            onSave={this.handleSavePermissions}
                            addTitle="admin.user.permission.action.add"
                            removeTitle="admin.user.permission.action.delete"
                        />
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
                </div>
            </div>}
        </div>;
    }
}

export default connect()(UserDetail);

