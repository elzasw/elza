/**
 * Stránka pro správu uživatelů.
 */
import './AdminUserPage.less';

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import PageLayout from "../shared/layout/PageLayout";
import {FormControl, Button} from 'react-bootstrap';
import {i18n, Search, ListBox, StoreHorizontalLoader, AbstractReactComponent, RibbonGroup, Icon} from 'components/shared';
import {UserDetail, Ribbon, AddUserForm, PasswordForm} from 'components/index.jsx';
import {usersFetchIfNeeded,
    usersUserDetailFetchIfNeeded,
    usersSelectUser,
    usersSearch,
    userCreate,
    userUpdate,
    adminPasswordChange,
    adminUserChangeActive} from 'actions/admin/user.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx';
import {renderUserItem} from 'components/admin/adminRenderUtils.jsx';
import {partyAdd} from 'actions/party/party.jsx'

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

    componentWillReceiveProps(nextProps) {
        this.dispatch(requestScopesIfNeeded())
        this.dispatch(usersFetchIfNeeded())
    }

    componentDidMount() {
        this.dispatch(requestScopesIfNeeded())
        this.dispatch(usersFetchIfNeeded())
    }

    handleSelect(item) {
        this.dispatch(usersSelectUser(item.id))
    }

    handleSearch(filterText) {
        const {user} = this.props;
        this.dispatch(usersSearch(filterText, user.filterState))
    }

    handleSearchClear() {
        const {user} = this.props;
        this.dispatch(usersSearch('', user.filterState))
    }

    handlePartyAdd(partyTypeId, callback) {
        this.dispatch(partyAdd(partyTypeId, -1, callback));
    };

    handleCreateUserForm() {
        this.dispatch(modalDialogShow(this, i18n('admin.user.add.title'), <AddUserForm create onSubmitForm={this.handleCreateUser} onCreateParty={this.handlePartyAdd} />))
    }

    handleCreateUser(data) {
        return this.dispatch(userCreate(data.username, data.password, data.party.id));
    }

    handleChangeUserPasswordForm() {
        this.dispatch(modalDialogShow(this, i18n('admin.user.passwordChange.title'), <PasswordForm admin={true} onSubmitForm={this.handleChangeUserPassword} />))
    }

    handleChangeUserPassword(data) {
        const {user: {userDetail: {id}}} = this.props;
        return this.dispatch(adminPasswordChange(id, data.password));
    }

    handleUpdateUser(data) {
        const {user: {userDetail: {id}}} = this.props;
        return this.dispatch(userUpdate(id, data.username, data.password));
    }

    handleChangeUsernameForm() {
        const {user} = this.props;
        this.dispatch(modalDialogShow(this, i18n('admin.user.update.title'), <AddUserForm initData={user.userDetail} onSubmitForm={this.handleUpdateUser} />))
    }

    handleChangeUserActive() {
        if(confirm(i18n('admin.user.changeActive.confirm'))) {
            const {user} = this.props;
            this.dispatch(adminUserChangeActive(user.userDetail.id, !user.userDetail.active));
        }
    }

    buildRibbon() {
        const {user} = this.props;

        const altActions = [];
        const itemActions = [];
        altActions.push(
            <Button key="add-user" onClick={this.handleCreateUserForm}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.admin.user.add')}</span></div></Button>
        );

        if (user.userDetail.id !== null) {
            itemActions.push(
                <Button key="change-active-user" onClick={this.handleChangeUserActive}><Icon glyph={user.userDetail.active ? 'fa-ban' : 'fa-check'} /><div><span className="btnText">{user.userDetail.active ? i18n('ribbon.action.admin.user.deactivate') : i18n('ribbon.action.admin.user.activate')}</span></div></Button>
            )
            itemActions.push(
                <Button key="password-change-user" onClick={this.handleChangeUserPasswordForm}><Icon glyph='fa-key' /><div><span className="btnText">{i18n('ribbon.action.admin.user.passwordChange')}</span></div></Button>
            )
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
        this.dispatch(usersSearch(user.filterText, {type: e.target.value}))
    }

    render() {
        const {splitter, user} = this.props;
        let activeIndex
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
        )

        const centerPanel = (
            <UserDetail
                userDetail={user.userDetail}
                userCount={user.users.length}
            />
        )

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
    const {splitter, adminRegion} = state

    return {
        splitter,
        user: adminRegion.user
    }
}

export default connect(mapStateToProps)(AdminUserPage);
