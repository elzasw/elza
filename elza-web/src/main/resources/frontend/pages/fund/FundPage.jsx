/**
 * Stránka archivní soubory.
 */

require ('./FundPage.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Icon, i18n} from 'components/index.jsx';
import {Splitter, Autocomplete, FundForm, Ribbon, RibbonGroup, ToggleContent, FindindAidFileTree, AbstractReactComponent,
    ImportForm, ExportForm, Search, ListBox, FundDetail, FundDetailExt} from 'components';
import {NodeTabs, FundTreeTabs} from 'components/index.jsx';
import {ButtonGroup, Button, Panel} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {createFund} from 'actions/arr/fund.jsx'
import {storeLoadData, storeSave, storeLoad} from 'actions/store/store.jsx'
import {Combobox} from 'react-input-enhancements'
import {WebApi} from 'actions/index.jsx';
import {setInputFocus, dateToString} from 'components/Utils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {selectFundTab} from 'actions/arr/fund.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {fundsFetchIfNeeded, fundsSelectFund, fundsFundDetailFetchIfNeeded, fundsSearch} from 'actions/fund/fund.jsx'
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx'
import {approveFund, deleteFund, exportFund} from 'actions/arr/fund.jsx'
import {barrier} from 'components/Utils.jsx';
import {scopesDirty} from 'actions/refTables/scopesData.jsx'
import * as perms from 'actions/user/Permission.jsx';

var FundPage = class FundPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'handleAddFund',
            'handleImport',
            'handleExportDialog',
            'handleExport',
            'renderListItem',
            'handleSelect',
            'handleSearch',
            'handleSearchClear',
            'handleApproveFundVersion',
            'handleEditFundVersion',
            'handleCallEditFundVersion',
            'handleDeleteFund',
            'handleRuleSetUpdateFundVersion'
        );

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fundsFetchIfNeeded())
        this.dispatch(fundsFundDetailFetchIfNeeded())
    }

    componentDidMount() {
        this.dispatch(fundsFetchIfNeeded())
        this.dispatch(fundsFundDetailFetchIfNeeded())
    }

    handleAddFund() {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.add'),
            <FundForm create onSubmitForm={(data) => {this.dispatch(createFund(data))}}/>));
    }

    handleImport() {
        this.dispatch(
            modalDialogShow(this,
                i18n('import.title.fund'),
                <ImportForm fund={true}/>
            )
        );
    }

    handleExportDialog() {
        const {fundRegion: {fundDetail}} = this.props;
        this.dispatch(
            modalDialogShow(this,
                i18n('export.title.fund'),
                <ExportForm fund={true} onSubmitForm={data => {this.dispatch(exportFund(fundDetail.versionId, data.transformationName))}} />
            )
        );
    }

    handleExport(values) {
        console.log(values);
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    handleApproveFundVersion() {
        const {fundRegion: {fundDetail}} = this.props

        var data = {
            dateRange: fundDetail.activeVersion.dateRange,
        }
        this.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.title.approve'),
                <FundForm
                    approve
                    initData={data}
                    onSubmitForm={data => {this.dispatch(approveFund(fundDetail.versionId, data.dateRange))}}/>
            )
        );
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    handleRuleSetUpdateFundVersion() {
        const {fundRegion} = this.props;
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
              })}/>));
    }

    handleEditFundVersion() {
        const {fundRegion} = this.props
        const fundDetail = fundRegion.fundDetail

        var that = this;
        barrier(
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
                var data = {
                    name: fundDetail.name,
                    institutionId: fundDetail.institutionId,
                    internalCode: fundDetail.internalCode,
                    ruleSetId: fundDetail.activeVersion.ruleSetId,
                    regScopes: json.scopes
                };
                that.dispatch(modalDialogShow(that, i18n('arr.fund.title.update'),
                    <FundForm update initData={data} scopeList={json.scopeList}
                            onSubmitForm={that.handleCallEditFundVersion}/>));
            });
    }

    handleCallEditFundVersion(data) {
        const {fundRegion} = this.props
        const fundDetail = fundRegion.fundDetail        

        data.id = fundDetail.id;
        this.dispatch(scopesDirty(fundDetail.versionId));
        WebApi.updateFund(data).then((json) => {
            this.dispatch(modalDialogHide());
        })
    }

    buildRibbon() {
        const {fundRegion, userDetail} = this.props

        var altActions = [];
        if (userDetail.hasOne(perms.FUND_ADMIN)) {
            altActions.push(
                <Button key="add-fa" onClick={this.handleAddFund}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span></div></Button>
            )
            altActions.push(
                <Button key="fa-import" onClick={this.handleImport}><Icon glyph='fa-upload'/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fund.import')}</span></div>
                </Button>
            )
        }

        var itemActions = [];
        if (fundRegion.fundDetail.id !== null && !fundRegion.fundDetail.fetching && fundRegion.fundDetail.fetched) {
            if (userDetail.hasOne(perms.FUND_ADMIN, {type: perms.FUND_VER_WR, fundId: fundRegion.fundDetail.id})) {
                itemActions.push(
                    <Button key="edit-version" onClick={this.handleEditFundVersion}><Icon glyph="fa-pencil"/>
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.update')}</span></div>
                    </Button>,
                    <Button key="rule-set-version" onClick={this.handleRuleSetUpdateFundVersion}><Icon glyph="fa-code-fork"/>
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.ruleSet')}</span></div>
                    </Button>,
                    <Button key="approve-version" onClick={this.handleApproveFundVersion}><Icon glyph="fa-calendar-check-o"/>
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.approve')}</span></div>
                    </Button>)
            }
            if (userDetail.hasOne(perms.FUND_ADMIN)) {
                itemActions.push(
                    <Button key="fa-delete" onClick={this.handleDeleteFund}><Icon glyph='fa-trash'/>
                        <div><span className="btnText">{i18n('arr.fund.action.delete')}</span></div>
                    </Button>,
                )
            }
            if (userDetail.hasOne(perms.FUND_EXPORT_ALL, {type: perms.FUND_EXPORT, fundId: fundRegion.fundDetail.id})) {
                itemActions.push(
                    <Button key="fa-export" onClick={this.handleExportDialog}><Icon glyph='fa-download'/>
                        <div><span className="btnText">{i18n('ribbon.action.arr.fund.export')}</span></div>
                    </Button>
                )
            }
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="small">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon ref='ribbon' fund altSection={altSection} itemSection={itemSection} {...this.props} />
        )
    }

    handleDeleteFund() {
        const {fundRegion} = this.props
        const fundDetail = fundRegion.fundDetail

        if (confirm(i18n('arr.fund.action.delete.confirm', fundDetail.name))) {
            this.dispatch(deleteFund(fundDetail.id))
        }
    }

    handleShowInArr(item) {
        // Přepnutí na stránku pořádání
        this.dispatch(routerNavigate('/arr'))

        // Otevření archivního souboru
        var fundObj = getFundFromFundAndVersion(item, item.versions[0]);
        this.dispatch(selectFundTab(fundObj));
    }

    renderListItem(item) {
        return (
            <div>
                <div className='name'>{item.name}</div>
                <div><Button className='link' onClick={this.handleShowInArr.bind(this, item)} bsStyle='link'>{i18n('arr.fund.action.showInArr')}</Button></div>
                <div>{item.internalCode}</div>
            </div>
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

    render() {
        const {splitter, focus, fundRegion} = this.props;

        var activeIndex
        if (fundRegion.fundDetail.id !== null) {
            activeIndex = indexById(fundRegion.funds, fundRegion.fundDetail.id)
        }
        var leftPanel = (
            <div className="fund-list-container">
                <Search
                    onSearch={this.handleSearch}
                    onClear={this.handleSearchClear}
                    placeholder={i18n('search.input.search')}
                    value={fundRegion.filterText}
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
            </div>
        )

        var centerPanel = (
            <FundDetail
                fundDetail={fundRegion.fundDetail}
                focus={focus}
            />
        )

        var rightPanel = (
            <FundDetailExt
                fundDetail={fundRegion.fundDetail}
                focus={focus}
            />
        )

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
    const {focus, splitter, fundRegion, userDetail} = state
    return {
        focus,
        splitter,
        fundRegion,
        userDetail,
    }
}

module.exports = connect(mapStateToProps)(FundPage);

