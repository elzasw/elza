/**
 * Stránka pro správu uživatelů.
 */
require ('./AdminUserPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Ribbon} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import {i18n, Search, ListBox, AbstractReactComponent} from 'components/index.jsx';
import {usersFetchIfNeeded, usersSelectUser, usersSearch} from 'actions/admin/user.jsx'

var AdminUserPage = class AdminUserPage extends AbstractReactComponent{
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.bindMethods(
            'renderListItem',
            'handleSearch',
            'handleSearchClear',
        );
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(usersFetchIfNeeded())
    }

    componentDidMount() {
        this.dispatch(usersFetchIfNeeded())
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

        var leftPanel = (
            <div className="user-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    value={user.filterText}
                />
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
            <div>
                ...users
            </div>
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
