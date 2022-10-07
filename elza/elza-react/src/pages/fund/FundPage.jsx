import './FundPage.scss';
import PropTypes from 'prop-types';

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, ListBox, RibbonGroup, SearchWithGoto, Utils} from '../../components/shared';
import {i18n, Icon, ExportForm, FundDetail, FundDetailExt, FundForm, ImportForm, Ribbon} from '../../components';
import {Button} from '../../components/ui';
import PageLayout from '../shared/layout/PageLayout';
import {modalDialogShow} from '../../actions/global/modalDialog';
import {
    approveFund,
    createFund,
    deleteFund,
    deleteFundHistory,
    exportFund,
    selectFundTab,
    updateFund,
} from '../../actions/arr/fund';
import {WebApi} from '../../actions/index';
import {indexById} from '../../stores/app/utils';
import {routerNavigate} from '../../actions/router';
import {
    DEFAULT_FUND_LIST_MAX_SIZE,
    fundsFetchIfNeeded,
    fundsFilter,
    fundsFundDetailFetchIfNeeded,
    fundsSearch,
    fundsSelectFund,
} from '../../actions/fund/fund';
import {getFundFromFundAndVersion} from '../../components/arr/ArrUtils';
import {scopesDirty} from '../../actions/refTables/scopesData';
import * as perms from '../../actions/user/Permission';
import {globalFundTreeInvalidate} from '../../actions/arr/globalFundTree';
import SearchFundsForm from '../../components/arr/SearchFundsForm';
import IssueLists from '../../components/arr/IssueLists';
import ListPager from '../../components/shared/listPager/ListPager';
import {Autocomplete} from '../../components/shared';
import {refInstitutionsFetchIfNeeded} from '../../actions/refTables/institutions';
import {objectById} from '../../shared/utils';
import { showConfirmDialog } from 'components/shared/dialog';
import {withRouter} from "react-router";
import {URL_FUND_TREE, urlFund} from "../../constants";

/**
 * Stránka archivní soubory.
 */

class FundPage extends AbstractReactComponent {
    static propTypes = {
        maxSize: PropTypes.number,
    };

    static defaultProps = {
        maxSize: DEFAULT_FUND_LIST_MAX_SIZE,
    };

