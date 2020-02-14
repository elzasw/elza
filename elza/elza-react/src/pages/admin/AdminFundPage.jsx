/**
 * Stránka pro správu uživatelů.
 */
import './AdminFundPage.scss';

import React from 'react';
import {connect} from 'react-redux'
import {GroupDetail, AddGroupForm, Ribbon} from 'components/index.jsx';
import PageLayout from "../shared/layout/PageLayout";
import {i18n, Search, ListBox, AbstractReactComponent, Icon, RibbonGroup, StoreHorizontalLoader} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {addToastrSuccess} from 'components/shared/toastr/ToastrActions.jsx'
import {renderGroupItem} from "components/admin/adminRenderUtils.jsx"
import {fundsFilter, fundsFetchIfNeeded, selectFund} from "./../../actions/admin/fund";
import {renderFundItem} from "../../components/admin/adminRenderUtils";
import storeFromArea from "../../shared/utils/storeFromArea";
import * as fund from "../../actions/admin/fund";
import FundDetail from "../../components/admin/FundDetail";
import FundsPermissionPanel from "../../components/admin/FundsPermissionPanel";

class AdminFundPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.state = {
            fundRows: this.getFundRows(props)
        }
    }

    componentWillReceiveProps(nextProps) {
        this.props.dispatch(fundsFetchIfNeeded());
        if (nextProps.funds.rows !== this.props.funds.rows) {
            this.setState({fundRows: this.getFundRows(nextProps)});
        }
    }

    getFundRows = (props) => {
        const {funds} = props;
        if (!funds.fetched) {
            return [];
        }

        const rows = [
            {id: FundsPermissionPanel.ALL_ID, name: i18n("admin.perms.tabs.funds.items.fundAll")},
            ...funds.rows
        ];

        return rows;
    };

    componentDidMount() {
        this.props.dispatch(fundsFetchIfNeeded());
    }

    handleSelect = (item) => {
        this.props.dispatch(selectFund(item.id));
    };

    handleSearch = (filterText) => {
        this.props.dispatch(fundsFilter(filterText));
    };

    handleSearchClear = () => {
        this.props.dispatch(fundsFilter(""));
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

    renderFundDetail = () => {
        return <FundDetail />
    };

    render() {
        const {splitter, funds, fund} = this.props;
        const {fundRows} = this.state;

        let activeIndex;
        if (fund.id !== null) {
            activeIndex = indexById(fundRows, fund.id);
        }

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
                    key="funds"
                    className='fund-listbox'
                    ref='fundList'
                    items={fundRows}
                    activeIndex={activeIndex}
                    renderItemContent={renderFundItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />}
            </div>
        );

        let centerPanel;
        if (fund.id !== null) {
            centerPanel = this.renderFundDetail();
        }

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
    const {splitter} = state;

    return {
        splitter,
        funds: storeFromArea(state, fund.AREA_ADMIN_FUNDS),
        fund: storeFromArea(state, fund.AREA_ADMIN_FUND),
    }
}

export default connect(mapStateToProps)(AdminFundPage);
