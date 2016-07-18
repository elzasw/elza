/**
 * Stránka pro správu uživatelů.
 */
require('./AdminGroupPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {Ribbon} from 'components/index.jsx';
import {PageLayout} from 'pages/index.jsx';
import {i18n, GroupDetail, Search, ListBox, AbstractReactComponent, AddGroupForm, Icon, RibbonGroup, Loading} from 'components/index.jsx';
import {groupsFetchIfNeeded, groupsGroupDetailFetchIfNeeded, groupsSelectGroup, groupsSearch, groupCreate, groupDelete} from 'actions/admin/group.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {WebApi} from 'actions/index.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'

const AdminGroupPage = class AdminGroupPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.bindMethods(
            'renderListItem',
            'handleSearch',
            'handleSearchClear',
            'handleSelect',
            'handleCreateGroupForm',
            'handleCreateGroup',
            'handleDeleteGroup',
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
        const {group: {groupDetail}} = this.props;
        const altActions = [];
        const itemActions = [];

        altActions.push(
            <Button key="add-group" onClick={this.handleCreateGroupForm}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.admin.group.add')}</span></div></Button>
        );

        if (groupDetail.id !== null) {
            itemActions.push(
                <Button key="delete-group" onClick={this.handleDeleteGroup}><Icon glyph="fa-trash" /><div><span className="btnText">{i18n('ribbon.action.admin.group.delete')}</span></div></Button>
            );
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

    handleDeleteGroup() {
        const {group:{groupDetail:{id}}} = this.props;
        groupDelete(id).then(response => {
            this.dispatch(addToastrSuccess(i18n('admin.group.delete.success')));
        });
    }

    handleCreateGroupForm() {
        this.dispatch(modalDialogShow(this, i18n('admin.group.add.title'), <AddGroupForm onSubmitForm={this.handleCreateGroup} />))
    }

    handleCreateGroup(data) {
        groupCreate(data.name, data.code).then(response => {
            this.dispatch(addToastrSuccess(i18n('admin.group.add.success')));
            this.dispatch(modalDialogHide());
            this.dispatch(groupsSelectGroup(response.id));
        }).catch(e => {
            console.error(e);
        });
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

        let activeIndex
        if (group.groupDetail.id !== null) {
            activeIndex = indexById(group.groups, group.groupDetail.id)
        }

        const leftPanel = (
            !group.fetching && group.groups ? <div className="group-list-container">
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
            </div> : <Loading />
        )

        const centerPanel = (
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
