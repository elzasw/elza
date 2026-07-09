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
    fundsExportResults,
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
import { urlEntity, urlFund, urlFundOutputs, urlFundTree } from "../../constants";
import { objectById } from '../../shared/utils';
import { indexById } from '../../stores/app/utils';
import PageLayout from '../shared/layout/PageLayout';
import './FundPage.scss';
import { Button, Checkbox, DrawerBody, DrawerHeader, DrawerHeaderTitle, InlineDrawer, Menu, MenuButton, MenuItem, MenuList, MenuPopover, MenuTrigger } from '@fluentui/react-components';
import { Dismiss24Regular, ArrowDownloadRegular } from "@fluentui/react-icons"
import { FundFilters } from 'components/fund/filters/FundFilters';
import { FundPageRibbon } from 'components/fund/FundPageRibbon';
import { FundPager } from 'components/fund/FundPager';
import { MultiFundActionDialog } from 'components/fund/MultiFundActionDialog';
import { defineMessages, FormattedMessage, injectIntl } from 'react-intl';

const OUTPUT_MAX_NUMBER = 10;

const messages = defineMessages({
    fundPageExportResults: {
        id: "fundPage_export_results",
        defaultMessage: "Stáhnout CSV",
    },
    fundPageMultiActionTitle: {
        id: "fundPage.multiAction.title",
        defaultMessage: "Hromadná akce nad fondy",
    },
    fundPageMultiActionStart: {
        id: "fundPage.multiAction.start",
        defaultMessage: "Hromadná akce nad fondy",
    },
    fundPageMultiActionSelected: {
        id: "fundPage.multiAction.selected",
        defaultMessage: "Vybráno: {count}",
    },
    fundPageMultiActionSelectAll: {
        id: "fundPage.multiAction.selectAll",
        defaultMessage: "Vybrat vše odpovídající filtru ({count})",
    },
    fundPageMultiActionRun: {
        id: "fundPage.multiAction.run",
        defaultMessage: "Vybrat akci",
    },
    fundPageMultiActionCancel: {
        id: "fundPage.multiAction.cancel",
        defaultMessage: "Zrušit",
    },
    fundPageDrawerClose: {
        id: "fundPage.drawer.close",
        defaultMessage: "Zavřít",
    },
    fundPageDrawerRules: {
        id: "fundPage.drawer.rules",
        defaultMessage: "Pravidla",
    },
    fundPageDrawerOutputs: {
        id: "fundPage.drawer.outputs",
        defaultMessage: "Výstupy",
    },
    fundPageDrawerAllOutputs: {
        id: "fundPage.drawer.allOutputs",
        defaultMessage: "Všechny výstupy...",
    },
    fundPageDrawerVersions: {
        id: "fundPage.drawer.versions",
        defaultMessage: "Verze",
    },
    fundPageDrawerCurrentVersion: {
        id: "fundPage.drawer.currentVersion",
        defaultMessage: "Aktuální verze",
    },
    fundPageDrawerEntityScopes: {
        id: "fundPage.drawer.entityScopes",
        defaultMessage: "Oblasti entit",
    },
})


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

    state = {
        institutions: [],
        sidebarOpen: false,
        selectionMode: false,
        selectedFundIds: [],
        selectAllMatching: false,
    };

    constructor(props) {
        super(props);

        this.bindMethods(
            'handleAddFund',
            'handleImport',
            'handleExportDialog',
            'renderListItem',
            'handleSelect',
            'handleSearchClear',
            'handleApproveFundVersion',
            'handleEditFundVersion',
            'handleCallEditFundVersion',
            'handleDeleteFund',
            'handleDeleteFundHistory',
            'handleRuleSetUpdateFundVersion',
        );

        this.buildRibbon = this.buildRibbon.bind(this);
        WebApi.getInstitutions(true).then(institutions => this.setState({ institutions }));
    }

    UNSAFE_componentWillReceiveProps() {
        this.props.dispatch(fundsFetchIfNeeded());
        this.props.dispatch(fundsFundDetailFetchIfNeeded());
        this.props.dispatch(refInstitutionsFetchIfNeeded());
        this.props.dispatch(refRuleSetFetchIfNeeded());
    }

    componentDidMount() {
        const { dispatch, fundRegion, history, select = false } = this.props;
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

                this.handleToggleDrawer(true)
            }
        }
    }

    handleAddFund() {
        const { userDetail } = this.props;
        let initData = {};
        if (!userDetail.hasOne(perms.ADMIN, perms.FUND_ADMIN)) {
            initData.fundAdmins = [{ id: 'default', user: userDetail }];
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
                        includeUUID: true,
                        includeAccessPoints: true,
                        includeDaos: true
                    }}
                    onSubmitForm={({ exportFilter, includeUUID, includeAccessPoints, includeDaos }) => {
                        return dispatch(exportFund(fundDetail.versionId, { exportFilter, includeUUID, includeAccessPoints, includeDaos }));
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
        const { institutionsAll } = this.props;
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
        const { ruleSet, institutionsAll } = this.props;
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
        const { dispatch } = this.props;

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
        const { dispatch } = this.props;
        const fundDetail = await getFundDetail(fundId);

        const response = await dispatch(showConfirmDialog(i18n('arr.fund.action.delete.confirm', fundDetail.name)));
        if (response) {
            dispatch(deleteFund(fundDetail.id));
        }
    }

    async handleDeleteFundHistory(fundId) {
        const { dispatch } = this.props;
        const fundDetail = await getFundDetail(fundId);

        const response = await dispatch(showConfirmDialog(i18n('arr.fund.action.deletehistory.confirm', fundDetail.name)));
        if (response) {
            dispatch(deleteFundHistory(fundDetail.id));
        }
    }

    copyToClipboard = async (string) => {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(string);
        }
    };

    handleIssuesSettings = async (fundId) => {
        const { dispatch } = this.props;
        const fundDetail = await getFundDetail(fundId);

        dispatch(
            modalDialogShow(this, i18n('arr.issues.settings.title'), <IssueLists fundId={fundDetail.id} />),
        );
    };

    async handleShowInArr(item) {
        const { dispatch } = this.props;

        // Load fund detail
        const { id, versions } = await getFundDetail(item.id);

        dispatch(globalFundTreeInvalidate());
        // redirecting to arrangement page of the loaded fund
        dispatch(routerNavigate(urlFundTree(id, versions[0].id)));
    }

    renderListItem(props) {
        const { institutionsAll, userDetail } = this.props;
        const { institutions, selectionMode, selectedFundIds, selectAllMatching } = this.state;
        const { item } = props;
        // hide institution name, when only one is used for funds
        const institution = institutions?.length > 1 ? institutionsAll.items.find(({ code }) => code == item.institutionIdentifier) : undefined;

        const itemActions = [];
        if (item.id !== null) {
            if (userDetail.hasOne(perms.FUND_ADMIN, { type: perms.FUND_VER_WR, fundId: item.id })) {
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
            if (userDetail.hasOne(perms.FUND_ISSUE_ADMIN_ALL, { type: perms.FUND_ISSUE_ADMIN, fundId: item.id })) {
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
            if (userDetail.hasOne(perms.FUND_EXPORT_ALL, { type: perms.FUND_EXPORT, fundId: item.id })) {
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
            {selectionMode && item.id !== null &&
                <div
                    style={{ display: "flex", alignItems: "center", paddingRight: "8px", flexShrink: 0 }}
                    onMouseDown={(e) => e.stopPropagation()}
                    onClick={(e) => e.stopPropagation()}
                >
                    <Checkbox
                        checked={selectAllMatching || selectedFundIds.includes(item.id)}
                        disabled={selectAllMatching}
                        onChange={(_e, d) => this.handleToggleFundCheck(item.id, !!d.checked)}
                    />
                </div>
            }
            <div style={{ flexGrow: 1, flexShrink: 1, overflow: "hidden" }}>
                <div className="item-row" key={item.id}>
                    {selectionMode
                        ? <span className="name main" title={item.name} key={`fund-${item.id}`}>{item.name}</span>
                        : <Link className="name main link" title={item.name} key={`fund-${item.id}`} to={urlFundTree(item.id)} onMouseDown={(e) => e.stopPropagation()}>
                            {item.name}
                        </Link>
                    }
                    <div style={{ flexGrow: 1 }}></div>
                </div>
                <div className="item-row desc" key={item.id + '-x'}>
                    <div style={{ display: "flex", width: "100%" }}>
                        {/* <span className="desc-part id bubble" onClick={() => this.copyToClipboard(item.id)}>{item.id}</span> */}
                        {item.fundNumber != undefined && <span className="desc-part bubble internal-code" onClick={() => this.copyToClipboard(item.fundNumber)}>{item.fundNumber}</span>}
                        {item.internalCode && <span className="desc-part bubble" onClick={() => this.copyToClipboard(item.internalCode)}>{item.internalCode}</span>}
                        {item.mark && <span className="desc-part bubble" onClick={() => this.copyToClipboard(item.mark)}>{item.mark}</span>}
                        {institution && <Link className="name desc-part link bubble shrink" title={institution.name} key={`fund-${item.id}`} to={urlEntity(institution.accessPointId)} onMouseDown={(e) => e.stopPropagation()}>
                            <div>
                                {institution.name}
                            </div>
                        </Link>}
                        {item.unitdate && <span className="desc-part bubble id" onClick={() => this.copyToClipboard(item.unitdate)}>{item.unitdate}</span>}
                    </div>
                    <div style={{ flexGrow: 1 }}></div>
                </div>
            </div>
            <div style={{
                // flexGrow: 1,
                // flexShrink: 1,
                overflow: "hidden",
            }}>
                {/* <div style={{ display: "flex", justifyContent: "flex-end", columnGap: "10px" }}> */}
                {/*     <Link ><Icon glyph="fa-code-fork" /> 3</Link> */}
                {/*     <Link to={urlFundOutputs(item.id, item.versionId)}><Icon glyph="fa-print" /> 0</Link> */}
                {/* </div> */}
                <div style={{ display: "flex", justifyContent: "flex-end" }}>
                    {item.createDate && <span className="desc-part muted">vytvořeno: {new Date(item.createDate).toLocaleDateString()}, {new Date(item.createDate).toLocaleTimeString(undefined, { timeStyle: "short" })}</span>}
                </div>
            </div>
            <div className="fund-actions" onMouseDown={(e) => { e.stopPropagation() }}>
                {itemActions.length > 0 &&
                    <Menu>
                        <MenuTrigger disableButtonEnhancement={true}>
                            <MenuButton appearance='subtle' icon={<Icon glyph="fa-ellipsis-v" />} />
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
        const { history, dispatch } = this.props;
        const { selectionMode, selectAllMatching, selectedFundIds } = this.state;

        if (selectionMode) {
            if (item.id != null && !selectAllMatching) {
                this.handleToggleFundCheck(item.id, !selectedFundIds.includes(item.id));
            }
            return;
        }

        this.handleToggleDrawer(true)

        history.push(urlFund(item.id));
        dispatch(fundsSelectFund(item.id));
    }

    handleEnterSelectionMode = () => {
        this.setState({ selectionMode: true, selectedFundIds: [], selectAllMatching: false });
    };

    handleCancelSelection = () => {
        this.setState({ selectionMode: false, selectedFundIds: [], selectAllMatching: false });
    };

    handleToggleFundCheck = (fundId, checked) => {
        this.setState(({ selectedFundIds }) => {
            const set = new Set(selectedFundIds);
            if (checked) {
                set.add(fundId);
            } else {
                set.delete(fundId);
            }
            return { selectedFundIds: [...set] };
        });
    };

    handleToggleSelectAllMatching = (checked) => {
        this.setState({ selectAllMatching: checked });
    };

    handleRunMultiFund = () => {
        const { dispatch, fundRegion, intl } = this.props;
        const { selectAllMatching, selectedFundIds } = this.state;

        // Při výběru "vše odpovídající filtru" se předává aktivní filtr — fondy se
        // vyhodnocují až na serveru, jejich id se na klienta nikdy nestahují.
        let dialogProps;
        if (selectAllMatching) {
            dialogProps = { filters: fundRegion.filter.filter?.map((f) => f.getFilterValue(f)) ?? [] };
        } else {
            if (selectedFundIds.length === 0) {
                return;
            }
            dialogProps = { fundIds: selectedFundIds };
        }

        dispatch(
            modalDialogShow(
                this,
                intl.formatMessage(messages.fundPageMultiActionTitle),
                <MultiFundActionDialog {...dialogProps} />,
            ),
        );
    };

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
        const { filter } = this.props.fundRegion;
        let { from } = filter;

        if (from >= DEFAULT_FUND_LIST_MAX_SIZE) {
            from = from - DEFAULT_FUND_LIST_MAX_SIZE;
            this.props.dispatch(fundsFilter({ ...filter, from }));
        }
    };

    handleFilterNext = () => {
        const { filter, fundsCount } = this.props.fundRegion;
        let { from } = filter;

        if (from < fundsCount - DEFAULT_FUND_LIST_MAX_SIZE) {
            from = from + DEFAULT_FUND_LIST_MAX_SIZE;
            this.props.dispatch(fundsFilter({ ...filter, from }));
        }
    };

    handleFilterInstitution = institutionIdentifier => {
        const { filter } = this.props.fundRegion;

        if (institutionIdentifier !== filter.institutionIdentifier) {
            this.props.dispatch(fundsFilter({ ...filter, institutionIdentifier }));
        }
    };

    handleToggleDrawer = (state) => {
        const { sidebarOpen } = this.state;
        this.setState({ sidebarOpen: state !== undefined ? state : !sidebarOpen });
    }

    handleFiltersChange = (filters) => {
        const { dispatch, fundRegion } = this.props;
        dispatch(fundsFilter({ ...fundRegion.filter, filter: filters, from: 0 }));
    }

    handleExportResults = () => {
        const { dispatch } = this.props;
        dispatch(fundsExportResults());
    }

    render() {
        const { splitter, fundRegion, maxSize, ruleSet, userDetail, intl } = this.props;
        const { sidebarOpen, selectionMode, selectedFundIds, selectAllMatching } = this.state;

        let activeIndex;
        if (fundRegion.fundDetail.id !== null) {
            activeIndex = indexById(fundRegion.funds, fundRegion.fundDetail.id);
        }

        const activeVersion = fundRegion.fundDetail.versions?.find(({ id }) => fundRegion.fundDetail.activeVersion.id === id);
        const activeRuleSet = activeVersion?.ruleSetId != undefined ? ruleSet.itemsMap[activeVersion.ruleSetId] : undefined;

        const leftPanel = (
            <div className="fund-list-container">
                <div className="filter-container" style={{ display: "flex" }}>
                    <FundPager
                        onPrevious={this.handleFilterPrev}
                        onNext={this.handleFilterNext}
                        from={fundRegion.filter.from}
                        pageSize={maxSize}
                        totalCount={fundRegion.fundsCount}
                    />
                    <FundFilters currentFilters={fundRegion.filter.filter} onChange={this.handleFiltersChange} />
                    <div style={{ margin: "5px", flexShrink: 0 }} onClick={this.handleExportResults}>
                        <Button icon={<ArrowDownloadRegular />}>
                            <FormattedMessage {...messages.fundPageExportResults} />
                        </Button>
                    </div>
                    {userDetail.hasOne(perms.FUND_BA_ALL) && !selectionMode &&
                        <div style={{ margin: "5px", flexShrink: 0 }}>
                            <Button onClick={this.handleEnterSelectionMode}>
                                <FormattedMessage {...messages.fundPageMultiActionStart} />
                            </Button>
                        </div>
                    }
                </div>
                {selectionMode &&
                    <div className="filter-container" style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                        <span style={{ flexShrink: 0 }}>
                            <FormattedMessage
                                {...messages.fundPageMultiActionSelected}
                                values={{ count: selectAllMatching ? fundRegion.fundsCount : selectedFundIds.length }}
                            />
                        </span>
                        <Checkbox
                            label={intl.formatMessage(messages.fundPageMultiActionSelectAll, {
                                count: fundRegion.fundsCount,
                            })}
                            checked={selectAllMatching}
                            onChange={(_e, d) => this.handleToggleSelectAllMatching(!!d.checked)}
                        />
                        <div style={{ flexGrow: 1 }} />
                        <Button
                            appearance="primary"
                            disabled={!selectAllMatching && selectedFundIds.length === 0}
                            onClick={this.handleRunMultiFund}
                        >
                            <FormattedMessage {...messages.fundPageMultiActionRun} />
                        </Button>
                        <Button onClick={this.handleCancelSelection}>
                            <FormattedMessage {...messages.fundPageMultiActionCancel} />
                        </Button>
                    </div>
                }
                <div style={{ position: "relative", display: "flex", flexGrow: 1, flexShrink: 1, height: "400px" }}>
                    <div style={{ display: "flex", flexDirection: "column", flexGrow: 1, overflow: "hidden" }}>
                        <ListBox
                            className="fund-listbox"
                            ref="fundList"
                            items={fundRegion.funds}
                            activeIndex={activeIndex}
                            renderItemContent={this.renderListItem}
                            // onFocus={this.handleSelect}
                            onSelect={this.handleSelect}
                        />
                    </div>
                    <InlineDrawer className="drawer" position='end' separator={true} open={sidebarOpen} size='small' style={{ height: "auto", flexShrink: 0 }}>
                        <DrawerHeader>
                            <DrawerHeaderTitle
                                action={
                                    <Button
                                        appearance="subtle"
                                        aria-label={intl.formatMessage(messages.fundPageDrawerClose)}
                                        icon={<Dismiss24Regular />}
                                        onClick={() => this.handleToggleDrawer(false)}
                                    />
                                }
                            >
                                <Link
                                    className="name main link"
                                    title={fundRegion.fundDetail.name}
                                    key={`fund-${fundRegion.fundDetail.id}`}
                                    to={urlFundTree(fundRegion.fundDetail.id)}
                                    onMouseDown={(e) => e.stopPropagation()}
                                >
                                    {fundRegion.fundDetail.name}
                                </Link>
                            </DrawerHeaderTitle>
                        </DrawerHeader>
                        <DrawerBody style={{ overflow: "auto" }}>
                            <div style={{ marginBottom: "10px" }}>
                                {activeRuleSet && <>
                                    <div><b><FormattedMessage {...messages.fundPageDrawerRules} /></b></div>
                                    {activeRuleSet?.name}
                                </>}
                                <div><b><FormattedMessage {...messages.fundPageDrawerOutputs} /></b></div>
                                {/* <div style={{overflow: "auto"}}> */}
                                {fundRegion.fundDetail.validNamedOutputs?.slice(0, OUTPUT_MAX_NUMBER).sort((a, b) => new Date(b.generatedDate) - new Date(a.generatedDate)).map(({ name, id, generatedDate }) => {
                                    return <div key={id} style={{ marginBottom: "5px" }}>
                                        <div>
                                            <Link to={urlFundOutputs(fundRegion.fundDetail.id, undefined, id)}>
                                                {name}
                                            </Link>
                                        </div>
                                        {generatedDate && <div>
                                            {new Date(generatedDate).toLocaleDateString()}, {new Date(generatedDate).toLocaleTimeString()}
                                        </div>}
                                    </div>
                                })}
                                <div>
                                    <Link to={urlFundOutputs(fundRegion.fundDetail.id)}>
                                        <FormattedMessage {...messages.fundPageDrawerAllOutputs} />
                                    </Link>
                                </div>
                                {/* </div> */}
                            </div>
                            <div style={{ marginBottom: "10px" }}>
                                <div><b><FormattedMessage {...messages.fundPageDrawerVersions} /></b></div>
                                {fundRegion.fundDetail.versions?.map(({ lockDate, id }) => {
                                    return <div key={id} style={{ fontWeight: fundRegion.fundDetail.versionId === id ? "bold" : undefined }}>
                                        <Link to={urlFundTree(fundRegion.fundDetail.id, lockDate ? id : undefined)}>
                                            {lockDate ? <>
                                                {new Date(lockDate).toLocaleDateString()}
                                                {", "}
                                                {new Date(lockDate).toLocaleTimeString()}
                                            </> : <>
                                                <FormattedMessage {...messages.fundPageDrawerCurrentVersion} />
                                            </>}
                                        </Link>
                                    </div>
                                })}
                            </div>
                            <div style={{ marginBottom: "10px" }}>
                                <div><b><FormattedMessage {...messages.fundPageDrawerEntityScopes} /></b></div>
                                {fundRegion.fundDetail.apScopes?.map(({ name }) => {
                                    return <div key={name}>
                                        {name}
                                    </div>
                                })}
                            </div>
                        </DrawerBody>
                    </InlineDrawer>
                </div>
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
    const { focus, splitter, fundRegion, userDetail, refTables } = state;

    return {
        focus,
        splitter,
        fundRegion,
        userDetail,
        ruleSet: refTables.ruleSet,
        institutionsAll: refTables.institutions,
    };
}

export default withRouter(connect(mapStateToProps)(injectIntl(FundPage)));
