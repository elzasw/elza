import { showConfirmDialog } from 'components/shared/dialog';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { withRouter } from "react-router";
import { Link } from "react-router-dom";
import {
    approveFund,
    createFund,
    deleteFund,
    deleteFundHistory,
    exportFund,
    updateFund
} from '../../actions/arr/fund';
import { globalFundTreeInvalidate } from '../../actions/arr/globalFundTree';
import {
    DEFAULT_FUND_LIST_MAX_SIZE,
    fundsFetchIfNeeded,
    fundsFilter,
    fundsFundDetailFetchIfNeeded,
    fundsSearch,
    fundsSelectFund,
    getFundDetail
} from '../../actions/fund/fund';
import { modalDialogShow } from '../../actions/global/modalDialog';
import { WebApi } from '../../actions/index';
import { refInstitutionsFetchIfNeeded } from '../../actions/refTables/institutions';
import { refRuleSetFetchIfNeeded } from '../../actions/refTables/ruleSet';
import { scopesDirty } from '../../actions/refTables/scopesData';
import { routerNavigate } from '../../actions/router';
import * as perms from '../../actions/user/Permission';
import { ExportForm, FundForm, i18n, Icon, ImportForm } from '../../components';
import IssueLists from '../../components/arr/IssueLists';
import SearchFundsForm from '../../components/arr/search-funds-form/SearchFundsForm';
import { AbstractReactComponent, ListBox } from '../../components/shared';
import ListPager from '../../components/shared/listPager/ListPager';
import { urlEntity, urlFund, urlFundTree } from "../../constants";
import { objectById } from '../../shared/utils';
import { indexById } from '../../stores/app/utils';
import PageLayout from '../shared/layout/PageLayout';
import './FundPage.scss';
import { Menu, MenuButton, MenuItem, MenuList, MenuPopover, MenuTrigger } from '@fluentui/react-components';
import { FundFilters } from 'components/fund/filters/FundFilters';
import { FundPageRibbon } from 'components/fund/FundPageRibbon';


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
        this.props.dispatch(refRuleSetFetchIfNeeded());
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

    async handleExportDialog(fundId) {
        const { dispatch } = this.props;
        const fundDetail = await getFundDetail(fundId);

        dispatch(
            modalDialogShow(
                this,
                i18n('export.title.fund'),
                <ExportForm
                    fund={true}
                    initialValues={{
                        includeUUID:true,
                        includeAccessPoints: true
                    }}
                    onSubmitForm={({exportFilterId, includeUUID, includeAccessPoints}) => {
                        return dispatch(exportFund(fundDetail.versionId, {exportFilterId, includeUUID, includeAccessPoints}));
                    }}
                />,
            ),
        );
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    async handleApproveFundVersion(fundId) {
        const { dispatch } = this.props;

        const fundDetail = await getFundDetail(fundId);
        const data = { dateRange: fundDetail.activeVersion.dateRange, };

        dispatch(
            modalDialogShow(
                this,
                `${i18n('arr.fund.title.approve')}`,
                <FundForm
                    approve
                    initialValues={data}
                    onSubmitForm={() => {
                        return dispatch(approveFund(fundDetail.versionId));
                    }}
                />,
            ),
        );
    }

    /**
     * Zobrazení dualogu uzavření verze AS.
     */
    async handleRuleSetUpdateFundVersion(fundId) {
        const {institutionsAll} = this.props;
        const fundDetail = await getFundDetail(fundId);
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
                        this.handleCallEditFundVersion(fundDetail, {
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

    async handleEditFundVersion(fundId) {
        const {ruleSet, institutionsAll} = this.props;
        const fundDetail = await getFundDetail(fundId);
        const rules = objectById(ruleSet.items, fundDetail.activeVersion.ruleSetId);
        const institution = objectById(institutionsAll.items, fundDetail.institutionId);
        const scopeList = await WebApi.getAllScopes();

        const data = {
            name: fundDetail.name,
            institutionIdentifier: institution.code,
            internalCode: fundDetail.internalCode,
            fundNumber: fundDetail.fundNumber,
            unitdate: fundDetail.unitdate,
            mark: fundDetail.mark,
            ruleSetCode: rules.code,
            scopes: (fundDetail.apScopes || []).map(i => i.code),
            managed: fundDetail.managed,
        };

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.title.update'),
                <FundForm
                    update
                    initialValues={data}
                    scopeList={scopeList}
                    onSubmitForm={(_data) => this.handleCallEditFundVersion(fundDetail, _data)}
                />,
            ),
        );
    }

    async handleCallEditFundVersion(fundDetail, data) {
        const {dispatch} = this.props;

        dispatch(scopesDirty(fundDetail.versionId));
        return dispatch(
            updateFund(fundDetail.id, {
                scopes: data.scopes,
                institutionIdentifier: data.institutionIdentifier,
                internalCode: data.internalCode,
                name: data.name,
                ruleSetCode: data.ruleSetCode,
                fundNumber: data.fundNumber,
                unitdate: data.unitdate,
                mark: data.mark,
                managed: data.managed,
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
        return <FundPageRibbon
            onAddFund={this.handleAddFund}
            onFundsSearchForm={this.handleFundsSearchForm}
            onImport={this.handleImport}
        />
    }

    async handleDeleteFund(fundId) {
        const {dispatch} = this.props;
        const fundDetail = await getFundDetail(fundId);

        const response = await dispatch(showConfirmDialog(i18n('arr.fund.action.delete.confirm', fundDetail.name)));
        if (response) {
            dispatch(deleteFund(fundDetail.id));
        }
    }

    async handleDeleteFundHistory(fundId) {
        const {dispatch} = this.props;
        const fundDetail = await getFundDetail(fundId);

        const response = await dispatch(showConfirmDialog(i18n('arr.fund.action.deletehistory.confirm', fundDetail.name)));
        if (response) {
            dispatch(deleteFundHistory(fundDetail.id));
        }
    }

    copyToClipboard = async (string) => {
        if(navigator.clipboard){
            navigator.clipboard.writeText(string);
        }
    };

    handleIssuesSettings = async (fundId) => {
        const {dispatch} = this.props;
        const fundDetail = await getFundDetail(fundId);

        dispatch(
            modalDialogShow(this, i18n('arr.issues.settings.title'), <IssueLists fundId={fundDetail.id} />),
        );
    };

    async handleShowInArr(item) {
        const { dispatch } = this.props;

        // Load fund detail
        const {id, versions} = await getFundDetail(item.id);

        dispatch(globalFundTreeInvalidate());
        // redirecting to arrangement page of the loaded fund
        dispatch(routerNavigate(urlFundTree(id, versions[0].id)));
    }

    renderListItem(props) {
        const { institutionsAll, userDetail } = this.props;
        const {item} = props;
        const institution = institutionsAll.items.find(({code}) => code == item.institutionIdentifier);

        const itemActions = [];
        if (item.id !== null) {
            if (userDetail.hasOne(perms.FUND_ADMIN, {type: perms.FUND_VER_WR, fundId: item.id})) {
                itemActions.push(
                    <MenuItem
                        key="edit-version"
                        icon={<Icon glyph="fa-pencil" />}
                        title={i18n('ribbon.action.arr.fund.update')}
                        onClick={() => this.handleEditFundVersion(item.id)}
                    >
                        {i18n('ribbon.action.arr.fund.update')}
                    </MenuItem>,
                    <MenuItem
                        key="rule-set-version"
                        icon={<Icon glyph="fa-calendar-check-o" />}
                        title={i18n('ribbon.action.arr.fund.ruleSet')}
                        onClick={() => this.handleRuleSetUpdateFundVersion(item.id)}
                    >
                        {i18n('ribbon.action.arr.fund.ruleSet')}
                    </MenuItem>,
                    <MenuItem
                        key="approve-version"
                        icon={<Icon glyph="fa-code-fork" />}
                        title={i18n('ribbon.action.arr.fund.approve')}
                        onClick={() => this.handleApproveFundVersion(item.id)}
                    >
                        {i18n('ribbon.action.arr.fund.approve')}
                    </MenuItem>,
                );
            }
            if (userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL, {type: perms.FUND_ISSUE_ADMIN, fundId: item.id})) {
                itemActions.push(
                    <MenuItem
                        key="fa-lecturing"
                        icon={<Icon glyph="fa-commenting" />}
                        title={i18n('arr.issues.settings.title')}
                        onClick={() => this.handleIssuesSettings(item.id)}
                    >
                        {i18n('arr.issues.settings.title')}
                    </MenuItem>,
                );
            }
            if (userDetail.hasOne(perms.FUND_EXPORT_ALL, {type: perms.FUND_EXPORT, fundId: item.id})) {
                itemActions.push(
                    <MenuItem
                        key="fa-export"
                        icon={<Icon glyph="fa-download" />}
                        title={i18n('ribbon.action.arr.fund.export')}
                        onClick={() => this.handleExportDialog(item.id)}
                    >
                        {i18n('ribbon.action.arr.fund.export')}
                    </MenuItem>,
                );
            }
            if (userDetail.hasOne(perms.FUND_ADMIN)) {
                itemActions.push(
                    <MenuItem
                        className="danger"
                        key="fa-deletehistory"
                        icon={<Icon glyph="fa-times-circle-o" />}
                        title={i18n('arr.fund.action.deletehistory')}
                        onClick={() => this.handleDeleteFundHistory(item.id)}
                    >
                        {i18n('arr.fund.action.deletehistory')}
                    </MenuItem>,
                );
                itemActions.push(
                    <MenuItem
                        className="danger"
                        key="fa-delete"
                        icon={<Icon glyph="fa-trash" />}
                        title={i18n('arr.fund.action.delete')}
                        onClick={() => this.handleDeleteFund(item.id)}
                    >
                        {i18n('arr.fund.action.delete')}
                    </MenuItem>,
                );
            }
        }
        return <>
            <div style={{flexGrow: 1, flexShrink: 1, overflow: "hidden"}}>
                <div className="item-row" key={item.id}>
                    <Link className="name main link" title={item.name} key={`fund-${item.id}`} to={urlFundTree(item.id)}>
                        {item.name}
                    </Link>
                    {institution && <Link className="name desc-part link bubble shrink" title={institution.name} key={`fund-${item.id}`} to={urlEntity(institution.accessPointId)}>
                        <div>
                            {institution.name}
                        </div>
                    </Link>}
                    {!institution && <span className="desc-part">{item.institutionIdentifier}</span>}
                    <div style={{flexGrow: 1}}></div>
                </div>
                <div className="item-row desc" key={item.id + '-x'}>
                    <div style={{display: "flex", width: "100%"}}>
                        {/* <span className="desc-part id bubble" onClick={() => this.copyToClipboard(item.id)}>{item.id}</span> */}
                        {item.internalCode && <span className="desc-part bubble internal-code" onClick={() => this.copyToClipboard(item.internalCode)}>{item.internalCode}</span>}
                        {item.fundNumber && <span className="desc-part bubble" onClick={() => this.copyToClipboard(item.fundNumber)}>{item.fundNumber}</span>}
                        {item.mark && <span className="desc-part bubble" onClick={() => this.copyToClipboard(item.mark)}>{item.mark}</span>}

                        {item.createDate && <span className="desc-part muted">vytvořeno: {new Date(item.createDate).toLocaleDateString()}, {new Date(item.createDate).toLocaleTimeString(undefined, {timeStyle: "short"})}</span>}
                    </div>
                    <div style={{flexGrow: 1}}></div>
                </div>
            </div>
            <div className="fund-actions">
                {itemActions.length > 0 &&
                    <Menu>
                        <MenuTrigger disableButtonEnhancement={true}>
                            <MenuButton appearance='subtle' icon={<Icon glyph="fa-ellipsis-v"/>}/>
                        </MenuTrigger>
                        <MenuPopover>
                            <MenuList>
                                {itemActions.map((action) => {
                                    return action;
                                })}
                            </MenuList>
                        </MenuPopover>
                    </Menu>
                }
            </div>
        </>;
    }

    handleSelect(item) {
        const {history, dispatch} = this.props;
        history.push(urlFund(item.id));
        dispatch(fundsSelectFund(item.id));
    }

    // handleSearch({fulltext}) {
    //     const {filter} = this.props.fundRegion;
    //     this.props.dispatch(fundsFilter({
    //         ...filter,
    //         fulltext,
    //     }));
    // }

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

    handleFiltersChange = (filters) => {
        const {dispatch, fundRegion} = this.props;
        dispatch(fundsFilter({ ...fundRegion.filter, filter: filters }));
    }

    render() {
        const {splitter, fundRegion, maxSize} = this.props;

        let activeIndex;
        if (fundRegion.fundDetail.id !== null) {
            activeIndex = indexById(fundRegion.funds, fundRegion.fundDetail.id);
        }

        const leftPanel = (
            <div className="fund-list-container">
                <div className="filter-container">
                    <FundFilters currentFilters={fundRegion.filter.filter} onChange={this.handleFiltersChange}/>
                </div>
                <ListBox
                    className="fund-listbox"
                    ref="fundList"
                    items={fundRegion.funds}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    // onFocus={this.handleSelect}
                    // onSelect={this.handleSelect}
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

        // const centerPanel = (
        //     <FundDetail fundDetail={fundRegion.fundDetail} focus={focus} fundCount={fundRegion.funds.length} />
        // );

        // let rightPanel;
        // if (fundRegion.fundDetail.fetched) {
        //     rightPanel = <FundDetailExt fundDetail={fundRegion.fundDetail} focus={focus} />;
        // }

        return (
            <PageLayout
                splitter={splitter}
                className="fund-page"
                ribbon={this.buildRibbon()}
                // leftPanel={leftPanel}
                centerPanel={leftPanel}
                // rightPanel={rightPanel}
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
