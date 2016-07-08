/**
 * Stránka pro správu uživatelů.
 */
require ('./AdminGroupPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Ribbon} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import {i18n, GroupDetail, Search, ListBox, AbstractReactComponent} from 'components/index.jsx';
import {groupsFetchIfNeeded, groupsGroupDetailFetchIfNeeded, groupsSelectGroup, groupsSearch} from 'actions/admin/group.jsx'
import {indexById} from 'stores/app/utils.jsx'

var AdminGroupPage = class AdminGroupPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.bindMethods(
            'renderListItem',
            'handleSearch',
            'handleSearchClear',
            'handleSelect',
        );
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(groupsFetchIfNeeded())
        this.dispatch(groupsGroupDetailFetchIfNeeded())
    }

    componentDidMount() {
        this.dispatch(groupsFetchIfNeeded())
        this.dispatch(groupsGroupDetailFetchIfNeeded())
    }

    handleSelect(item) {
        this.dispatch(groupsSelectGroup(item.id))
    }    
    
    handleSearch(filterText) {
        this.dispatch(groupsSearch(filterText))
    }

    handleSearchClear() {
        this.dispatch(groupsSearch(''))
    }

    buildRibbon() {
        return (
            <Ribbon admin {...this.props} />
        )
    }

    renderListItem(item) {
        return (
            <div>
                <div className='name'>{item.name}</div>
            </div>
        )
    }

    render() {
        const {splitter, group} = this.props;

        var activeIndex
        if (group.groupDetail.id !== null) {
            activeIndex = indexById(group.groups, group.groupDetail.id)
        }

        var leftPanel = (
            <div className="group-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    value={group.filterText}
                />
                <ListBox
                    className='group-listbox'
                    ref='groupList'
                    items={group.groups}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />
            </div>
        )

        var centerPanel = (
            <GroupDetail
                groupDetail={group.groupDetail}
            />
        )

        return (
            <PageLayout
                splitter={splitter}
                className='admin-group-page'
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
        group: adminRegion.group
    }
}

module.exports = connect(mapStateToProps)(AdminGroupPage);
