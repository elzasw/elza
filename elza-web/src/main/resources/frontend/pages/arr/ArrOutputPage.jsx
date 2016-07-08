/**
 * Stránka výstupů.
 */

require('./ArrOutputPage.less');

import React from 'react';
import Utils from "components/Utils.jsx";
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {
    ListBox,
    Ribbon,
    Loading,
    RibbonGroup,
    FundNodesAddForm,
    Icon,
    FundNodesList,
    i18n,
    ArrOutputDetail,
    AddOutputForm,
    AbstractReactComponent,
    Tabs,
    FundOutputFiles
} from 'components/index.jsx';
import {Input, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
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
var classNames = require('classnames');
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');

var keyModifier = Utils.getKeyModifier();

var keymap = {
    ArrOutput: {
        area1: keyModifier + '1',
        area2: keyModifier + '2',
        area3: keyModifier + '3'
    }
};
var shortcutManager = new ShortcutsManager(keymap);

let _selectedTab = 0


const OutputState = {
    OPEN: 'OPEN',
    COMPUTING: 'COMPUTING',
    GENERATING: 'GENERATING',
    FINISHED: 'FINISHED',
    OUTDATED: 'OUTDATED',
    ERROR: 'ERROR' /// Pomocný stav websocketu
};

const ArrOutputPage = class ArrOutputPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'getActiveFund',
            'renderListItem',
            'handleSelect',
            'trySetFocus',
            'handleShortcuts',
            'handleAddOutput',
            'handleUsageEnd',
            'handleDelete',
            'handleBulkActions',
            'renderRightPanel',
            'renderFunctionsPanel',
            'renderTemplatesPanel',
            'renderOutputPanel',
            'handleTabSelect',
            'handleGenerateOutput',
            'handleAddDescItemType',
            'handleRevertToOpen',
            'handleClone',
            'handleStateSearch',
            'isEditable'
        );
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(templatesFetchIfNeeded());

        const fund = this.getActiveFund(this.props)
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
            this.dispatch(packetsFetchIfNeeded(fund.id));
            this.dispatch(outputTypesFetchIfNeeded());
        }
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        this.dispatch(templatesFetchIfNeeded());

        const fund = this.getActiveFund(nextProps)
        if (fund) {
            this.dispatch(fundOutputFetchIfNeeded(fund.versionId));
            this.dispatch(packetsFetchIfNeeded(fund.id));
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

    getActiveFund(props = this.props) {
        const arrRegion = props.arrRegion;
        
        if (arrRegion.activeIndex != null) {
            return arrRegion.funds[arrRegion.activeIndex];
        }
        return null;
    }

    requestValidationData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, false))
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'area1':
                this.dispatch(setFocus('fund-output', 1));
                break;
            case 'area2':
                this.dispatch(setFocus('fund-output', 2));
                break;
            case 'area3':
                this.dispatch(setFocus('fund-output', 3));
                break
        }
    }

    getChildContext() {
        return {shortcuts: shortcutManager};
    }

    handleAddOutput() {
        const fund = this.getActiveFund();

        this.dispatch(modalDialogShow(this, i18n('arr.output.title.add'),
            <AddOutputForm
                create
                onSubmitForm={(data) => {this.dispatch(fundOutputCreate(fund.versionId, data))}}/>));
    }

    handleBulkActions() {
        const fund = this.getActiveFund();
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;

        this.dispatch(fundActionFormChange(fund.versionId, {nodes: fundOutputDetail.outputDefinition.nodes}));
        this.dispatch(fundActionFormShow(fund.versionId));
        this.dispatch(routerNavigate('/arr/actions'));
    }

    /**
     * Zobrazení dialogu pro přidání atributu.
     */
    handleAddDescItemType() {
        const fund = this.getActiveFund(this.props);
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        const subNodeForm = fundOutputDetail.subNodeForm;

        const formData = subNodeForm.formData;

        // Pro přidání chceme jen ty, které zatím ještě nemáme
        var infoTypesMap = {...subNodeForm.infoTypesMap};
        formData.descItemGroups.forEach(group => {
            group.descItemTypes.forEach(descItemType => {
                delete infoTypesMap[descItemType.id];
            })
        })
        var descItemTypes = [];
        Object.keys(infoTypesMap).forEach(function (key) {
            descItemTypes.push({
                ...subNodeForm.refTypesMap[key],
                ...infoTypesMap[key],
            });
        });

        function typeId(type) {
            switch (type) {
                case "REQUIRED":
                    return 0;
                case "RECOMMENDED":
                    return 1;
                case "POSSIBLE":
                    return 2;
                case "IMPOSSIBLE":
                    return 99;
                default:
                    return 3;
            }
        }

        // Seřazení podle position
        descItemTypes.sort((a, b) => typeId(a.type) - typeId(b.type));
        var submit = (data) => {
            this.dispatch(modalDialogHide());
            this.dispatch(outputFormActions.fundSubNodeFormDescItemTypeAdd(fund.versionId, null, data.descItemTypeId));
        };
        // Modální dialog
        var form = <AddDescItemTypeForm descItemTypes={descItemTypes} onSubmitForm={submit} onSubmit2={submit}/>;
        this.dispatch(modalDialogShow(this, i18n('subNodeForm.descItemType.title.add'), form));
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        const {userDetail} = this.props;

        const fund = this.getActiveFund();
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

            if (hasPersmission) {
                altActions.push(
                    <Button key="add-output" onClick={this.handleAddOutput}><Icon glyph="fa-plus-circle"/>
                        <div><span className="btnText">{i18n('ribbon.action.arr.output.add')}</span></div>
                    </Button>
                )
                if (isDetailIdNotNull && !outputDetail.lockDate) {
                    altActions.push(
                        <Button key="generate-output" onClick={() => {this.handleGenerateOutput(outputDetail.id)}} disabled={!isDetailLoaded || !this.isOutputGeneratingAllowed(outputDetail.outputDefinition)}><Icon glyph="fa-youtube-play" />
                            <div><span className="btnText">{i18n('ribbon.action.arr.output.generate')}</span></div>
                        </Button>
                    )
                }
            }


            if (isDetailIdNotNull && isDetailLoaded) {
                if (hasPersmission) {
                    if (!outputDetail.lockDate && outputDetail.outputDefinition.state !== OutputState.FINISHED && outputDetail.outputDefinition.state !== OutputState.OUTDATED) {
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

                if (outputDetail.outputDefinition.nodes.length > 0) {
                    if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: fund.id})) { // právo na hromadné akce
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
            <Ribbon arr fundId={fund ? fund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    }

    isOutputGeneratingAllowed(outputDefinition) {
        return outputDefinition &&
            outputDefinition.outputResultId == null &&
            outputDefinition.state === OutputState.OPEN
    }

    handleUsageEnd() {
        const fund = this.getActiveFund()
        const fundOutputDetail = fund.fundOutput.fundOutputDetail
        if (confirm(i18n('arr.output.usageEnd.confirm'))) {
            this.dispatch(fundOutputUsageEnd(fund.versionId, fundOutputDetail.id))
        }
    }

    handleDelete() {
        const fund = this.getActiveFund()
        const fundOutputDetail = fund.fundOutput.fundOutputDetail
        if (confirm(i18n('arr.output.delete.confirm'))) {
            this.dispatch(fundOutputDelete(fund.versionId, fundOutputDetail.id))
        }
    }

    renderListItem(item, isActive, index) {
        const {outputTypes} = this.props;
        const fund = this.getActiveFund()
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
        const fund = this.getActiveFund()
        this.dispatch(fundOutputSelectOutput(fund.versionId, item.id))
    }

    renderRightPanel() {

        // Záložky a obsah aktuálně vybrané založky
        var items = [];
        var tabContent
        var tabIndex = 0

        items.push({id: tabIndex, title: i18n('arr.output.panel.title.function')});
        if (_selectedTab === tabIndex) tabContent = this.renderFunctionsPanel();
        tabIndex++;

        items.push({id: tabIndex, title: i18n('arr.output.panel.title.template')});
        if (_selectedTab === tabIndex) tabContent = this.renderTemplatesPanel();
        tabIndex++;

        items.push({id: tabIndex, title: i18n('arr.output.panel.title.output')});
        if (_selectedTab === tabIndex) tabContent = this.renderOutputPanel();
        tabIndex++;

        return (
            <Tabs.Container>

                <Tabs.Tabs items={items}
                           activeItem={{id: _selectedTab}}
                           onSelect={this.handleTabSelect}
                />
                <Tabs.Content>
                    {tabContent}
                </Tabs.Content>
            </Tabs.Container>
        )
    }

    isEditable(item) {
        return !item.lockDate && item.outputDefinition && item.outputDefinition.state === OutputState.OPEN
    }

    render() {
        const {focus, arrRegion, splitter, templates, userDetail, rulDataTypes, packetTypes, descItemTypes, calendarTypes} = this.props;

        const fund = this.getActiveFund();
        var leftPanel, rightPanel;
        let centerPanel;

        var packets = [];
        if (fund && arrRegion.packets[fund.id]) {
            packets = arrRegion.packets[fund.id].items;
        }

        if (userDetail.hasArrOutputPage(fund ? fund.id : null)) { // má právo na tuto stránku
            if (fund) {
                const fundOutput = fund.fundOutput;

                var activeIndex;
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
                leftPanel = (
                    <div className="fund-output-list-container">
                        <Input type="select" onChange={this.handleStateSearch} value={fundOutput.filterState}>
                            <option value={-1} key="no-filter">{i18n('arr.output.list.state.all')}</option>
                            {filterStates}
                        </Input>
                        <ListBox
                            className='fund-output-listbox'
                            ref='fundOutputList'
                            items={fundOutput.outputs}
                            activeIndex={activeIndex}
                            renderItemContent={this.renderListItem}
                            onFocus={this.handleSelect}
                            onSelect={this.handleSelect}
                        />
                    </div>
                );
                const fundOutputDetail = fund.fundOutput.fundOutputDetail;
                centerPanel = <ArrOutputDetail
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
                />;

                if (fundOutputDetail.id !== null && fundOutputDetail.fetched) {
                    rightPanel = (
                        <div className="fund-output-right-panel-container">{this.renderRightPanel()}</div>
                    )
                }
            } else {
                centerPanel = <div className="fund-noselect">{i18n('arr.fund.noselect')}</div>
            }
        } else {
            centerPanel = <div>{i18n('global.insufficient.right')}</div>
        }

        return (
            <Shortcuts name='ArrOutput' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    className='arr-output-page'
                    ribbon={this.buildRibbon()}
                    leftPanel={leftPanel}
                    centerPanel={centerPanel}
                    rightPanel={rightPanel}
                />
            </Shortcuts>
        )
    }

    renderFunctionsPanel() {

    }

    renderTemplatesPanel() {
        return <div>{i18n('arr.output.panel.template.noSettings')}</div>
    }

    renderOutputPanel() {
        const activeFund = this.getActiveFund();
        const {fundOutput : {fundOutputDetail, fundOutputFiles}} = activeFund;
        if (fundOutputDetail.outputDefinition.outputResultId === null) {
            return <div>{i18n('arr.output.panel.files.notGenerated')}</div>
        }
        if (fundOutputDetail.fetched) {
            return <FundOutputFiles
                ref="fundOutputFiles"
                versionId={activeFund.versionId}
                outputResultId={fundOutputDetail.outputDefinition.outputResultId}
                {...fundOutputFiles}
            />
        }
        return <Loading />
    }


    handleTabSelect(item) {
        _selectedTab = item.id;
        this.setState({});
    }

    handleGenerateOutput(outputId) {
        this.dispatch(fundOutputGenerate(outputId));
    }

    handleRevertToOpen() {
        const fund = this.getActiveFund();
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        //if (confirm(i18n('arr.output.revert.confirm'))) {
            this.dispatch(fundOutputRevert(fund.versionId, fundOutputDetail.id));
        //}
    }

    handleClone() {
        const fund = this.getActiveFund();
        const fundOutputDetail = fund.fundOutput.fundOutputDetail;
        this.dispatch(fundOutputClone(fund.versionId, fundOutputDetail.id));
    }

    handleStateSearch(e) {
        const fund = this.getActiveFund();
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

ArrOutputPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps)(ArrOutputPage);
