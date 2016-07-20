/**
 * Stránka pro správu uživatelů.
 */
require('./AdminUserPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {PageLayout} from 'pages/index.jsx';
import {FormControl, Button} from 'react-bootstrap';
import {i18n, UserDetail, Search, ListBox, AbstractReactComponent, RibbonGroup, Ribbon, Icon, AddUserForm, PasswordForm} from 'components/index.jsx';
import {usersFetchIfNeeded, usersUserDetailFetchIfNeeded, usersSelectUser, usersSearch, userCreate, adminPasswordChange} from 'actions/admin/user.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {WebApi} from 'actions/index.jsx'
import {requestScopesIfNeeded} from 'actions/refTables/scopesData.jsx'
import {renderUserItem} from "components/admin/adminRenderUtils.jsx"

const AdminUserPage = class AdminUserPage extends AbstractReactComponent{
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

    handleCreateUserForm() {
        this.dispatch(modalDialogShow(this, i18n('admin.user.add.title'), <AddUserForm onSubmitForm={this.handleCreateUser} />))
    }

    handleCreateUser(data) {
        userCreate(data.username, data.password, data.party.partyId).then(response => {
            this.dispatch(addToastrSuccess(i18n('admin.user.add.success')));
            this.dispatch(modalDialogHide());
            this.dispatch(usersSelectUser(response.id))
        }).catch(e => {
            console.error(e);
        });
    }

    handleChangeUserPasswordForm() {
        this.dispatch(modalDialogShow(this, i18n('admin.user.passwordChange.title'), <PasswordForm admin={true} onSubmitForm={this.handleChangeUserPassword} />))
    }

    handleChangeUserPassword(data) {
        const {user: {userDetail: {id}}} = this.props;
        adminPasswordChange(id, data.password).then(response => {
            this.dispatch(addToastrSuccess(i18n('admin.user.passwordChange.success')));
            this.dispatch(modalDialogHide())
        });
    }

    handleChangeUserActive() {
        if(confirm(i18n('admin.user.changeActive.confirm'))) {
            const {user} = this.props;
            WebApi.changeActive(user.userDetail.id, !user.userDetail.active);
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
                <ListBox
                    className='user-listbox'
                    ref='userList'
                    items={user.users}
                    activeIndex={activeIndex}
                    renderItemContent={renderUserItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />
            </div>
        )

        const centerPanel = (
            <UserDetail
                userDetail={user.userDetail}
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

module.exports = connect(mapStateToProps)(AdminUserPage);
