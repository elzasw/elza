import './FundPage.less';

/**
 * Stránka archivní soubory.
 */

import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux'
import { Link, IndexLink } from 'react-router';
import { Icon, i18n } from 'components/index.jsx';
import { Splitter, Autocomplete, ListBox, RibbonGroup, ToggleContent, AbstractReactComponent, SearchWithGoto, Utils } from 'components/shared';
import { NodeTabs, FundForm, FundDetail, Ribbon, FindindAidFileTree, ImportForm, ExportForm, FundDetailExt } from 'components'
import { ButtonGroup, Button, Panel } from 'react-bootstrap';
import PageLayout from "../shared/layout/PageLayout";
import { modalDialogShow, modalDialogHide } from 'actions/global/modalDialog.jsx'
import { createFund } from 'actions/arr/fund.jsx'
import { storeLoadData, storeLoad } from 'actions/store/store.jsx'
import { WebApi } from 'actions/index.jsx';
import { canSetFocus, focusWasSet, isFocusFor } from 'actions/global/focus.jsx'
import { indexById } from 'stores/app/utils.jsx'
import { selectFundTab } from 'actions/arr/fund.jsx'
import { routerNavigate } from 'actions/router.jsx'
import { fundsFetchIfNeeded, fundsSelectFund, fundsFundDetailFetchIfNeeded, fundsSearch, fundsFilter, DEFAULT_FUND_LIST_MAX_SIZE } from 'actions/fund/fund.jsx'
import { getFundFromFundAndVersion } from 'components/arr/ArrUtils.jsx'
import { approveFund, deleteFund, deleteFundHistory, exportFund, updateFund } from 'actions/arr/fund.jsx'
import { scopesDirty } from 'actions/refTables/scopesData.jsx'
import * as perms from 'actions/user/Permission.jsx';
import { globalFundTreeInvalidate } from "../../actions/arr/globalFundTree";
import SearchFundsForm from "../../components/arr/SearchFundsForm";
import IssueLists from "../../components/arr/IssueLists";
import ListPager from "../../components/shared/listPager/ListPager";

class FundPage extends AbstractReactComponent {
    static PropTypes = {
        maxSize: PropTypes.number
    };

    static defaultProps = {
        maxSize: DEFAULT_FUND_LIST_MAX_SIZE
    };

