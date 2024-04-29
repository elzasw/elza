/**
 * Stránka výstupů.
 */

import classNames from 'classnames';
import './ArrOutputPage.scss';

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils';
import {connect} from 'react-redux';
import {
    AddOutputForm,
    ArrOutputDetail,
    FormInput,
    FundOutputFunctions,
    Ribbon,
    RunActionForm,
} from '../../components/index';
import {i18n, Icon, ListBox, RibbonGroup, StoreHorizontalLoader, Tabs, Utils} from 'components/shared';
import {Button} from '../../components/ui';
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog';
import {canSetFocus, focusWasSet, isFocusFor, setFocus} from 'actions/global/focus';
import {
    fundOutputClone,
    fundOutputCreate,
    fundOutputDelete,
    fundOutputFetchIfNeeded,
    fundOutputFilterByState,
    fundOutputGenerate,
    fundOutputRevert,
    fundOutputSelectOutput,
    fundOutputSend,
    fundOutputUsageEnd,
} from '../../actions/arr/fundOutput';
import {fundOutputActionRun} from 'actions/arr/fundOutputFunctions';
import * as perms from 'actions/user/Permission';
import {fundActionFormChange, fundActionFormShow} from 'actions/arr/fundAction';
import {routerNavigate} from 'actions/router';
import {templatesFetchIfNeeded} from 'actions/refTables/templates';
import AddDescItemTypeForm from 'components/arr/nodeForm/AddDescItemTypeForm';
import {outputFormActions} from 'actions/arr/subNodeForm';
import {outputTypesFetchIfNeeded} from 'actions/refTables/outputTypes';
import {getDescItemsAddTree, getOneSettings} from 'components/arr/ArrUtils';
import ArrParentPage from './ArrParentPage';
import {PropTypes} from 'prop-types';
import defaultKeymap from './ArrOutputPageKeymap';

import TemplateSettingsForm from '../../components/arr/TemplateSettingsForm';
import {FOCUS_KEYS, urlFundActions, urlFundOutputs, getFundVersion} from '../../constants.tsx';
import FundNodesSelectForm from '../../components/arr/FundNodesSelectForm';
import {fundOutputAddNodes} from '../../actions/arr/fundOutput';
import {versionValidate} from '../../actions/arr/versionValidation';
import {structureTypesFetchIfNeeded} from '../../actions/refTables/structureTypes';
import * as outputFilters from "../../actions/refTables/outputFilters";
import { showConfirmDialog } from 'components/shared/dialog';

const OutputState = {
    OPEN: 'OPEN',
    COMPUTING: 'COMPUTING',
    GENERATING: 'GENERATING',
    FINISHED: 'FINISHED',
    OUTDATED: 'OUTDATED',
    ERROR: 'ERROR', /// Pomocný stav websocketu
};

const allowSendOutput = window.allowSendOutput !== undefined && window.allowSendOutput;
const outputStates = Object.values(OutputState).filter((state)=> state !== OutputState.ERROR)