    state = {institutions: []};

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
            'handleRuleSetUpdateFundVersion',
        );

        this.buildRibbon = this.buildRibbon.bind(this);
        WebApi.getInstitutions(true).then(institutions => this.setState({institutions}));
    }

    UNSAFE_componentWillReceiveProps() {
        this.props.dispatch(fundsFetchIfNeeded());
        this.props.dispatch(fundsFundDetailFetchIfNeeded());
        this.props.dispatch(refInstitutionsFetchIfNeeded());
    }

    componentDidMount() {
        const {dispatch, fundRegion, history, select = false} = this.props;
        dispatch(fundsFetchIfNeeded());
        dispatch(refInstitutionsFetchIfNeeded());

        if (!select) {
            const matchId = this.props.match.params.id;

            // pokud si pamatujeme poslední navštívý AS při prvním vstupu - provedeme přesměrování
            if (fundRegion.fundDetail.id !== null && matchId == null) {
                history.replace(urlFund(fundRegion.fundDetail.id));
            }

            if (matchId) {
                dispatch(fundsSelectFund(matchId));
            }
        }
    }

    handleAddFund() {
        const {userDetail} = this.props;
        let initData = {};
        if (!userDetail.hasOne(perms.ADMIN, perms.FUND_ADMIN)) {
            initData.fundAdmins = [{id: 'default', user: userDetail}];
        }
        WebApi.getAllScopes().then(scopes => {
            this.props.dispatch(
                modalDialogShow(
                    this,
                    i18n('arr.fund.title.add'),
                    <FundForm
                        create
                        initialValues={initData}
                        scopeList={scopes}
                        onSubmitForm={data => {
                            return this.props.dispatch(createFund(data));
                        }}
                    />,
                ),
            );
        });
    }

    handleImport() {
        this.props.dispatch(modalDialogShow(this, i18n('import.title.fund'), <ImportForm fund={true} />));
    }

    handleExportDialog() {
        const {
            fundRegion: {fundDetail},
        } = this.props;
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('export.title.fund'),
                <ExportForm
                    fund={true}
                    onSubmitForm={data => {
                        return this.props.dispatch(exportFund(fundDetail.versionId, data.transformationName, data.exportFilterId));
                    }}
                />,
            ),
        );
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    handleApproveFundVersion() {
        const {
            fundRegion: {fundDetail},
        } = this.props;

        const data = {
            dateRange: fundDetail.activeVersion.dateRange,
        };
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.title.approve'),
                <FundForm
                    approve
                    initialValues={data}
                    onSubmitForm={data => {
                        return this.props.dispatch(approveFund(fundDetail.versionId));
                    }}
                />,
            ),
        );
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    handleRuleSetUpdateFundVersion() {
        const {fundRegion, ruleSet, institutionsAll} = this.props;
        const fundDetail = fundRegion.fundDetail;
        const institution = objectById(institutionsAll.items, fundDetail.institutionId);

        const initData = {
            ruleSetId: fundDetail.activeVersion.ruleSetId,
        };
        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.title.ruleSet'),
                <FundForm
                    ruleSet
                    initialValues={initData}
                    onSubmitForm={data =>
                        this.handleCallEditFundVersion({
                            ...data,
                            name: fundDetail.name,
                            institutionIdentifier: institution.code,
                            internalCode: fundDetail.internalCode,
                            fundNumber: fundDetail.fundNumber,
                            unitdate: fundDetail.unitdate,
                            mark: fundDetail.mark,
                        })
                    }
                />,
            ),
        );
    }

    handleEditFundVersion() {
        const {fundRegion, ruleSet, institutionsAll} = this.props;
        const fundDetail = fundRegion.fundDetail;
        const rules = objectById(ruleSet.items, fundDetail.activeVersion.ruleSetId);
        const institution = objectById(institutionsAll.items, fundDetail.institutionId);

        Utils.barrier(WebApi.getScopes(fundDetail.versionId), WebApi.getAllScopes())
            .then(data => {
                return {
                    scopes: data[0].data,
                    scopeList: data[1].data,
                };
            })
            .then(json => {
                const data = {
                    name: fundDetail.name,
                    institutionIdentifier: institution.code,
                    internalCode: fundDetail.internalCode,
                    fundNumber: fundDetail.fundNumber,
                    unitdate: fundDetail.unitdate,
                    mark: fundDetail.mark,
                    ruleSetCode: rules.code,
                    scopes: (fundDetail.apScopes || []).map(i => i.code),
                };
                this.props.dispatch(
                    modalDialogShow(
                        this,
                        i18n('arr.fund.title.update'),
                        <FundForm
                            update
                            initialValues={data}
                            scopeList={json.scopeList}
                            onSubmitForm={this.handleCallEditFundVersion}
                        />,
                    ),
                );
            });
    }

    handleCallEditFundVersion(data) {
        const {fundRegion} = this.props;
        const fundDetail = fundRegion.fundDetail;

        this.props.dispatch(scopesDirty(fundDetail.versionId));
        return this.props.dispatch(
            updateFund(fundDetail.id, {
                scopes: data.scopes,
                institutionIdentifier: data.institutionIdentifier,
                internalCode: data.internalCode,
                name: data.name,
                ruleSetCode: data.ruleSetCode,
                fundNumber: data.fundNumber,
                unitdate: data.unitdate,
                mark: data.mark,
            }),
        );
    }

    /**
     * Vyvolání dialogu s vyhledáním na všemi AS.
     */
    handleFundsSearchForm = () => {
        this.props.dispatch(modalDialogShow(this, i18n('arr.fund.title.search'), <SearchFundsForm />));
    };

    buildRibbon() {
        const {fundRegion, userDetail} = this.props;

        const altActions = [];
        if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
            altActions.push(
                <Button key="add-fa" onClick={this.handleAddFund}>
                    <Icon glyph="fa-plus-circle" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.arr.fund.add')}</span>
                    </div>
                </Button>,
            );
        }

        altActions.push(
            <Button key="search-fa" onClick={this.handleFundsSearchForm}>
                <Icon glyph="fa-search" />
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.fund.search')}</span>
                </div>
            </Button>,
        );

        if (userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_CREATE)) {
            altActions.push(
                <Button key="fa-import" onClick={this.handleImport}>
                    <Icon glyph="fa-upload" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.arr.fund.import')}</span>
                    </div>
                </Button>,
            );
        }

        const itemActions = [];
        if (fundRegion.fundDetail.id !== null && !fundRegion.fundDetail.fetching && fundRegion.fundDetail.fetched) {
            if (userDetail.hasOne(perms.FUND_ADMIN, {type: perms.FUND_VER_WR, fundId: fundRegion.fundDetail.id})) {
                itemActions.push(
                    <Button key="edit-version" onClick={this.handleEditFundVersion}>
                        <Icon glyph="fa-pencil" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.arr.fund.update')}</span>
                        </div>
                    </Button>,
                    <Button key="rule-set-version" onClick={this.handleRuleSetUpdateFundVersion}>
                        <Icon glyph="fa-calendar-check-o" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.arr.fund.ruleSet')}</span>
                        </div>
                    </Button>,
                    <Button key="approve-version" onClick={this.handleApproveFundVersion}>
                        <Icon glyph="fa-code-fork" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.arr.fund.approve')}</span>
                        </div>
                    </Button>,
                );
            }
            if (userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL, {type: perms.FUND_ISSUE_ADMIN, fundId: fundRegion.fundDetail.id})) {
                itemActions.push(
                    <Button key="fa-lecturing" onClick={this.handleIssuesSettings}>
                        <Icon glyph="fa-commenting" />
                        <div>
                            <span className="btnText">{i18n('arr.issues.settings.title')}</span>
                        </div>
                    </Button>,
                );
            }
            if (userDetail.hasOne(perms.FUND_ADMIN)) {
                itemActions.push(
                    <Button key="fa-delete" onClick={this.handleDeleteFund}>
                        <Icon glyph="fa-trash" />
                        <div>
                            <span className="btnText">{i18n('arr.fund.action.delete')}</span>
                        </div>
                    </Button>,
                );
                itemActions.push(
                    <Button key="fa-deletehistory" onClick={this.handleDeleteFundHistory}>
                        <Icon glyph="fa-times-circle-o" />
                        <div>
                            <span className="btnText">{i18n('arr.fund.action.deletehistory')}</span>
                        </div>
                    </Button>,
                );
            }
            if (userDetail.hasOne(perms.FUND_EXPORT_ALL, {type: perms.FUND_EXPORT, fundId: fundRegion.fundDetail.id})) {
                itemActions.push(
                    <Button key="fa-export" onClick={this.handleExportDialog}>
                        <Icon glyph="fa-download" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.arr.fund.export')}</span>
                        </div>
                    </Button>,
                );
            }
        }

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

        return <Ribbon ref="ribbon" fund altSection={altSection} itemSection={itemSection} {...this.props} />;
    }

    async handleDeleteFund() {
        const {fundRegion, dispatch} = this.props;
        const fundDetail = fundRegion.fundDetail;

        const response = await dispatch(showConfirmDialog(i18n('arr.fund.action.delete.confirm', fundDetail.name)));
        if (response) {
            this.props.dispatch(deleteFund(fundDetail.id));
        }
    }

    async handleDeleteFundHistory() {
        const {fundRegion, dispatch} = this.props;
        const fundDetail = fundRegion.fundDetail;

        const response = await dispatch(showConfirmDialog(i18n('arr.fund.action.deletehistory.confirm', fundDetail.name)));
        if (response) {
            this.props.dispatch(deleteFundHistory(fundDetail.id));
        }
    }

    handleIssuesSettings = () => {
        const {fundRegion} = this.props;
        const fundDetail = fundRegion.fundDetail;

        this.props.dispatch(
            modalDialogShow(this, i18n('arr.issues.settings.title'), <IssueLists fundId={fundDetail.id} />),
        );
    };

    handleShowInArr(item) {
        // Přepnutí na stránku pořádání
        this.props.dispatch(routerNavigate(URL_FUND_TREE));

        // Otevření archivního souboru
        WebApi.getFundDetail(item.id).then(data => {
            const fundObj = getFundFromFundAndVersion(data, data.versions[0]);
            this.props.dispatch(globalFundTreeInvalidate());
            this.props.dispatch(selectFundTab(fundObj));
        });
    }

    renderListItem(props) {
        const {item} = props;
        // popis archivního souboru
        var fields=[];
        if(item.internalCode) {
            fields.push(item.internalCode);
        }
        if(item.fundNumber) {
            fields.push(item.fundNumber);
        }
        if(item.mark) {
            fields.push(item.mark);
        }
        var desc = fields.join(', ');
        return [
            <div className="item-row" key={item.id}>
                <div className="name">{item.name}</div>
                <Button variant="action" onClick={this.handleShowInArr.bind(this, item)}>
                    <Icon glyph="fa-folder-open" />
                </Button>
            </div>,
            <div className="item-row desc" key={item.id + '-x'}>
                <div>{desc}</div>
                <div>{item.id}</div>
            </div>,
        ];
    }

    handleSelect(item) {
        const {history, dispatch} = this.props;
        history.push(urlFund(item.id));
        dispatch(fundsSelectFund(item.id));
    }

    handleSearch(filterText) {
        this.props.dispatch(fundsSearch(filterText));
    }

    handleSearchClear() {
        this.props.dispatch(fundsSearch(''));
    }

    handleFilterPrev = () => {
        const {filter} = this.props.fundRegion;
        let {from} = filter;

        if (from >= DEFAULT_FUND_LIST_MAX_SIZE) {
            from = from - DEFAULT_FUND_LIST_MAX_SIZE;
            this.props.dispatch(fundsFilter({...filter, from}));
        }
    };

    handleFilterNext = () => {
        const {filter, fundsCount} = this.props.fundRegion;
        let {from} = filter;

        if (from < fundsCount - DEFAULT_FUND_LIST_MAX_SIZE) {
            from = from + DEFAULT_FUND_LIST_MAX_SIZE;
            this.props.dispatch(fundsFilter({...filter, from}));
        }
    };

    handleFilterInstitution = institutionIdentifier => {
        const {filter} = this.props.fundRegion;

        if (institutionIdentifier !== filter.institutionIdentifier) {
            this.props.dispatch(fundsFilter({...filter, institutionIdentifier}));
        }
    };

    render() {
        const {splitter, focus, fundRegion, maxSize} = this.props;

        let activeIndex;
        if (fundRegion.fundDetail.id !== null) {
            activeIndex = indexById(fundRegion.funds, fundRegion.fundDetail.id);
        }

        const leftPanel = (
            <div className="fund-list-container">
                <Autocomplete
                    useIdAsValue={true}
                    items={[{code: undefined, name: i18n('global.all')}, ...this.state.institutions]}
                    getItemId={item => (item ? item.code : undefined)}
                    getItemName={item =>
                        item
                            ? item.name
                                ? item.name
                                : i18n('arr.fund.filterSettings.value.empty') + ' id:' + item.id
                            : undefined
                    }
                    placeholder={i18n('arr.fund.institution')}
                    value={fundRegion.filter.institutionIdentifier}
                    onChange={this.handleFilterInstitution}
                />
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
                    className="fund-listbox"
                    ref="fundList"
                    items={fundRegion.funds}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />
                {(
                    fundRegion.fundsCount > maxSize ||
                    fundRegion.filter.from !== 0
                ) && (
                    <ListPager
                        prev={this.handleFilterPrev}
                        next={this.handleFilterNext}
                        from={fundRegion.filter.from}
                        pageSize={maxSize}
                        totalCount={fundRegion.fundsCount}
                    />
                )}
            </div>
        );

        const centerPanel = (
            <FundDetail fundDetail={fundRegion.fundDetail} focus={focus} fundCount={fundRegion.funds.length} />
        );

        let rightPanel;
        if (fundRegion.fundDetail.fetched) {
            rightPanel = <FundDetailExt fundDetail={fundRegion.fundDetail} focus={focus} />;
        }

        return (
            <PageLayout
                splitter={splitter}
                className="fund-page"
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        );
    }
}

function mapStateToProps(state) {
    const {focus, splitter, fundRegion, userDetail, refTables} = state;

    return {
        focus,
        splitter,
        fundRegion,
        userDetail,
        ruleSet: refTables.ruleSet,
        institutionsAll: refTables.institutions,
    };
}

export default withRouter(connect(mapStateToProps)(FundPage));
