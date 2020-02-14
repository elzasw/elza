// ---
import './AdminUserPage.scss';

import React from 'react';
import {connect} from 'react-redux'
import PageLayout from "../shared/layout/PageLayout";
import {Button, FormControl} from 'react-bootstrap';
import {
    AbstractReactComponent,
    i18n,
    Icon,
    ListBox,
    RibbonGroup,
    Search,
    StoreHorizontalLoader
} from 'components/shared';
import {AddUserForm, Ribbon, UserDetail} from 'components/index.jsx';
import {
    adminPasswordChange,
    adminUserChangeActive,
    userCreate,
    usersFetchIfNeeded,
    usersSearch,
    usersSelectUser,
    userUpdate
} from 'actions/admin/user.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx';
import {renderUserItem} from 'components/admin/adminRenderUtils.jsx';
import {partyAdd} from 'actions/party/party.jsx'
import PasswordForm from "../../components/admin/PasswordForm";

/**
 * Stránka pro správu uživatelů.
 */
class AdminUserPage extends AbstractReactComponent{
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.bindMethods(
            'handleSearch',
            'handleSearchClear',
            'handleFilterStateChange',
            'handleSelect',
            'handleCreateUserForm',
            'handleCreateUser',
            'handleChangeUserActive',
            'handleChangeUserPasswordForm',
            'handleChangeUserPassword',
            'handleChangeUsernameForm',
            'handleUpdateUser',
            'handlePartyAdd',
        );
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(requestScopesIfNeeded());
        this.props.dispatch(usersFetchIfNeeded())
    }

    componentDidMount() {
        this.props.dispatch(requestScopesIfNeeded());
        this.props.dispatch(usersFetchIfNeeded())
    }

    handleSelect(item) {
        this.props.dispatch(usersSelectUser(item.id))
    }

    handleSearch(filterText) {
        const {user} = this.props;
        this.props.dispatch(usersSearch(filterText, user.filterState))
    }

    handleSearchClear() {
        const {user} = this.props;
        this.props.dispatch(usersSearch('', user.filterState))
    }

    handlePartyAdd(partyTypeId, callback) {
        this.props.dispatch(partyAdd(partyTypeId, -1, callback));
    };

    handleCreateUserForm() {
        this.props.dispatch(modalDialogShow(this, i18n('admin.user.add.title'), <AddUserForm create onSubmitForm={this.handleCreateUser} onCreateParty={this.handlePartyAdd} />))
    }

    handleCreateUser(data) {
        return this.props.dispatch(userCreate(data.username, data.valuesMap, data.party.id));
    }

    handleChangeUserPasswordForm() {
        this.props.dispatch(modalDialogShow(this, i18n('admin.user.passwordChange.title'), <PasswordForm admin={true} onSubmitForm={this.handleChangeUserPassword} />))
    }

    handleChangeUserPassword(data) {
        const {user: {userDetail: {id}}} = this.props;
        return this.props.dispatch(adminPasswordChange(id, data.password));
    }

    handleUpdateUser(data) {
        const {user: {userDetail: {id}}} = this.props;
        return this.props.dispatch(userUpdate(id, data.username, data.valuesMap));
    }

    handleChangeUsernameForm() {
        const {user} = this.props;

        const index = indexById(user.users, user.userDetail.id);
        if (index !== null) {
            const data = user.users[index];
            const authTypes = data.authTypes;
            const initData = {
                username: data.username,
                passwordCheckbox: authTypes.indexOf('PASSWORD') >= 0,
                shibbolethCheckbox: authTypes.indexOf('SHIBBOLETH') >= 0
            };
            this.props.dispatch(modalDialogShow(this, i18n('admin.user.update.title'), <AddUserForm initialValues={initData} onSubmitForm={this.handleUpdateUser} />))
        }
    }

    handleChangeUserActive() {
        if(confirm(i18n('admin.user.changeActive.confirm'))) {
            const {user} = this.props;
            this.props.dispatch(adminUserChangeActive(user.userDetail.id, !user.userDetail.active));
        }
    }

    buildRibbon() {
        const {user} = this.props;

        const altActions = [];
        const itemActions = [];
        altActions.push(
            <Button key="add-user" onClick={this.handleCreateUserForm}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.admin.user.add')}</span></div></Button>
        );

        const userDetail = user.userDetail;

        if (userDetail.id != null) {
            itemActions.push(
                <Button key="change-active-user" onClick={this.handleChangeUserActive}><Icon glyph={user.userDetail.active ? 'fa-ban' : 'fa-check'} /><div><span className="btnText">{user.userDetail.active ? i18n('ribbon.action.admin.user.deactivate') : i18n('ribbon.action.admin.user.activate')}</span></div></Button>
            );
            const userData = user.users[indexById(user.users, userDetail.id)];
            if (userData && userData.authTypes.indexOf('PASSWORD') >= 0) {
                itemActions.push(
                    <Button key="password-change-user" onClick={this.handleChangeUserPasswordForm}><Icon glyph='fa-key' /><div><span className="btnText">{i18n('ribbon.action.admin.user.passwordChange')}</span></div></Button>
                );
            }
            itemActions.push(
                <Button key="username-change-user" onClick={this.handleChangeUsernameForm}><Icon glyph='fa-edit' /><div><span className="btnText">{i18n('ribbon.action.admin.user.edit')}</span></div></Button>
            )
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key='alt-actions' className="small">{altActions}</RibbonGroup>
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key='item-actions' className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon admin {...this.props}  altSection={altSection} itemSection={itemSection} />
        )
    }

    handleFilterStateChange(e) {
        const {user} = this.props;
        this.props.dispatch(usersSearch(user.filterText, {type: e.target.value}))
    }

    render() {
        const {splitter, user} = this.props;
        let activeIndex;
        if (user.userDetail.id !== null) {
            activeIndex = indexById(user.users, user.userDetail.id)
        }

        const leftPanel = (
            <div className="user-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    value={user.filterText}
                />
                <FormControl componentClass="select" value={user.filterState.type} onChange={this.handleFilterStateChange}>
                    <option value="all">{i18n("admin.user.filter.all")}</option>
                    <option value="onlyActive">{i18n("admin.user.filter.onlyActive")}</option>
                </FormControl>
                <StoreHorizontalLoader store={user}/>
                {user.fetched && <ListBox
                    className='user-listbox'
                    ref='userList'
                    items={user.users}
                    activeIndex={activeIndex}
                    renderItemContent={renderUserItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />}
            </div>
        );

        let centerPanel;
        if (user.userDetail.id) {
            centerPanel = (
                <UserDetail
                    userDetail={user.userDetail}
                    userCount={user.users.length}
                />
            )
        }

        return (
            <PageLayout
                splitter={splitter}
                className='admin-user-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
            />
        )
    }
}

/**
 * Namapování state do properties.
 */
function mapStateToProps(state) {
    const {splitter, adminRegion} = state;

    return {
        splitter,
        user: adminRegion.user
    }
}

export default connect(mapStateToProps)(AdminUserPage);