const ArrOutputPage = class ArrOutputPage extends ArrParentPage {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    UNSAFE_componentWillMount() {
        let newKeymap = Utils.mergeKeymaps(ArrParentPage.defaultKeymap, defaultKeymap);
        Utils.addShortcutManager(this, newKeymap);
    }

    refFundOutputFunctions = null;
    refFundOutputList = null;

    constructor(props) {
        super(props, 'arr-output-page');

        this.bindMethods(
            'renderListItem',
            'handleSelect',
            'trySetFocus',
            'handleShortcuts',
            'handleAddOutput',
            'handleUsageEnd',
            'handleDelete',
            'handleBulkActions',
            'handleOtherActionDialog',
            'renderRightPanel',
            'renderFunctionsPanel',
            'renderTemplatesPanel',
            'handleTabSelect',
            'handleGenerateOutput',
            'handleAddDescItemType',
            'handleRevertToOpen',
            'handleClone',
            'handleOutputStateSearch',
            'isEditable',
        );
    }

    state = {
        selectedTab: '0',
    };

    async componentDidMount() {
        super.componentDidMount()
        const {dispatch, match, history} = this.props;
        await this.resolveUrls();
        dispatch(templatesFetchIfNeeded());
        dispatch(outputFilters.fetchIfNeeded());
        dispatch(structureTypesFetchIfNeeded(null));

        const fund = this.getActiveFund(this.props);
        const matchId = match.params.outputId;
        const urlOutputId = matchId ? parseInt(matchId) : null;
        dispatch(outputTypesFetchIfNeeded());
        if (fund) {
            dispatch(fundOutputFetchIfNeeded(fund.versionId));
            const outputDetail = fund.fundOutput.fundOutputDetail;
            const outputId = outputDetail.id;
            if (urlOutputId == null && outputId != null) {
                history.replace(urlFundOutputs(fund.id, getFundVersion(fund), outputId));
            } else if (urlOutputId !== outputId) {
                dispatch(fundOutputSelectOutput(fund.versionId, urlOutputId));
                history.replace(urlFundOutputs(fund.id, getFundVersion(fund), urlOutputId));
            }
        }
        this.trySetFocus(this.props);
    }

    getPageUrl(fund) {
        return urlFundOutputs(fund.id, getFundVersion(fund));
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        super.UNSAFE_componentWillReceiveProps(nextProps);
        this.props.dispatch(templatesFetchIfNeeded());
        this.props.dispatch(outputFilters.fetchIfNeeded());

        const fund = this.getActiveFund(nextProps);
        if (fund) {
            this.props.dispatch(fundOutputFetchIfNeeded(fund.versionId));
            this.props.dispatch(outputTypesFetchIfNeeded());
        }
        this.trySetFocus(nextProps);
    }

    trySetFocus(props) {
        var {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, FOCUS_KEYS.FUND_OUTPUT, 1)) {
                this.refFundOutputList &&
                    this.setState({}, () => {
                        ReactDOM.findDOMNode(this.refFundOutputList).focus();
                    });
                focusWasSet();
            }
        }
    }

    requestValidationData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.props.dispatch(versionValidate(versionId, false));
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts ArrOutputPage', '[' + action + ']', this);
        e.preventDefault();
        switch (action) {
            case 'newOutput':
                this.handleAddOutput();
                break;
            case 'area1':
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_OUTPUT, 1));
                break;
            case 'area2':
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_OUTPUT, 2));
                break;
            case 'area3':
                this.props.dispatch(setFocus(FOCUS_KEYS.FUND_OUTPUT, 3));
                break;
            default:
                super.handleShortcuts(action, e);
        }
    }

    handleAddNodes = fundOutputDetail => {
        const fund = this.getActiveFund(this.props);

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fund.nodes.title.select'),
                <FundNodesSelectForm
                    onSubmitForm={(ids, nodes) => {
                        this.props.dispatch(fundOutputAddNodes(fund.versionId, fundOutputDetail.id, ids));
                    }}
                />,
            ),
        );
    };

    handleAddOutput() {
        const fund = this.getActiveFund(this.props);

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.output.title.add'),
                <AddOutputForm
                    create
                    onSubmitForm={data => {
                        return this.props.dispatch(fundOutputCreate(fund.versionId, data));
                    }}
                    onSubmitSuccess={data => {
                        this.handleAddNodes(data);
                    }}
                />,
            ),
        );
    }

    handleBulkActions() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        this.props.dispatch(fundActionFormChange(fund.versionId, {nodes: fundOutputDetail.nodes}));
        this.props.dispatch(fundActionFormShow(fund.versionId));
        this.props.dispatch(routerNavigate(urlFundActions(fund.id, getFundVersion(fund))));
    }

    handleOtherActionDialog() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        this.props.dispatch(
            modalDialogShow(
                this,
                i18n('arr.output.title.add'),
                <RunActionForm
                    versionId={fund.versionId}
                    onSubmitForm={data => {
                        return this.props.dispatch(fundOutputActionRun(fund.versionId, data.code));
                    }}
                />,
            ),
        );
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        const subNodeForm = fundOutputDetail.subNodeForm;

        let strictMode = fund.activeVersion.strictMode;

        let userStrictMode = getOneSettings(this.props.userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fund.id);
        if (userStrictMode && userStrictMode.value !== null) {
            strictMode = userStrictMode.value === 'true';
        }

        const formData = subNodeForm.formData;
        const descItemTypes = getDescItemsAddTree(
            formData.descItemGroups,
            subNodeForm.infoTypesMap,
            subNodeForm.refTypesMap,
            subNodeForm.infoGroups,
            strictMode,
        );

        // Zatím zakomentováno, možná se bude ještě nějak řadit - zatím není jasné podle čeho řadit - podle uvedení v yaml nebo jinak?
        // function typeId(type) {
        //     switch (type) {
        //         case "REQUIRED":
        //             return 0;
        //         case "RECOMMENDED":
        //             return 1;
        //         case "POSSIBLE":
        //             return 2;
        //         case "IMPOSSIBLE":
        //             return 99;
        //         default:
        //             return 3;
        //     }
        // }
        // Seřazení podle position
        // descItemTypes.sort((a, b) => typeId(a.type) - typeId(b.type));

        var submit = data => {
            this.props.dispatch(modalDialogHide());
            this.props.dispatch(
                outputFormActions.fundSubNodeFormDescItemTypeAdd(fund.versionId, null, data.descItemTypeId.id),
            );
        };

        // Modální dialog
        var form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit} />;
        this.props.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const {userDetail} = this.props;

        const fund = this.getActiveFund(this.props);
        var itemActions = [];
        var altActions = [];
        if (fund) {
            const outputDetail = fund.fundOutput.fundOutputDetail;
            const isDetailIdNotNull = outputDetail.id !== null;
            const isDetailLoaded = outputDetail.fetched && !outputDetail.isFetching;
            const isOutputGenerated = outputDetail.state === OutputState.FINISHED;

            const hasPersmission = userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_OUTPUT_WR_ALL, {
                type: perms.FUND_OUTPUT_WR,
                fundId: fund.id,
            });

            if (hasPersmission && !readMode && !closed) {
                altActions.push(
                    <Button key="add-output" onClick={this.handleAddOutput}>
                        <Icon glyph="fa-plus-circle" />
                        <div>
                            <span className="btnText">{i18n('ribbon.action.arr.output.add')}</span>
                        </div>
                    </Button>,
                );
                if (isDetailIdNotNull && !closed) {
                    altActions.push(
                        <Button
                            key="generate-output"
                            onClick={() => {
                                this.handleGenerateOutput(outputDetail.id);
                            }}
                            disabled={!isDetailLoaded || !this.isOutputGeneratingAllowed(outputDetail)}
                        >
                            <Icon glyph="fa-youtube-play" />
                            <div>
                                <span className="btnText">{i18n('ribbon.action.arr.output.generate')}</span>
                            </div>
                        </Button>,
                    );
                }
                if (isDetailIdNotNull && !closed && allowSendOutput) {
                    altActions.push(
                        <Button
                            key="generate-output"
                            onClick={() => {
                                this.handleSendOutput(outputDetail.id);
                            }}
                            disabled={!isDetailLoaded || !isOutputGenerated}
                        >
                            <Icon glyph="fa-send" />
                            <div>
                                <span className="btnText">{i18n('ribbon.action.arr.output.send')}</span>
                            </div>
                        </Button>,
                    );
                }
            }

            if (isDetailIdNotNull && isDetailLoaded && !readMode) {
                const runnable =
                    !closed &&
                    outputDetail.state !== OutputState.FINISHED &&
                    outputDetail.state !== OutputState.OUTDATED;
                if (hasPersmission) {
                    if (runnable) {
                        itemActions.push(
                            <Button key="add-item" onClick={this.handleAddDescItemType}>
                                <Icon glyph="fa-plus-circle" />
                                <div>
                                    <span className="btnText">{i18n('ribbon.action.arr.output.item.add')}</span>
                                </div>
                            </Button>,
                        );
                        /**
                         *  Skrytí tlačítka - pravděpodobně nebudeme verzovat
                         */
                        /*
                        itemActions.push(
                            <Button key="fund-output-usage-end" onClick={this.handleUsageEnd}><Icon glyph="fa-clock-o"/>
                                <div><span className="btnText">{i18n('ribbon.action.arr.output.usageEnd')}</span></div>
                            </Button>
                        );
                        */
                    }

                    itemActions.push(
                        <Button key="fund-output-delete" onClick={this.handleDelete} disabled={!isDetailLoaded}>
                            <Icon glyph="fa-trash" />
                            <div>
                                <span className="btnText">{i18n('ribbon.action.arr.output.delete')}</span>
                            </div>
                        </Button>,
                    );

                    if (
                        outputDetail.generatedDate &&
                        (outputDetail.state === OutputState.FINISHED || outputDetail.state === OutputState.OUTDATED)
                    ) {
                        itemActions.push(
                            <Button
                                key="fund-output-revert"
                                onClick={this.handleRevertToOpen}
                                disabled={!isDetailLoaded}
                            >
                                <Icon glyph="fa-undo" />
                                <div>
                                    <span className="btnText">{i18n('ribbon.action.arr.output.revert')}</span>
                                </div>
                            </Button>,
                        );
                    }
                    itemActions.push(
                        <Button key="fund-output-clone" onClick={this.handleClone} disabled={!isDetailLoaded}>
                            <Icon glyph="fa-clone" />
                            <div>
                                <span className="btnText">{i18n('ribbon.action.arr.output.clone')}</span>
                            </div>
                        </Button>,
                    );
                }

                if (runnable && outputDetail.nodes.length > 0) {
                    if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id})) {
                        // právo na hromadné akce
                        itemActions.push(
                            <Button key="fund-output-other-action" onClick={this.handleOtherActionDialog}>
                                <Icon glyph="fa-cog" />
                                <div>
                                    <span className="btnText">{i18n('ribbon.action.arr.output.otherAction')}</span>
                                </div>
                            </Button>,
                        );
                        itemActions.push(
                            <Button key="fund-output-bulk-actions" onClick={this.handleBulkActions}>
                                <Icon glyph="fa-cog" />
                                <div>
                                    <span className="btnText">{i18n('ribbon.action.arr.output.bulkActions')}</span>
                                </div>
                            </Button>,
                        );
                    }
                }
            }
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return <Ribbon arr subMenu versionId={getFundVersion(fund)} fundId={fund ? fund.id : null} altSection={altSection} itemSection={itemSection} />;
    }

    isOutputGeneratingAllowed(output) {
        return (
            output &&
            output.outputResultIds == null &&
            output.state === OutputState.OPEN &&
            output.templateIds != null &&
            output.templateIds.length > 0 &&
            output.nodes.length > 0
        );
    }

    async handleUsageEnd() {
        const {dispatch} = this.props;
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        const response = await dispatch(showConfirmDialog(i18n('arr.output.usageEnd.confirm')));
        if (response) {
            this.props.dispatch(fundOutputUsageEnd(fund.versionId, fundOutputDetail.id));
        }
    }

    async handleDelete() {
        const {dispatch} = this.props;
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        const response = await dispatch(showConfirmDialog(i18n('arr.output.delete.confirm')));
        if (response) {
            this.props.dispatch(fundOutputDelete(fund.versionId, fundOutputDetail.id));
        }
    }

    renderListItem(props) {
        const {item} = props;
        const {outputTypes} = this.props;

        const typeIndex = indexById(outputTypes, item.outputTypeId);

        return (
            <div className={classNames('item')}>
                <div className="name">{item.internalCode }{item.name}</div>
                <div className="type">
                    {i18n('arr.output.list.type', typeIndex !== null ? outputTypes[typeIndex].name : '')}
                </div>
                <div className="state">
                    {i18n('arr.output.list.state.label')} {i18n('arr.output.list.state.' + item.state.toLowerCase())}{' '}
                    {item.generatedDate && <span>({Utils.dateToString(new Date(item.generatedDate))})</span>}
                </div>
                {item.deleteDate ? <div>{Utils.dateTimeToString(new Date(item.deleteDate))}</div> : <div>&nbsp;</div>}
            </div>
        );
    }

    handleSelect(item) {
        const {dispatch, history} = this.props;
        const fund = this.getActiveFund(this.props);
        dispatch(fundOutputSelectOutput(fund.versionId, item.id));
        history.push(urlFundOutputs(fund.id, getFundVersion(fund), item.id));
    }

    renderRightPanel(readMode, closed) {
        const {descItemTypes, userDetail} = this.props;
        const fund = this.getActiveFund(this.props);

        if (!userDetail.hasFundActionPage(fund.id)) {
            //Pokud uživatel nemá oprávnění spouštět funkce
            return null;
        }
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        const fetched =
            fundOutputDetail.fetched &&
            fundOutputDetail.subNodeForm.fetched &&
            descItemTypes.fetched;
        if (!fetched) {
            return null;
        }
        const {selectedTab} = this.state;

        // Záložky a obsah aktuálně vybrané založky
        const items = [];
        let tabContent;
        let tabIndex = 0;

        items.push({id: '' + tabIndex, title: i18n('arr.output.panel.title.function')});
        if (selectedTab === '' + tabIndex) tabContent = this.renderFunctionsPanel(readMode);
        tabIndex++;

        items.push({id: '' + tabIndex, title: i18n('arr.output.panel.title.template')});

        if (selectedTab === '' + tabIndex) tabContent = this.renderTemplatesPanel(readMode);
        tabIndex++;

        return (
            <div className="fund-output-right-panel-container">
                <Tabs.Container>
                    <Tabs.Tabs items={items} activeItem={{id: selectedTab}} onSelect={this.handleTabSelect} />
                    <Tabs.Content>{tabContent}</Tabs.Content>
                </Tabs.Container>
            </div>
        );
    }

    isEditable(item) {
        return !item.lockDate && item.state === OutputState.OPEN;
    }

    renderLeftPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const fundOutput = fund.fundOutput;


        let activeIndex = null;
        if (fundOutput.fundOutputDetail.id !== null) {
            activeIndex = indexById(fundOutput.outputs, fundOutput.fundOutputDetail.id);
        }

        return (
            <div className="fund-output-list-container">
                <FormInput type="select" onChange={this.handleOutputStateSearch} value={fundOutput.filterState || -1}>
                    <option value="" key="no-filter">
                        {i18n('arr.output.list.state.all')}
                    </option>
                    {outputStates.map((state)=>
                        <option value={state} key={'state' + state}>
                            {i18n('arr.output.list.state.' + state.toLocaleLowerCase())}
                        </option>
                    )}
                </FormInput>
                <StoreHorizontalLoader store={fundOutput} />
                {fundOutput.fetched && (
                    <ListBox
                        className="fund-output-listbox"
                        ref={ref => (this.refFundOutputList = ref)}
                        items={fundOutput.outputs}
                        activeIndex={activeIndex}
                        renderItemContent={this.renderListItem}
                        onFocus={this.handleSelect}
                        onSelect={this.handleSelect}
                    />
                )}
            </div>
        );
    }

    renderCenterPanel(readMode, closed) {
        const {templates, rulDataTypes, descItemTypes, userDetail, outputFilters} = this.props;

        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        return (
            <ArrOutputDetail
                readOnly={!this.isEditable(fundOutputDetail)}
                versionId={fund.versionId}
                fund={fund}
                descItemTypes={descItemTypes}
                templates={templates}
                outputFilters={outputFilters}
                rulDataTypes={rulDataTypes}
                userDetail={userDetail}
                fundOutputDetail={fundOutputDetail}
                readMode={readMode}
                closed={closed}
            />
        );
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }

    renderFunctionsPanel(readMode) {
        const activeFund = this.getActiveFund(this.props);
        const {userDetail} = this.props;
        if (!userDetail.hasFundActionPage(activeFund.id)) {
            //Pokud uživatel nemá oprávnění spouštět funkce
            return <span>{i18n('arr.output.functions.noPermissions')}</span>;
        }

        const {fundOutput} = activeFund;
        return (
            <FundOutputFunctions
                ref={ref => (this.refFundOutputFunctions = ref)}
                readMode={readMode}
                versionId={activeFund.versionId}
                outputId={fundOutput.fundOutputDetail.id}
                outputState={fundOutput.fundOutputDetail.state}
                fundOutputFunctions={fundOutput.fundOutputFunctions}
            />
        );
    }

    renderTemplatesPanel(readMode) {
        const {
            arrRegion,
            arrRegion: {activeIndex},
            templates,
        } = this.props;
        const templateIds = arrRegion.funds[activeIndex].fundOutput.fundOutputDetail.templateIds;
        const outputId = arrRegion.funds[activeIndex].fundOutput.fundOutputDetail.id;
        const outputSettings = JSON.parse(arrRegion.funds[activeIndex].fundOutput.fundOutputDetail.outputSettings);

        const template = templates.items.null.items.find(templateItem => {
            return templateIds.indexOf(templateItem.id) !== -1;
        });

        return (
            template &&
            outputId && (
                <TemplateSettingsForm
                    readMode={readMode}
                    outputId={outputId}
                    engine={template.engine}
                    initialValues={outputSettings}
                />
            )
        );
    }

    handleTabSelect(key) {
        this.setState({selectedTab: key.id});
    }

    handleGenerateOutput(outputId) {
        this.props.dispatch(fundOutputGenerate(outputId));
    }

    handleSendOutput(outputId) {
        this.props.dispatch(fundOutputSend(outputId));
    }

    handleRevertToOpen() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        //if (window.confirm(i18n('arr.output.revert.confirm'))) {
        this.props.dispatch(fundOutputRevert(fund.versionId, fundOutputDetail.id));
        //}
    }

    handleClone() {
        const { dispatch } = this.props;
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        // dispatch(outputFilters.invalidate());
        dispatch(fundOutputClone(fund.versionId, fundOutputDetail.id));
    }

    handleOutputStateSearch(e) {
        const fund = this.getActiveFund(this.props);
        this.props.dispatch(fundOutputFilterByState(fund.versionId, e.target.value));
    }

    handleFunctionsStateSearch(e) {
        const fund = this.getActiveFund(this.props);
        this.props.dispatch(fundOutputFilterByState(fund.versionId, e.target.value));
    }
};

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, focus, userDetail} = state;
    return {
        splitter,
        arrRegion,
        focus,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        templates: refTables.templates,
        outputFilters: refTables.outputFilters,
        outputTypes: refTables.outputTypes.items,
    };
}

ArrOutputPage.propTypes = {
    splitter: PropTypes.object.isRequired,
    arrRegion: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrOutputPage);
