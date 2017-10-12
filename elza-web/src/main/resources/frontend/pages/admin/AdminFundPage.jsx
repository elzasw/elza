/**
 * Stránka pro správu uživatelů.
 */
import './AdminFundPage.less';

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {GroupDetail, AddGroupForm, Ribbon} from 'components/index.jsx';
import PageLayout from "../shared/layout/PageLayout";
import {i18n, Search, ListBox, AbstractReactComponent, Icon, RibbonGroup, StoreHorizontalLoader} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {renderGroupItem} from "components/admin/adminRenderUtils.jsx"
import {fundsFilter, fundsFetchIfNeeded} from "./../../actions/admin/fund";
import {renderFundItem} from "../../components/admin/adminRenderUtils";

class AdminFundPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fundsFetchIfNeeded())
        // this.dispatch(groupsGroupDetailFetchIfNeeded())
    }

    componentDidMount() {
        this.dispatch(fundsFetchIfNeeded())
        // this.dispatch(groupsGroupDetailFetchIfNeeded())
    }

    handleSelect = (item) => {
        // this.dispatch(groupsSelectGroup(item.id))
    };

    handleSearch = (filterText) => {
        this.dispatch(fundsFilter(filterText))
    };

    handleSearchClear = () => {
        this.dispatch(fundsFilter(""));
    };

    buildRibbon() {
        const altActions = [];
        const itemActions = [];

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

    render() {
        const {splitter, funds} = this.props;

        let activeIndex
        // if (group.groupDetail.id !== null) {
        //     activeIndex = indexById(group.groups, group.groupDetail.id)
        // }

        const leftPanel = (
            <div className="fund-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    value={funds.filter.text || ""}
                />
                <StoreHorizontalLoader store={funds}/>
                {funds.fetched && <ListBox
                    className='fund-listbox'
                    ref='fundList'
                    items={funds.rows}
                    activeIndex={activeIndex}
                    renderItemContent={renderFundItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />}
            </div>
        )

        // const centerPanel = (
        //     <GroupDetail
        //         groupDetail={group.groupDetail}
        //         groupCount={group.groups ? group.groups.length : 0}
        //     />
        // )
        let centerPanel;

        return (
            <PageLayout
                splitter={splitter}
                className='admin-fund-page'
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
        funds: adminRegion.funds
    }
}

export default connect(mapStateToProps)(AdminFundPage);
