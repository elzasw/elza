/**
 * Stránka pro správu uživatelů.
 */
require ('./AdminUserPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Ribbon} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import {Input} from 'react-bootstrap';
import {i18n, UserDetail, Search, ListBox, AbstractReactComponent} from 'components/index.jsx';
import {usersFetchIfNeeded, usersUserDetailFetchIfNeeded, usersSelectUser, usersSearch} from 'actions/admin/user.jsx'
import {indexById} from 'stores/app/utils.jsx'

var AdminUserPage = class AdminUserPage extends AbstractReactComponent{
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.bindMethods(
            'renderListItem',
            'handleSearch',
            'handleSearchClear',
            'handleFilterStateChange',
            'handleSelect',
        );
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(usersFetchIfNeeded())
    }

    componentDidMount() {
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

    buildRibbon() {
        return (
            <Ribbon admin {...this.props} />
        )
    }

    handleFilterStateChange(e) {
        const {user} = this.props;
        this.dispatch(usersSearch(user.filterText, {type: e.target.value}))
    }

    renderListItem(item) {
        return (
            <div>
                <div className='name'>{item.party.record.record} ({item.username})</div>
            </div>
        )
    }

    render() {
        const {splitter, user} = this.props;

        var activeIndex
        if (user.userDetail.id !== null) {
            activeIndex = indexById(user.users, user.userDetail.id)
        }

        var leftPanel = (
            <div className="user-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    value={user.filterText}
                />
                <Input type="select" value={user.filterState.type} onChange={this.handleFilterStateChange}>
                    <option value="all">{i18n("admin.user.filter.all")}</option>
                    <option value="onlyActive">{i18n("admin.user.filter.onlyActive")}</option>
                </Input>
                <ListBox
                    className='user-listbox'
                    ref='userList'
                    items={user.users}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />
            </div>
        )

        var centerPanel = (
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
