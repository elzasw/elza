/**
 * Stránka pro správu uživatelů.
 */
import './AdminGroupPage.scss';

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {GroupDetail, AddGroupForm, Ribbon} from 'components/index.jsx';
import PageLayout from "../shared/layout/PageLayout";
import {i18n, Search, ListBox, AbstractReactComponent, Icon, RibbonGroup, StoreHorizontalLoader} from 'components/shared';
import {groupsFetchIfNeeded, groupsGroupDetailFetchIfNeeded, groupsSelectGroup, groupsSearch, groupUpdate, groupCreate, groupDelete} from 'actions/admin/group.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {renderGroupItem} from "components/admin/adminRenderUtils.jsx"

const AdminGroupPage = class AdminGroupPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.bindMethods(
            'handleSearch',
            'handleSearchClear',
            'handleSelect',
            'handleCreateGroupForm',
            'handleEditGroupForm',
            'handleCreateGroup',
            'handleUpdateGroup',
            'handleDeleteGroup',
        );
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(groupsFetchIfNeeded())
        this.props.dispatch(groupsGroupDetailFetchIfNeeded())
    }

    componentDidMount() {
        this.props.dispatch(groupsFetchIfNeeded())
        this.props.dispatch(groupsGroupDetailFetchIfNeeded())
    }

    handleSelect(item) {
        this.props.dispatch(groupsSelectGroup(item.id))
    }

    handleSearch(filterText) {
        this.props.dispatch(groupsSearch(filterText))
    }

    handleSearchClear() {
        this.props.dispatch(groupsSearch(''))
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
            itemActions.push(
                <Button key="edit-group" onClick={this.handleEditGroupForm}><Icon glyph="fa-edit" /><div><span className="btnText">{i18n('ribbon.action.admin.group.edit')}</span></div></Button>
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
        this.props.dispatch(groupDelete(id)).then(response => {
            this.props.dispatch(addToastrSuccess(i18n('admin.group.delete.success')));
        });
    }

    handleCreateGroupForm() {
        this.props.dispatch(modalDialogShow(this, i18n('admin.group.add.title'), <AddGroupForm create onSubmitForm={this.handleCreateGroup}/>))
    }

    handleEditGroupForm() {
        const {group} = this.props;
        this.props.dispatch(modalDialogShow(this, i18n('admin.group.edit.title'), <AddGroupForm initData={group.groupDetail} onSubmitForm={this.handleUpdateGroup} />))
    }

    handleCreateGroup(data) {
        return this.props.dispatch(groupCreate(data.name, data.code, data.description));
    }

    handleUpdateGroup(data) {
        const {group:{groupDetail:{id}}} = this.props;
        return this.props.dispatch(groupUpdate(id, data.name, data.description));
    }

    render() {
        const {splitter, group} = this.props;

        let activeIndex
        if (group.groupDetail.id !== null) {
            activeIndex = indexById(group.groups, group.groupDetail.id)
        }

        const leftPanel = (
            <div className="group-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    value={group.filterText || ""}
                />
                <StoreHorizontalLoader store={group}/>
                {group.fetched && <ListBox
                    className='group-listbox'
                    ref='groupList'
                    items={group.groups}
                    activeIndex={activeIndex}
                    renderItemContent={renderGroupItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />}
            </div>
        )

        const centerPanel = (
            <GroupDetail
                groupDetail={group.groupDetail}
                groupCount={group.groups ? group.groups.length : 0}
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

export default connect(mapStateToProps)(AdminGroupPage);
