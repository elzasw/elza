/**
 * Stránka pro správu uživatelů.
 */
import './AdminFundPage.scss';

import React from 'react';
import {connect} from 'react-redux';
import {Ribbon} from 'components/index.jsx';
import PageLayout from '../shared/layout/PageLayout';
import {AbstractReactComponent, i18n, ListBox, RibbonGroup, Search, StoreHorizontalLoader} from 'components/shared';
import {indexById} from 'stores/app/utils.jsx';
import {fundsFetchIfNeeded, fundsFilter, selectFund, fundsPage, ADMIN_FUNDS_PAGE_SIZE} from './../../actions/admin/fund';
import {renderFundItem} from '../../components/admin/adminRenderUtils';
import storeFromArea from '../../shared/utils/storeFromArea';
import * as fund from '../../actions/admin/fund';
import FundDetail from '../../components/admin/FundDetail';
import FundsPermissionPanel from '../../components/admin/FundsPermissionPanel';
import ListPager from '../../components/shared/listPager/ListPager';

class AdminFundPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.state = {
            fundRows: this.getFundRows(props),
        };
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.props.dispatch(fundsFetchIfNeeded());
        if (nextProps.funds.rows !== this.props.funds.rows) {
            this.setState({fundRows: this.getFundRows(nextProps)});
        }
    }

    getFundRows = props => {
        const {funds} = props;
        if (!funds.fetched) {
            return [];
        }

        const rows = [
            {id: FundsPermissionPanel.ALL_ID, name: i18n('admin.perms.tabs.funds.items.fundAll')},
            ...funds.rows,
        ];

        return rows;
    };

    componentDidMount() {
        this.props.dispatch(fundsPage(1, 2))
        this.props.dispatch(fundsFetchIfNeeded());
    }

    buildRibbon() {
        const altActions = [];
        const itemActions = [];

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt-actions" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item-actions" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return <Ribbon admin {...this.props} altSection={altSection} itemSection={itemSection} />;
    }

    renderFundDetail = () => {
        return <FundDetail />;
    };


    render() {
        const {splitter, funds, fund} = this.props;
        const {fundRows} = this.state;
        const {page, pageSize} = funds.filter;

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
                    value={funds.filter.text || ''}
                />
                <StoreHorizontalLoader store={funds} />
                {funds.fetched && (
                    <ListBox
                        key="funds"
                        className="fund-listbox"
                        ref="fundList"
                        items={fundRows}
                        activeIndex={activeIndex}
                        renderItemContent={renderFundItem}
                        onFocus={this.handleSelect}
                        onSelect={this.handleSelect}
                    />
                )}
                {funds.count > pageSize && (
                    <ListPager
                        prev={this.handleFilterPrev}
                        next={this.handleFilterNext}
                        from={(page-1)*pageSize}
                        maxSize={pageSize}
                        totalCount={funds.count}
                    />
                )}
            </div>
        );

        let centerPanel;
        if (fund.id !== null) {
            centerPanel = this.renderFundDetail();
        }

        return (
            <PageLayout
                splitter={splitter}
                className="admin-fund-page"
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
            />
        );
    }

    handleSelect = item => {
        this.props.dispatch(selectFund(item.id));
    };

    handleSearch = filterText => {
        this.props.dispatch(fundsFilter(filterText));
    };

    handleSearchClear = () => {
        this.props.dispatch(fundsFilter(''));
    };

    handleFilterPrev = () => {
        const {funds} = this.props;
        const nextPage = funds.filter.page - 1;
        console.log("prev", nextPage);
        if(nextPage > 0){
            this.props.dispatch(fundsPage(nextPage, ADMIN_FUNDS_PAGE_SIZE))
        }
    }

    handleFilterNext = () => {
        const {funds} = this.props;
        const nextPage = funds.filter.page + 1;
        if(funds.count >= nextPage*funds.filter.pageSize){
            this.props.dispatch(fundsPage(this.props.funds.filter.page + 1, ADMIN_FUNDS_PAGE_SIZE))
        }
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
    };
}

export default connect(mapStateToProps)(AdminFundPage);