    constructor(props) {
        super(props);

        this.bindMethods(
            'handleAddFund',
            'handleImport',
            'handleExportDialog',
            'renderListItem',
            'handleSelect',
            'handleSearch',
            'handleSearchClear',
            'handleApproveFundVersion',
            'handleEditFundVersion',
            'handleCallEditFundVersion',
            'handleDeleteFund',
            'handleDeleteFundHistory',
            'handleRuleSetUpdateFundVersion'
        );

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    componentWillReceiveProps() {
        this.dispatch(fundsFetchIfNeeded());
        this.dispatch(fundsFundDetailFetchIfNeeded());
    }

    componentDidMount() {
        this.dispatch(fundsFetchIfNeeded());
    }

    handleAddFund() {
        const { userDetail } = this.props;
        let initData = {};
        if (!userDetail.hasOne(perms.ADMIN, perms.FUND_ADMIN)) {
            initData.fundAdmins = [{ id: "default", user: userDetail }];
        }
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.add'),
            <FundForm
                create
                initData={initData}
                onSubmitForm={(data) => { return this.dispatch(createFund(data)) }}
            />));
    }

    handleImport() {
        this.dispatch(
            modalDialogShow(this,
                i18n('import.title.fund'),
                <ImportForm fund={true} />
            )
        );
    }

    handleExportDialog() {
        const { fundRegion: { fundDetail } } = this.props;
        this.dispatch(
            modalDialogShow(this,
                i18n('export.title.fund'),
                <ExportForm fund={true} onSubmitForm={data => {
                    return this.dispatch(exportFund(fundDetail.versionId, data.transformationName));
                }} />
            )
        );
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    handleApproveFundVersion() {
        const { fundRegion: { fundDetail } } = this.props

        const data = {
            dateRange: fundDetail.activeVersion.dateRange,
        }
        this.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.title.approve'),
                <FundForm
                    approve
                    initData={data}
                    onSubmitForm={data => {
                        return this.dispatch(approveFund(fundDetail.versionId, data.dateRange));
                    }} />
            )
        );
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    handleRuleSetUpdateFundVersion() {
        const { fundRegion } = this.props;
        const fundDetail = fundRegion.fundDetail;

        const initData = {
            ruleSetId: fundDetail.activeVersion.ruleSetId
        };
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.ruleSet'),
            <FundForm ruleSet initData={initData}
                onSubmitForm={(data) => this.handleCallEditFundVersion({
                    ...data,
                    name: fundDetail.name,
                    institutionId: fundDetail.institutionId,
                    internalCode: fundDetail.internalCode
                })} />));
    }

    handleEditFundVersion() {
        const { fundRegion } = this.props
        const fundDetail = fundRegion.fundDetail

        const that = this;
        Utils.barrier(
            WebApi.getScopes(fundDetail.versionId),
            WebApi.getAllScopes()
        )
            .then(data => {
                return {
                    scopes: data[0].data,
                    scopeList: data[1].data
                }
            })
            .then(json => {
                const data = {
                    name: fundDetail.name,
                    institutionId: fundDetail.institutionId,
                    internalCode: fundDetail.internalCode,
                    ruleSetId: fundDetail.activeVersion.ruleSetId,
                    apScopes: json.scopes
                };
                that.dispatch(modalDialogShow(that, i18n('arr.fund.title.update'),
                    <FundForm update initData={data} scopeList={json.scopeList}
                        onSubmitForm={that.handleCallEditFundVersion} />));
            });
    }

    handleCallEditFundVersion(data) {
        const { fundRegion } = this.props
        const fundDetail = fundRegion.fundDetail

        data.id = fundDetail.id;
        this.dispatch(scopesDirty(fundDetail.versionId));
        return this.dispatch(updateFund(data));
    }

    /**
     * Vyvolání dialogu s vyhledáním na všemi AS.
     */
    handleFundsSearchForm = () => {
        this.props.dispatch(modalDialogShow(
            this,
            i18n('arr.fund.title.search'),
            <SearchFundsForm />
        ));
    };

    buildRibbon() {
        const { fundRegion, userDetail } = this.props

        const altActions = [];
        if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
            altActions.push(
                <Button key="add-fa" onClick={this.handleAddFund}><Icon glyph="fa-plus-circle" />
                    <div><span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span></div>
                </Button>
            )
        }

        altActions.push(
            <Button key="search-fa" onClick={this.handleFundsSearchForm}><Icon glyph="fa-search" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.search')}</span></div></Button>
        );

        if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
            altActions.push(
                <Button key="fa-import" onClick={this.handleImport}><Icon glyph='fa-upload' />
                    <div><span className="btnText">{i18n('ribbon.action.arr.fund.import')}</span></div>
                </Button>
            )
        }

        const itemActions = [];
        if (fundRegion.fundDetail.id !== null && !fundRegion.fundDetail.fetching && fundRegion.fundDetail.fetched) {
            if (userDetail.hasOne(perms.FUND_ADMIN, { type: perms.FUND_VER_WR, fundId: fundRegion.fundDetail.id })) {
                itemActions.push(
                    <Button key="edit-version" onClick={this.handleEditFundVersion}><Icon glyph="fa-pencil" />
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.update')}</span></div>
                    </Button>,
                    <Button key="rule-set-version" onClick={this.handleRuleSetUpdateFundVersion}><Icon glyph="fa-code-fork" />
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.ruleSet')}</span></div>
                    </Button>,
                    <Button key="approve-version" onClick={this.handleApproveFundVersion}><Icon glyph="fa-calendar-check-o" />
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.approve')}</span></div>
                    </Button>)
            }
            if (userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL)) {
                itemActions.push(
                    <Button key="fa-lecturing" onClick={this.handleIssuesSettings}><Icon glyph='fa-commenting' />
                        <div><span className="btnText">{i18n('arr.issues.settings.title')}</span></div>
                    </Button>,
                )
            }
            if (userDetail.hasOne(perms.FUND_ADMIN)) {
                itemActions.push(
                    <Button key="fa-delete" onClick={this.handleDeleteFund}><Icon glyph='fa-trash' />
                        <div><span className="btnText">{i18n('arr.fund.action.delete')}</span></div>
                    </Button>,
                )
                itemActions.push(
                    <Button key="fa-deletehistory" onClick={this.handleDeleteFundHistory}><Icon glyph='fa-trash' />
                        <div><span className="btnText">{i18n('arr.fund.action.deletehistory')}</span></div>
                    </Button>,
                )
            }
            if (userDetail.hasOne(perms.FUND_EXPORT_ALL, { type: perms.FUND_EXPORT, fundId: fundRegion.fundDetail.id })) {
                itemActions.push(
                    <Button key="fa-export" onClick={this.handleExportDialog}><Icon glyph='fa-download' />
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.export')}</span></div>
                    </Button>
                )
            }
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt-actions" className="small">{altActions}</RibbonGroup>
        }

        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item-actions" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon ref='ribbon' fund altSection={altSection} itemSection={itemSection} {...this.props} />
        )
    }

    handleDeleteFund() {
        const { fundRegion } = this.props
        const fundDetail = fundRegion.fundDetail

        if (confirm(i18n('arr.fund.action.delete.confirm', fundDetail.name))) {
            this.dispatch(deleteFund(fundDetail.id))
        }
    }

    handleDeleteFundHistory() {
        const { fundRegion } = this.props
        const fundDetail = fundRegion.fundDetail

        if (confirm(i18n('arr.fund.action.deletehistory.confirm', fundDetail.name))) {
            this.dispatch(deleteFundHistory(fundDetail.id))
        }
    }

    handleIssuesSettings = () => {
        const { fundRegion } = this.props;
        const fundDetail = fundRegion.fundDetail;

        this.props.dispatch(modalDialogShow(this, i18n("arr.issues.settings.title"), <IssueLists fundId={fundDetail.id} />));
    }

    handleShowInArr(item) {
        // Přepnutí na stránku pořádání
        this.dispatch(routerNavigate('/arr'))

        // Otevření archivního souboru
        var fundObj = getFundFromFundAndVersion(item, item.versions[0]);
        this.dispatch(globalFundTreeInvalidate());
        this.dispatch(selectFundTab(fundObj));
    }

    renderListItem(props) {
        const { item } = props;
        return (
            [
                <div className='item-row'>
                    <div className='name'>{item.name}</div>
                    <div className='btn btn-action' onClick={this.handleShowInArr.bind(this, item)} bsStyle='link'><Icon glyph="fa-folder-open" /></div>
                </div>,
                <div className='item-row desc'>
                    <div>{item.internalCode}</div>
                    <div>{item.id}</div>
                </div>
            ]
        )
    }

    handleSelect(item) {
        this.dispatch(fundsSelectFund(item.id))
    }

    handleSearch(filterText) {
        this.dispatch(fundsSearch(filterText))
    }

    handleSearchClear() {
        this.dispatch(fundsSearch(''))
    }

    handleFilterPrev = () => {
        const { filter } = this.props.fundRegion;
        let { from } = filter;

        if (from >= DEFAULT_FUND_LIST_MAX_SIZE) {
            from = from - DEFAULT_FUND_LIST_MAX_SIZE;
            this.dispatch(fundsFilter({ ...filter, from }));
        }
    };

    handleFilterNext = () => {
        const { filter, fundsCount } = this.props.fundRegion;
        let { from } = filter;

        if (from < fundsCount - DEFAULT_FUND_LIST_MAX_SIZE) {
            from = from + DEFAULT_FUND_LIST_MAX_SIZE;
            this.dispatch(fundsFilter({ ...filter, from }));
        }
    };

    render() {
        const { splitter, focus, fundRegion, maxSize } = this.props;

        let activeIndex
        if (fundRegion.fundDetail.id !== null) {
            activeIndex = indexById(fundRegion.funds, fundRegion.fundDetail.id)
        }

        const leftPanel = (
            <div className="fund-list-container">
                <SearchWithGoto
                    onFulltextSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    filterText={fundRegion.filterText}
                    showFilterResult={true}
                    type="INFO"
                    itemsCount={fundRegion.funds.length}
                    allItemsCount={fundRegion.fundsCount}
                />
                <ListBox
                    className='fund-listbox'
                    ref='fundList'
                    items={fundRegion.funds}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />
                {fundRegion.fundsCount > maxSize && <ListPager
                    prev={this.handleFilterPrev}
                    next={this.handleFilterNext}
                    from={fundRegion.filter.from}
                    maxSize={maxSize}
                    totalCount={fundRegion.fundsCount}
                />}
            </div>
        )

        const centerPanel = <FundDetail
            fundDetail={fundRegion.fundDetail}
            focus={focus}
            fundCount={fundRegion.funds.length}
        />;

        let rightPanel;
        if (fundRegion.fundDetail.fetched) {
            rightPanel = <FundDetailExt
                fundDetail={fundRegion.fundDetail}
                focus={focus}
            />;
        }

        return (
            <PageLayout
                splitter={splitter}
                className='fund-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        )
    }
}

function mapStateToProps(state) {
    const { focus, splitter, fundRegion, userDetail } = state;

    return {
        focus,
        splitter,
        fundRegion,
        userDetail,
    }
}

export default connect(mapStateToProps)(FundPage);
