/**
 * Stránka výstupů.
 */

import classNames from "classnames";
import './ArrOutputPage.less';

import React from 'react';
import * as Utils from "components/Utils.jsx";
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {
    Ribbon,
    FundNodesSelectForm,
    FundNodesList,
    ArrOutputDetail,
    AddOutputForm,
    FundOutputFiles,
    FundOutputFunctions,
    RunActionForm,
    FormInput,
    ArrFundPanel
} from 'components/index.jsx';
import {
    ListBox,
    Loading,
    RibbonGroup,
    StoreHorizontalLoader,
    Icon,
    i18n,
    Tabs,
    AbstractReactComponent
} from 'components/shared';
import {Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import PageLayout from "../shared/layout/PageLayout";
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {canSetFocus, setFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {
    fundOutputFetchIfNeeded,
    fundOutputRemoveNodes,
    fundOutputSelectOutput,
    fundOutputCreate,
    fundOutputUsageEnd,
    fundOutputDelete,
    fundOutputAddNodes,
    fundOutputGenerate,
    fundOutputRevert,
    fundOutputClone,
    fundOutputFilterByState
} from 'actions/arr/fundOutput.jsx'
import {fundOutputActionRun} from 'actions/arr/fundOutputFunctions.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {fundActionFormShow, fundActionFormChange} from 'actions/arr/fundAction.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {templatesFetchIfNeeded} from 'actions/refTables/templates.jsx'
import AddDescItemTypeForm from 'components/arr/nodeForm/AddDescItemTypeForm.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'
import {outputTypesFetchIfNeeded} from "actions/refTables/outputTypes.jsx";
import {getDescItemsAddTree, getOneSettings} from 'components/arr/ArrUtils.jsx';
import ArrParentPage from "./ArrParentPage.jsx";

import {Shortcuts} from 'react-shortcuts';

let _selectedTab = 0

const OutputState = {
    OPEN: 'OPEN',
    COMPUTING: 'COMPUTING',
    GENERATING: 'GENERATING',
    FINISHED: 'FINISHED',
    OUTDATED: 'OUTDATED',
    ERROR: 'ERROR' /// Pomocný stav websocketu
};

const ArrOutputPage = class ArrOutputPage extends ArrParentPage {
    constructor(props) {
        super(props, "arr-output-page");

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
            'renderOutputPanel',
            'handleTabSelect',
            'handleGenerateOutput',
            'handleAddDescItemType',
            'handleRevertToOpen',
            'handleClone',
            'handleOutputStateSearch',
            'isEditable'
        );
    }

    componentDidMount() {
        super.componentDidMount();
        this.dispatch(templatesFetchIfNeeded());

        const fund = this.getActiveFund(this.props);
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
            this.dispatch(outputTypesFetchIfNeeded());
        }
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        super.componentWillReceiveProps(nextProps);
        this.dispatch(templatesFetchIfNeeded());

        const fund = this.getActiveFund(nextProps);
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
            this.dispatch(outputTypesFetchIfNeeded());
        }
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'fund-output', 1)) {
                this.refs.fundOutputList && this.setState({}, () => {
                    ReactDOM.findDOMNode(this.refs.fundOutputList).focus()
                })
                focusWasSet()
            }
        }
    }

    requestValidationData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, false))
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts ArrOutputPage", '[' + action + ']', this);
        switch (action) {
            case 'newOutput':
                this.handleAddOutput();
                break;
            case 'area1':
                this.dispatch(setFocus('fund-output', 1));
                break;
            case 'area2':
                this.dispatch(setFocus('fund-output', 2));
                break;
            case 'area3':
                this.dispatch(setFocus('fund-output', 3));
                break
            default:
                super.handleShortcuts(action);
        }
    }

    handleAddOutput() {
        const fund = this.getActiveFund(this.props);

        this.dispatch(modalDialogShow(this, i18n('arr.output.title.add'),
            <AddOutputForm
                create
                onSubmitForm={(data) => {return this.dispatch(fundOutputCreate(fund.versionId, data))}}/>));
    }

    handleBulkActions() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        this.dispatch(fundActionFormChange(fund.versionId, {nodes: fundOutputDetail.outputDefinition.nodes}));
        this.dispatch(fundActionFormShow(fund.versionId));
        this.dispatch(routerNavigate('/arr/actions'));
    }

    handleOtherActionDialog() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        this.dispatch(modalDialogShow(this, i18n('arr.output.title.add'),
            <RunActionForm versionId={fund.versionId} onSubmitForm={(data) => {
                return this.dispatch(fundOutputActionRun(fund.versionId, data.code));
            }}/>));
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
        const descItemTypes = getDescItemsAddTree(formData.descItemGroups, subNodeForm.infoTypesMap, subNodeForm.refTypesMap, subNodeForm.infoGroups, strictMode);

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

        var submit = (data) => {
            this.dispatch(modalDialogHide());
            this.dispatch(outputFormActions.fundSubNodeFormDescItemTypeAdd(fund.versionId, null, data.descItemTypeId.id));
        };

        // Modální dialog
        var form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>;
        this.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
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

            const hasPersmission = userDetail.hasOne(perms.FUND_ADMIN, perms.FUND_OUTPUT_WR_ALL, {
                type: perms.FUND_OUTPUT_WR,
                fundId: fund.id
            });

            if (hasPersmission && !readMode && !closed) {
                altActions.push(
                    <Button key="add-output" onClick={this.handleAddOutput}><Icon glyph="fa-plus-circle"/>
                        <div><span className="btnText">{i18n('ribbon.action.arr.output.add')}</span></div>
                    </Button>
                )
                if (isDetailIdNotNull && !closed) {
                    altActions.push(
                        <Button key="generate-output" onClick={() => {this.handleGenerateOutput(outputDetail.id)}} disabled={!isDetailLoaded || !this.isOutputGeneratingAllowed(outputDetail.outputDefinition)}><Icon glyph="fa-youtube-play" />
                            <div><span className="btnText">{i18n('ribbon.action.arr.output.generate')}</span></div>
                        </Button>
                    )
                }
            }


            if (isDetailIdNotNull && isDetailLoaded && !readMode) {
                const runnable = !closed && outputDetail.outputDefinition.state !== OutputState.FINISHED && outputDetail.outputDefinition.state !== OutputState.OUTDATED;
                if (hasPersmission) {
                    if (runnable) {
                        itemActions.push(
                            <Button key="add-item" onClick={this.handleAddDescItemType}><Icon glyph="fa-plus-circle" /><div><span className="btnText">{i18n('ribbon.action.arr.output.item.add')}</span></div></Button>
                        )
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
                        <Button key="fund-output-delete" onClick={this.handleDelete} disabled={!isDetailLoaded}><Icon glyph="fa-trash"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.output.delete')}</span></div>
                        </Button>
                    );

                    if (outputDetail.outputDefinition.generatedDate && (outputDetail.outputDefinition.state === OutputState.FINISHED || outputDetail.outputDefinition.state === OutputState.OUTDATED)) {
                        itemActions.push(
                            <Button key="fund-output-revert" onClick={this.handleRevertToOpen} disabled={!isDetailLoaded}><Icon glyph="fa-undo"/>
                                <div><span className="btnText">{i18n('ribbon.action.arr.output.revert')}</span></div>
                            </Button>
                        );
                    }
                    itemActions.push(
                        <Button key="fund-output-clone" onClick={this.handleClone} disabled={!isDetailLoaded}><Icon glyph="fa-clone"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.output.clone')}</span></div>
                        </Button>
                    )
                }

                if (runnable && outputDetail.outputDefinition.nodes.length > 0) {
                    if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id})) { // právo na hromadné akce
                        itemActions.push(
                            <Button key="fund-output-other-action" onClick={this.handleOtherActionDialog}><Icon
                                glyph="fa-cog"/>
                                <div><span className="btnText">{i18n('ribbon.action.arr.output.otherAction')}</span>
                                </div>
                            </Button>
                        );
                        itemActions.push(
                            <Button key="fund-output-bulk-actions" onClick={this.handleBulkActions}><Icon
                                glyph="fa-cog"/>
                                <div><span className="btnText">{i18n('ribbon.action.arr.output.bulkActions')}</span>
                                </div>
                            </Button>
                        )
                    }
                }
            }
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr subMenu fundId={fund ? fund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    }

    isOutputGeneratingAllowed(outputDefinition) {
        return outputDefinition &&
            outputDefinition.outputResultId == null &&
            outputDefinition.state === OutputState.OPEN &&
            outputDefinition.templateId != null &&
            outputDefinition.nodes.length > 0
    }

    handleUsageEnd() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail
        if (confirm(i18n('arr.output.usageEnd.confirm'))) {
            this.dispatch(fundOutputUsageEnd(fund.versionId, fundOutputDetail.id))
        }
    }

    handleDelete() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail
        if (confirm(i18n('arr.output.delete.confirm'))) {
            this.dispatch(fundOutputDelete(fund.versionId, fundOutputDetail.id))
        }
    }

    renderListItem(item, isActive, index) {
        const {outputTypes} = this.props;
        const fund = this.getActiveFund(this.props);
        const fundOutput = fund.fundOutput

        var temporaryChanged = false

        const currTemporary = item.outputDefinition.temporary
        var prevTemporary = index - 1 >= 0 ? fundOutput.outputs[index - 1].outputDefinition.temporary : false

        var cls = {
            item: true,
            'temporary-splitter': currTemporary !== prevTemporary
        }
        const typeIndex = indexById(outputTypes, item.outputDefinition.outputTypeId);

        return (
            <div className={classNames(cls)}>
                <div className='name'>{item.outputDefinition.name}</div>
                <div className='type'>{i18n('arr.output.list.type', typeIndex !== null ? outputTypes[typeIndex].name : "")}</div>
                <div className='state'>{i18n('arr.output.list.state.label')} {i18n('arr.output.list.state.' + item.outputDefinition.state.toLowerCase())} {
                    item.outputDefinition.generatedDate && <span>({Utils.dateToString(new Date(item.outputDefinition.generatedDate))})</span>
                }</div>
                {item.lockDate ? <div>{Utils.dateTimeToString(new Date(item.lockDate))}</div> : <div>&nbsp;</div>}
            </div>
        )
    }

    handleSelect(item) {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundOutputSelectOutput(fund.versionId, item.id))
    }

    renderRightPanel(readMode, closed) {
        const {calendarTypes, descItemTypes} = this.props;
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        const fetched = fundOutputDetail.fetched && fundOutputDetail.subNodeForm.fetched && calendarTypes.fetched && descItemTypes.fetched;
        if (!fetched) {
            return null;
        }

        // Záložky a obsah aktuálně vybrané založky
        const items = [];
        let tabContent
        let tabIndex = 0

        items.push({id: tabIndex, title: i18n('arr.output.panel.title.function')});
        if (_selectedTab === tabIndex) tabContent = this.renderFunctionsPanel(readMode);
        tabIndex++;

        items.push({id: tabIndex, title: i18n('arr.output.panel.title.template')});
        if (_selectedTab === tabIndex) tabContent = this.renderTemplatesPanel();
        tabIndex++;

        items.push({id: tabIndex, title: i18n('arr.output.panel.title.output')});
        if (_selectedTab === tabIndex) tabContent = this.renderOutputPanel();
        tabIndex++;

        return (
            <div className="fund-output-right-panel-container">
                <Tabs.Container>

                    <Tabs.Tabs items={items}
                               activeItem={{id: _selectedTab}}
                               onSelect={this.handleTabSelect}
                    />
                    <Tabs.Content>
                        {tabContent}
                    </Tabs.Content>
                </Tabs.Container>
            </div>
        )
    }

    isEditable(item) {
        return !item.lockDate && item.outputDefinition && item.outputDefinition.state === OutputState.OPEN
    }

    renderLeftPanel(readMode, closed) {
        const fund = this.getActiveFund(this.props);
        const fundOutput = fund.fundOutput;

        let activeIndex = null;
        if (fundOutput.fundOutputDetail.id !== null) {
            activeIndex = indexById(fundOutput.outputs, fundOutput.fundOutputDetail.id)
        }

        const filterStates = [];
        for (const item in OutputState) {
            if (item === OutputState.ERROR) {
                continue;
            }
            filterStates.push(<option value={item} key={"state" + item}>{i18n('arr.output.list.state.' + item.toLocaleLowerCase())}</option>)
        }

        return (
            <div className="fund-output-list-container">
                <FormInput componentClass="select" onChange={this.handleOutputStateSearch} value={fundOutput.filterState}>
                    <option value={-1} key="no-filter">{i18n('arr.output.list.state.all')}</option>
                    {filterStates}
                </FormInput>
                <StoreHorizontalLoader store={fundOutput} />
                {fundOutput.fetched && <ListBox
                    className='fund-output-listbox'
                    ref='fundOutputList'
                    items={fundOutput.outputs}
                    activeIndex={activeIndex}
                    renderItemContent={this.renderListItem}
                    onFocus={this.handleSelect}
                    onSelect={this.handleSelect}
                />}
            </div>
        )
    }

    renderCenterPanel(readMode, closed) {
        const {arrRegion, calendarTypes, packetTypes, templates, rulDataTypes, descItemTypes, userDetail} = this.props;

        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        var packets = [];
        if (fund && arrRegion.packets[fund.id]) {
            packets = arrRegion.packets[fund.id].items;
        }

        return (
            <ArrOutputDetail
                readOnly={!this.isEditable(fundOutputDetail)}
                versionId={fund.versionId}
                fund={fund}
                calendarTypes={calendarTypes}
                descItemTypes={descItemTypes}
                packetTypes={packetTypes}
                templates={templates}
                packets={packets}
                rulDataTypes={rulDataTypes}
                userDetail={userDetail}
                fundOutputDetail={fundOutputDetail}
                readMode={readMode}
                closed={closed}
            />
        )
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasArrOutputPage(activeFund ? activeFund.id : null);
    }

    renderFunctionsPanel(readMode) {

        const activeFund = this.getActiveFund(this.props);
        const {userDetail} = this.props;
        if(!userDetail.hasFundActionPage(activeFund.id)){          //Pokud uživatel nemá oprávnění spouštět funkce
           return <span>{i18n("arr.output.functions.noPermissions")}</span>
        }

        const {fundOutput} = activeFund;
        return <FundOutputFunctions
            ref="fundOutputFunctions"
            readMode={readMode}
            versionId={activeFund.versionId}
            outputId={fundOutput.fundOutputDetail.id}
            outputState={fundOutput.fundOutputDetail.outputDefinition.state}
            {...fundOutput.fundOutputFunctions}
        />
    }

    renderTemplatesPanel() {
        return <div>{i18n('arr.output.panel.template.noSettings')}</div>
    }

    renderOutputPanel() {
        const activeFund = this.getActiveFund(this.props);
        const {fundOutput : {fundOutputDetail, fundOutputFiles}} = activeFund;
        if (fundOutputDetail.outputDefinition.outputResultId === null) {
            return <div>{i18n('arr.output.panel.files.notGenerated')}</div>
        }
        return <FundOutputFiles
            ref="fundOutputFiles"
            versionId={activeFund.versionId}
            outputResultId={fundOutputDetail.outputDefinition.outputResultId}
            {...fundOutputFiles}
        />
    }


    handleTabSelect(item) {
        _selectedTab = item.id;
        this.setState({});
    }

    handleGenerateOutput(outputId) {
        this.dispatch(fundOutputGenerate(outputId));
    }

    handleRevertToOpen() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        //if (confirm(i18n('arr.output.revert.confirm'))) {
            this.dispatch(fundOutputRevert(fund.versionId, fundOutputDetail.id));
        //}
    }

    handleClone() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        this.dispatch(fundOutputClone(fund.versionId, fundOutputDetail.id));
    }

    handleOutputStateSearch(e) {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundOutputFilterByState(fund.versionId, e.target.value));
    }

    handleFunctionsStateSearch(e) {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundOutputFilterByState(fund.versionId, e.target.value));
    }
};

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, focus, userDetail} = state
    return {
        splitter,
        arrRegion,
        focus,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        packetTypes: refTables.packetTypes,
        ruleSet: refTables.ruleSet,
        templates: refTables.templates,
        outputTypes: refTables.outputTypes.items,
    }
}

ArrOutputPage.propTypes = {
    splitter: React.PropTypes.object.isRequired,
    arrRegion: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired
};

export default connect(mapStateToProps)(ArrOutputPage);
