/**
 * Stránka archivních pomůcek.
 */

require('./ArrPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {FundSettingsForm, Tabs, Icon, Ribbon, i18n, ArrFundPanel} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

import ArrParentPage from "./ArrParentPage.jsx";

import {
    BulkActionsDialog,
    RibbonGroup,
    AbstractReactComponent,
    NodeTabs,
    ListBox2,
    LazyListBox,
    VisiblePolicyForm,
    Loading,
    FundPackets,
    FundFiles,
    FundTreeMain
} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, showDaosJp, fundExtendedView, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
import {Utils} from 'components/index.jsx';
import {isFundRootId, getSettings, setSettings, getOneSettings} from 'components/arr/ArrUtils.jsx';
import {setFocus} from 'actions/global/focus.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy.jsx'
import {fundActionFormChange, fundActionFormShow} from 'actions/arr/fundAction.jsx'
import {fundSelectSubNode} from 'actions/arr/nodes.jsx'
import {createFundRoot} from 'components/arr/ArrUtils.jsx'
import ArrHistoryForm from 'components/arr/ArrHistoryForm.jsx'
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {fundTreeFetchIfNeeded} from 'actions/arr/fundTree.jsx'
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {selectTab} from 'actions/global/tab.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'

const keyModifier = Utils.getKeyModifier()

const keymap = ArrParentPage.mergeKeymap({
    ArrParent: {
        registerJp: keyModifier + 'j',
        area1: keyModifier + '1',
        area2: keyModifier + '2',
        area3: keyModifier + '3',
    },
});

const shortcutManager = new ShortcutsManager(keymap)

class ArrPage extends ArrParentPage {
    static PropTypes = {
        splitter: React.PropTypes.object.isRequired,
        arrRegion: React.PropTypes.object.isRequired,
        developer: React.PropTypes.object.isRequired,
        rulDataTypes: React.PropTypes.object.isRequired,
        calendarTypes: React.PropTypes.object.isRequired,
        descItemTypes: React.PropTypes.object.isRequired,
        packetTypes: React.PropTypes.object.isRequired,
        focus: React.PropTypes.object.isRequired,
        userDetail: React.PropTypes.object.isRequired,
        ruleSet: React.PropTypes.object.isRequired,
    };

    state = {
        developerExpandedSpecsIds: {},
        fundNodesError: null,
        tabs: null
    };

    constructor(props) {
        super(props, "fa-page");

        this.bindMethods('getActiveInfo', 'buildRibbon', 'handleRegisterJp',
            'handleBulkActionsDialog', 'handleSelectVisiblePoliciesNode', 'handleShowVisiblePolicies',
            'handleShortcuts', 'renderFundErrors', 'renderFundVisiblePolicies', 'handleSetVisiblePolicy',
            'renderPanel', 'renderDeveloperDescItems', 'handleShowHideSpecs', 'handleTabSelect', 'handleSelectErrorNode',
            'renderFundPackets', 'handleErrorPrevious', 'handleErrorNext', 'trySetFocus', 'handleOpenFundActionForm',
            'handleChangeFundSettingsSubmit',
            "handleSetExtendedView"
        );
    }
    componentDidMount() {
        super.componentDidMount();
        this.trySetFocus(this.props)
    }
    componentWillMount(){
        this.registerTabs(this.props);
    }

    componentWillReceiveProps(nextProps) {
        super.componentWillReceiveProps(nextProps);
        const {tab} = this.props;
        let selected = tab.values['arr-as'];
        var activeFund = this.getActiveFund(nextProps);
        if (activeFund !== null) {
            if (selected === 'visiblePolicies') {
                this.dispatch(fundNodesPolicyFetchIfNeeded(activeFund.versionId));
            }
            if (nextProps.developer.enabled) {
                var node;
                if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
                    node = activeFund.nodes.nodes[activeFund.nodes.activeIndex]
                }
                if (!node) {
                    return
                }
                if (node.selectedSubNodeId !== null &&
                    node.subNodeForm.data !== null && !node.developerScenarios.isFetching &&
                    node.developerScenarios.isDirty
                ) {
                    this.dispatch(developerNodeScenariosRequest({
                        id: node.selectedSubNodeId,
                        key: node.routingKey,
                        version: node.subNodeForm.data.parent.version
                    }, activeFund.versionId));
                }
            }

            if (this.state.fundNodesError !== activeFund.fundNodesError) {
                this.setState({fundNodesError: activeFund.fundNodesError})
                if (this.refs.fundErrors) {
                    this.refs.fundErrors.fetchNow();
                }
            }
        } else {
            this.setState({fundNodesError: null});
        }
        this.registerTabs(nextProps);
        this.trySetFocus(nextProps);
    }
    wrappedFocus = (ref) => {
        if (this.refs[ref]) {
            this.setState({}, () => {
                if (this.refs[ref].getWrappedInstance().focus()) {
                    focusWasSet();
                }
            })
        }
    }
    refFocus = (ref) => {
        if(this.refs[ref]){
             this.setState({}, () => {
                ReactDOM.findDOMNode(this.refs[ref]).focus();
                focusWasSet();
            })
        }
    }
    trySetFocus(props) {
        var {focus, tab} = props
        if (this.state.tabs !== null && canSetFocus()) {
            if (isFocusFor(focus, 'arr', 3)) {
                let selectedTab = this.state.tabs[tab.values['arr-as']];
                if(!selectedTab.focus && !selectedTab.ref){ //Pokud tab nemá zadánu funkci pro focus ani ref
                    focusWasSet();
                } else if (!selectedTab.focus){ //Pokud tab nemá zadánu funkci pro focus
                    this.refFocus(selectedTab.ref);
                } else {
                    selectedTab.focus();
                }
            }
        }
    }

    requestValidationData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, false))
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts ArrPage", '[' + action + ']', this);
        switch (action) {
            case 'registerJp':
                this.handleRegisterJp()
                break
            case 'area1':
                this.dispatch(setFocus('arr', 1))
                break
            case 'area2':
                this.dispatch(setFocus('arr', 2))
                break
            case 'area3':
                this.dispatch(setFocus('arr', 3))
                break
            default:
                super.handleShortcuts(action);
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    /**
     * Načtení informačního objektu o aktuálním zobrazení sekce archvní pomůcky.
     * @return {Object} informace o aktuálním zobrazení sekce archvní pomůcky
     */
    getActiveInfo(from = this.props.arrRegion) {
        var arrRegion = from;
        var activeFund = null;
        var activeNode = null;
        var activeSubNode = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
            if (activeFund.nodes.activeIndex != null) {
                activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
                if (activeNode.selectedSubNodeId != null) {
                    var i = indexById(activeNode.childNodes, activeNode.selectedSubNodeId);
                    if (i != null) {
                        activeSubNode = activeNode.childNodes[i];
                    }
                }
            }
        }
        return {
            activeFund,
            activeNode,
            activeSubNode,
        }
    }

    /**
     * Zobrazení skrytí digitálních entit.
     */
    handleToggleDaos = () => {
        this.dispatch(showDaosJp(!this.props.arrRegion.showDaosJp));
    }

    /**
     * Zobrazení / skrytí záznamů u JP o rejstřících.
     */
    handleRegisterJp() {
        this.dispatch(showRegisterJp(!this.props.arrRegion.showRegisterJp));
    }

    handleBulkActionsDialog() {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.bulkActions'),
            <BulkActionsDialog mandatory={false}/>
            )
        );
    }

    handleOpenFundActionForm(versionId, subNode) {
        this.dispatch(fundActionFormChange(versionId, {nodes: [subNode]}));
        this.dispatch(fundActionFormShow(versionId));
        this.dispatch(routerNavigate('/arr/actions'));
    }
    /**
     *   getTabs
     *   Vrátí objekt obsahující identifikátor vybrané záložky a objekt obsahující objekty záložek.
     *
     *   @param tabs {Object} - Objekt obsahující vlastnosti záložek
     *   @param settingsValues {Object} - objekt s nastavením
     *   @param selectedTab {String} - id/key záložky
     *   @param ignoreSettings {Bool} - určuje, zda se bere v potaz nastavení. Při použití false vypíše všechny možné hodnoty splňující podmínku záložky.(určeno pro změnu nastavení)
     */
    getTabs = (tabs,settingsValues,selectedTab = null,ignoreSettings = false) => {
        var items = [];
        for(var tab in tabs){
            var tabCondition = typeof tabs[tab].condition === "undefined" || tabs[tab].condition; //Pokud podmínka není definována nebo je splněna
            var tabSettings = ignoreSettings || settingsValues === null || settingsValues[tabs[tab].id]; //Pokud se má nastavení ignorovat, neexistuje nebo je záložka zapnuta
            var showTabCondition = typeof tabs[tab].showCondition === "undefined" || tabs[tab].showCondition || ignoreSettings; //Pokud podmínka pro zobrazení není definována, je splněna, nebo se má ignorovat nastavení

            if(tabSettings && tabCondition){
                if(showTabCondition){
                        var checked = settingsValues && settingsValues[tab] !== undefined ? settingsValues[tab] : true;
                        tabs[tab].checked = checked;
                        items.push(tabs[tab]);
                        selectedTab = this.selectIfNull(selectedTab, tabs[tab].id);
                }
            }
        }
        return {items: items,selectedTab: selectedTab};
    }
    handleChangeFundSettings() {
        const {userDetail} = this.props;
        var activeInfo = this.getActiveInfo();

        let fundId = activeInfo.activeFund.id;

        var settings = getOneSettings(userDetail.settings, 'FUND_RIGHT_PANEL', 'FUND', fundId);
        var dataRight = settings.value ? JSON.parse(settings.value) : null;
        var settings = getOneSettings(userDetail.settings, 'FUND_CENTER_PANEL', 'FUND', fundId);
        var dataCenter = settings.value ? JSON.parse(settings.value) : null;

        var settings = getOneSettings(userDetail.settings, 'FUND_STRICT_MODE', 'FUND', fundId);
        var dataStrictMode = settings.value ? settings.value === 'true' : null;

        var tabsArray = this.getTabs(this.state.tabs,dataRight,null,true).items;

        var init = {
            rightPanel: {
                tabs: tabsArray
            },
            centerPanel: {
                panels: [
                    {
                        name: i18n('arr.fund.settings.panel.center.parents'),
                        key: 'parents',
                        checked: dataCenter && dataCenter.parents !== undefined ? dataCenter.parents : true},
                    {
                        name: i18n('arr.fund.settings.panel.center.children'),
                        key: 'children',
                        checked: dataCenter && dataCenter.children !== undefined ? dataCenter.children : true},
                    {
                        name: i18n('arr.fund.settings.panel.rightPanel'),
                        key: 'rightPanel',
                        checked: dataCenter && dataCenter.rightPanel !== undefined ? dataCenter.rightPanel : true},
                ]
            },
            strictMode: {
                    name: i18n('arr.fund.settings.rules.strictMode'),
                    key: 'strictMode',
                    value: dataStrictMode},
        };

        var form = <FundSettingsForm initialValues={init} onSubmitForm={this.handleChangeFundSettingsSubmit} />;
        this.dispatch(modalDialogShow(this, i18n('arr.fund.settings.title'), form));
    }

    handleChangeFundSettingsSubmit(data) {
        const {userDetail} = this.props;
        var activeInfo = this.getActiveInfo();
        let fundId = activeInfo.activeFund.id;

        var settings = userDetail.settings;

        var rightPanelItem = getOneSettings(settings, 'FUND_RIGHT_PANEL', 'FUND', fundId);
        var value = {};
        data.rightPanel.tabs.map((item) => {value[item.key] = item.checked;});
        rightPanelItem.value = JSON.stringify(value);
        settings = setSettings(settings, rightPanelItem.id, rightPanelItem);

        var centerPanelItem = getOneSettings(settings, 'FUND_CENTER_PANEL', 'FUND', fundId);
        var value = {};
        data.centerPanel.panels.map((item) => {value[item.key] = item.checked;});
        centerPanelItem.value = JSON.stringify(value);
        settings = setSettings(settings, centerPanelItem.id, centerPanelItem);

        let strictMode = getOneSettings(settings, 'FUND_STRICT_MODE', 'FUND', fundId);
        strictMode.value = data.strictMode.value === "" ? null : data.strictMode.value;
        settings = setSettings(settings, strictMode.id, strictMode);

        return this.dispatch(userDetailsSaveSettings(settings));
    }

    /**
     * Zobrazení formuláře historie.
     * @param versionId verze AS
     */
    handleShowFundHistory = (versionId, locked) => {
        const form = <ArrHistoryForm
            versionId={versionId}
            locked={locked}
            onDeleteChanges={this.handleDeleteChanges}
        />
        this.dispatch(modalDialogShow(this, i18n('arr.history.title'), form, "dialog-lg"));
    }

    handleDeleteChanges = (nodeId, fromChangeId, toChangeId) => {
        const activeFund = this.getActiveFund(this.props);
        const versionId = activeFund.versionId;
        WebApi.revertChanges(versionId, nodeId, fromChangeId, toChangeId)
            .then(() => {
                this.dispatch(modalDialogHide());
            });
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon(readMode, closed) {
        const {arrRegion, userDetail} = this.props;

        var activeInfo = this.getActiveInfo();

        var altActions = [];

        var itemActions = [];

        if (activeInfo.activeFund && !activeInfo.activeFund.closed) {
            altActions.push(
                <Button key="fas" onClick={()=>{}}><Icon glyph="fa-cogs"/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fund.fas')}</span></div>
                </Button>
            )
        }


        altActions.push(
            <Button active={this.props.arrRegion.showRegisterJp} onClick={this.handleRegisterJp} key="toggle-record-jp">
                <Icon glyph="fa-th-list"/>
                <span className="btnText">{i18n('ribbon.action.arr.show-register-jp')}</span>
            </Button>
        )
        altActions.push(
            <Button active={this.props.arrRegion.showDaosJp} onClick={this.handleToggleDaos} key="toggle-daos-jp">
                <Icon glyph="fa-th-list"/>
                <span className="btnText">{i18n('ribbon.action.arr.show-daos')}</span>
            </Button>
        )

        var indexFund = arrRegion.activeIndex;
        if (indexFund !== null) {
            var activeFund = arrRegion.funds[indexFund];

            altActions.push(
                <Button key="fund-settings" onClick={this.handleChangeFundSettings.bind(this)}>
                    <Icon glyph="fa-wrench"/>
                    <span className="btnText">{i18n('ribbon.action.arr.fund.settings.ui')}</span>
                </Button>);

            // Zobrazení historie změn
            if (userDetail.hasOne(perms.FUND_ADMIN, {type: perms.FUND_VER_WR, fundId: activeFund.id}, perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId: activeFund.id})) {
                altActions.push(
                    <Button onClick={() => this.handleShowFundHistory(activeFund.versionId, readMode)} key="show-fund-history">
                        <Icon glyph="fa-clock-o"/>
                        <div>
                            <span className="btnText">{i18n('ribbon.action.showFundHistory')}</span>
                        </div>
                    </Button>
                )
            }

            var nodeIndex = activeFund.nodes.activeIndex;
            if (nodeIndex !== null) {
                var activeNode = activeFund.nodes.nodes[nodeIndex];
                const activeNodeObj = getMapFromList(activeNode.allChildNodes)[activeNode.selectedSubNodeId];

                if (activeNode.selectedSubNodeId !== null) {
                    itemActions.push(
                        <Button key="next-error" onClick={this.handleErrorPrevious.bind(this, activeFund.versionId, activeNode.selectedSubNodeId)}>
                            <Icon glyph="fa-arrow-left"/>
                            <span className="btnText">{i18n('ribbon.action.arr.validation.error.previous')}</span>
                        </Button>,
                        <Button key="previous-error" onClick={this.handleErrorNext.bind(this, activeFund.versionId, activeNode.selectedSubNodeId)}>
                            <Icon glyph="fa-arrow-right"/>
                            <span className="btnText">{i18n('ribbon.action.arr.validation.error.next')}</span>
                        </Button>
                    );
                    if (userDetail.hasOne(perms.FUND_BA_ALL, {type: perms.FUND_BA, fundId: activeFund.id}) && !readMode) {
                        itemActions.push(
                            <Button key="prepareFundAction" onClick={this.handleOpenFundActionForm.bind(this, activeFund.versionId, activeInfo.activeSubNode)}>
                                <Icon glyph="fa-calculator"/>
                                <span className="btnText">{i18n('ribbon.action.arr.fund.newFundAction')}</span>
                            </Button>
                        );
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
            <Ribbon arr subMenu fundId={activeInfo.activeFund ? activeInfo.activeFund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    }

    handleErrorNext(versionId, nodeId) {
        this.dispatch(versionValidationErrorNext(versionId, nodeId));
    }

    handleErrorPrevious(versionId, nodeId) {
        this.dispatch(versionValidationErrorPrevious(versionId, nodeId));
    }

    handleShowHideSpecs(descItemTypeId) {
        if (this.state.developerExpandedSpecsIds[descItemTypeId]) {
            delete this.state.developerExpandedSpecsIds[descItemTypeId]
            this.setState({developerExpandedSpecsIds: this.state.developerExpandedSpecsIds})
        } else {
            this.state.developerExpandedSpecsIds[descItemTypeId] = true
            this.setState({developerExpandedSpecsIds: this.state.developerExpandedSpecsIds})
        }
    }

    renderFundErrors(activeFund) {
        var activeNode = null;
        if (activeFund.nodes.activeIndex != null) {
            activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        return (
            <div className="errors-listbox-container">
                <LazyListBox
                    ref="fundErrors"
                    getItems={(fromIndex, toIndex) => {
                                return WebApi.getValidationItems(activeFund.versionId, fromIndex, toIndex)
                            }}
                    renderItemContent={(item) => item !== null ? <div>{item.name}</div> : '...'}
                    selectedItem={activeNode ? activeNode.selectedSubNodeId : null}
                    itemHeight={25} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                    onSelect={this.handleSelectErrorNode.bind(this, activeFund)}
                />
            </div>
        )
    }

    handleSelectErrorNode(activeFund, node) {
        if (node.parentNode == null) {
            node.parentNode = createFundRoot(activeFund);
        }
        this.dispatch(fundSelectSubNode(activeFund.versionId, node.id, node.parentNode));
    }

    handleSelectVisiblePoliciesNode(activeFund, node) {
        if (node.parentNode == null) {
            node.parentNode = createFundRoot(activeFund);
        }
        this.dispatch(fundSelectSubNode(activeFund.versionId, node.id, node.parentNode));
    }

    handleShowVisiblePolicies(activeFund) {
        var node;
        if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex]
        }
        if (!node) {
            return;
        }
        var form = <VisiblePolicyForm nodeId={node.selectedSubNodeId} fundVersionId={activeFund.versionId} onSubmitForm={this.handleSetVisiblePolicy.bind(this, node, activeFund.versionId)} />;
        this.dispatch(modalDialogShow(this, i18n('visiblePolicy.form.title'), form));
    }

    handleSetVisiblePolicy(node, versionId, data) {
        var mapIds = {};
        data.records.forEach((val, index) => {
            mapIds[parseInt(val.id)] = val.checked;
        });
        return this.dispatch(setVisiblePolicyRequest(node.selectedSubNodeId, versionId, mapIds));
    }

    renderFundVisiblePolicies(activeFund) {
        var nodesPolicy = activeFund.fundNodesPolicy;

        if (!nodesPolicy.fetched) {
            return <Loading />
        }
        var activeNode = null;
        if (activeFund.nodes.activeIndex != null) {
            activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        if (nodesPolicy.items.length == 0) {
            return <div>{i18n('global.data.noitem')}</div>
        }

        return (
            <div className="visiblePolicies-container">
                <ListBox2 className="visiblePolicies-listbox"
                    ref="fundVisiblePolicies"
                    items={nodesPolicy.items}
                    selectedItem={activeNode !== null ? activeNode.selectedSubNodeId : null}
                    renderItemContent={(node, isActive) => <div>{node.name}</div>}
                    onSelect={this.handleSelectVisiblePoliciesNode.bind(this, activeFund)}
                    /*onDoubleClick={this.handleShowVisiblePolicies.bind(this, activeFund)}*/
                />
            </div>
        )
    }

    renderDeveloperDescItems(activeFund, node) {
        var rows = []
        if(!node){
            return <div className='developer-panel'>
                Je potřeba vybrat jednotku popisu.
            </div>
        }
        else if(node.subNodeForm.fetched) {
            node.subNodeForm.infoGroups.forEach(group => {
                var types = []
                group.types.forEach(type => {
                    var infoType = node.subNodeForm.infoTypesMap[type.id]
                    var refType = node.subNodeForm.refTypesMap[type.id]

                    var specs;
                    if (refType.useSpecification && this.state.developerExpandedSpecsIds[refType.id]) {
                        if (refType.descItemSpecs.length > 0) {
                            specs = refType.descItemSpecs.map(spec => {
                                var infoSpec = infoType.specs[indexById(infoType.specs, spec.id)]

                                return (
                                    <div key={'spec' + spec.id} className={'desc-item-spec ' +  infoSpec.type}>
                                        <h3 title={spec.name}>{spec.shortcut} <small>[{spec.code}]</small></h3>
                                        <div key='1' className='desc'>{spec.description}</div>
                                        <div key='2' className='attrs'>
                                            <div key='1'><label>type:</label>{infoSpec.type}</div>
                                            <div key='2'><label>repeatable:</label>{spec.repeatable ? i18n('global.title.yes') : i18n('global.title.no')}</div>
                                            <div key='3'><label>viewOrder:</label>{spec.viewOrder}</div>
                                        </div>
                                    </div>
                                )
                            })
                        } else {
                            specs = i18n('developer.descItems.specs.empty')
                        }
                        specs = <div>{specs}</div>
                    }

                    types.push(
                        <div key={'type' + refType.id} className={'desc-item-type ' + infoType.type}>
                            <h2 title={refType.name}>{refType.shortcut} <small>[{refType.code}]</small></h2>
                            <div key='1' className='desc'>{refType.description}</div>
                            <div key='2' className='attrs'>
                                <div key='1'><label>type:</label>{infoType.type}</div>
                                <div key='2'><label>repeatable:</label>{infoType.rep === 1 ? i18n('global.title.yes') : i18n('global.title.no')}</div>
                                <div key='3'><label>dataType:</label>{refType.dataType.code}</div>
                                <div key='4'><label>width:</label>{infoType.width}</div>
                                <div key='5'><label>viewOrder:</label>{refType.viewOrder}</div>
                                <div key='6'><label>isValueUnique:</label>{refType.isValueUnique ? i18n('global.title.yes') : i18n('global.title.no')}</div>
                                <div key='7'><label>canBeOrdered:</label>{refType.canBeOrdered ? i18n('global.title.yes') : i18n('global.title.no')}</div>
                            </div>
                            {refType.useSpecification && <Button onClick={this.handleShowHideSpecs.bind(this, refType.id)}>specifikace <Icon glyph={this.state.developerExpandedSpecsIds[refType.id] ? 'fa-angle-up' : 'fa-angle-down'}/></Button>}
                            {specs}
                        </div>
                    )
                })

                rows.push(
                    <div key={'group' + group.code}>
                        <h1 key='1'>{group.code}</h1>
                        <div key='2'>{types}</div>
                    </div>
                )
            })
        }

        return <div className='developer-panel'>
            <div className='desc-items-container'>{rows}</div>
        </div>
    }

    renderDeveloperScenarios(activeFund, node) {
        if(!node){
            return <div className='developer-panel'>
                Je potřeba vybrat jednotku popisu.
            </div>
        }
        if (node.developerScenarios.isFetching) {
            return <Loading />
        }

        let isRootNode = isFundRootId(node.id);

        var rows = [];
        for (var key in node.developerScenarios.data) {
            if (!node.developerScenarios.data.hasOwnProperty(key)
                || (isRootNode && (key === 'after' || key === 'before'))) {
                continue;
            }

            let obj = node.developerScenarios.data[key];

            let types = obj.map(data => (
                <div className="desc-item-type">
                    <h2>{data.name}</h2>
                    {data.groups.map(group => (
                        group.types.map(type => {
                            if (node.subNodeForm.infoTypesMap === null || node.subNodeForm.refTypesMap === null) {
                                return;
                            }
                            let infoTypes = node.subNodeForm.infoTypesMap[type.id];
                            let refTypes = node.subNodeForm.refTypesMap[type.id];
                            return type.descItems.map(item => {
                                let infoType = infoTypes.specs[indexById(infoTypes.specs, item.descItemSpecId)];
                                let refType = refTypes.descItemSpecsMap[item.descItemSpecId];
                                return <div>
                                    <h4 title={refTypes.name}>
                                        {refTypes.shortcut}
                                        <small>[{refTypes.code}]</small>
                                    </h4>
                                    <div>
                                        <label>value:</label>
                                        <span title={refType.name}>{refType.shortcut}</span> |
                                        <small>{infoType.rep}</small>
                                        |
                                        <small>[{refType.code}]</small>
                                    </div>
                                </div>
                            })
                        })
                    ))}
                </div>

            ));

            /** key = after, before, child */
            rows.push(
                <div>
                    <h1>{i18n('developer.scenarios.' + key)}</h1>
                    <div>{types}</div>
                </div>
            )
        }
        return <div className='developer-panel'>
            <div className='desc-items-container'>{rows}</div>
        </div>
    }

    handleTabSelect(item) {
        const {arrRegion, tab} = this.props;

        this.dispatch(selectTab('arr-as', item.id));
        if (item.update) {  //Pokud má záložka definovánu funkci update(), pak se tato funkce zavolá.
            item.update();
        }

    }
    /**
     *   selectIfNull
     *   Pokud není definována vybraná záložka, vrátí druhou předanou, pokud ne, vrátí zpět původní.
     *
     *   @param selectedTab {String} - id/key záložky
     *   @param tabId {String}- záložka k vybrání
     */
    selectIfNull = (selectedTab,tabId) => {
        if (!selectedTab) {
            return tabId;
        } else{
            return selectedTab;
        }
    }
    /**
    *   checkTabEnabled
    *   Zjišťuje, zda je vybraná záložka povolená. Pokud ano, vrátí jí zpátky, pokud ne, vrátí null.
    *
    *   @param selectedTab {String} - id/key záložky
    *   @param settingsValues {Object}- hodnoty nastavení
    */
    checkTabEnabled = (selectedTab, settingsValues) => {
        if(selectedTab){
            if(settingsValues){
                if(!settingsValues[selectedTab]){
                    return null;
                }
            }
        }
        return selectedTab;
    }
    /**
    *   registerTabs
    *   Funkce pro definici záložek pravého panelu. Záložky se uloží do state.
    *
    *   @param props {Object}
    */
    registerTabs = (props) => {
        const {developer, arrRegion, userDetail} = props;
        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
        var node;
        if (!activeFund) {
            return;
        }
        if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }
        /**
         *  Vlastnosti objektu záložky
         *
         *  @prop id {String} identifikátor záložky
         *  @prop key {String} identifikátor záložky
         *  @prop name {String} Nadpis záložky
         *  @prop ref {String} odkaz na záložku
         *  @func render() funkce pro vykreslení obsahu záložky
         *  @func focus() funkce pro získání focusu záložky
         *  @func update() funkce určená pro aktualizaci
         *  @prop condition {Bool} podmínka, která určuje jestli se záložka vykreslí
         *
         */
        var tabs = {
                    "packets":{
                        id: "packets",
                        key: "packets",
                        name: i18n('arr.panel.title.packets'),
                        ref: "fundPackets",
                        render:() => this.renderFundPackets(activeFund),
                        focus: () => this.wrappedFocus("fundPackets"),
                        condition: userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId: activeFund.id}),
                        permissionRestrictions: []
                    },
                    "files":{
                        id: "files" ,
                        key: "files" ,
                        name: i18n('arr.panel.title.files'),
                        ref: "fundFiles",
                        render:() => this.renderFundFiles(activeFund),
                        focus: () => this.wrappedFocus("fundFiles"),
                        condition: userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId: activeFund.id})
                    },
                    "discrepancies":{
                        id: "discrepancies" ,
                        key: "discrepancies" ,
                        ref: "fundErrors",
                        name: i18n('arr.panel.title.discrepancies'),
                        render:() => this.renderFundErrors(activeFund)
                    },
                    "visiblePolicies":{
                        id: "visiblePolicies",
                        key: "visiblePolicies",
                        ref: "fundVisiblePolicies",
                        name: i18n('arr.panel.title.visiblePolicies'),
                        render:() => this.renderFundVisiblePolicies(activeFund),
                        update:() => this.dispatch(fundNodesPolicyFetchIfNeeded(activeFund.versionId))
                    },
                    "descItems":{
                        id: "descItems" ,
                        key: "descItems" ,
                        name: i18n('developer.title.descItems'),
                        render:() => this.renderDeveloperDescItems(activeFund, node),
                        condition: developer.enabled,
                        showCondition: node ? true : false
                    },
                    "scenarios":{
                        id: "scenarios" ,
                        key: "scenarios" ,
                        name: i18n('developer.title.scenarios'),
                        render:() => this.renderDeveloperScenarios(activeFund, node),
                        condition: developer.enabled,
                        showCondition: node ? true : false
                    }
        };
        this.setState({tabs:tabs});
    }


    renderPanel() {
        const {arrRegion, userDetail, tab} = this.props;

        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        var settings = getOneSettings(userDetail.settings, 'FUND_RIGHT_PANEL', 'FUND', activeFund.id);
        var centerSettings = getOneSettings(userDetail.settings, 'FUND_CENTER_PANEL', 'FUND', activeFund.id);
        var settingsValues = settings.value ? JSON.parse(settings.value) : null;
        var centerSettingsValues = centerSettings.value ? JSON.parse(centerSettings.value) : null;
        var selectedTab =  this.checkTabEnabled(tab.values['arr-as'], settingsValues);

        var tabsObject = this.state.tabs;
        var tabs = this.getTabs(tabsObject,settingsValues,selectedTab);
        var tabsItems = tabs.items;
        selectedTab = tabs.selectedTab;

        if(!selectedTab || (centerSettingsValues && !centerSettingsValues.rightPanel)){ //pokud neexistuje žádná vybratelná záložka nebo je vypnutý pravý panel
            return false;
        }

        var tabContent = tabsObject[selectedTab].render();

        return (
            <Tabs.Container>
                <Tabs.Tabs items={tabsItems}
                           activeItem={{id: selectedTab}}
                           onSelect={this.handleTabSelect}
                />
                <Tabs.Content>
                    {tabContent}
                </Tabs.Content>
            </Tabs.Container>
        )
    }

    renderFundPackets() {
        const {arrRegion, packetTypes} = this.props;
        const activeFund = arrRegion.activeIndex !== null ? arrRegion.funds[arrRegion.activeIndex] : null;

        return (
            <FundPackets
                ref="fundPackets"
                versionId={activeFund.versionId}
                fundId={activeFund.id}
                packetTypes={packetTypes}
                {...activeFund.fundPackets}
            />
        )
    }

    renderFundFiles() {
        const {arrRegion} = this.props;
        const activeFund = arrRegion.activeIndex !== null ? arrRegion.funds[arrRegion.activeIndex] : null;

        return (
            <FundFiles
                ref="fundFiles"
                versionId={activeFund.versionId}
                fundId={activeFund.id}
                {...activeFund.fundFiles}
            />
        )
    }

    handleSetExtendedView(showExtendedView) {
        this.dispatch(fundExtendedView(showExtendedView));
    }

    renderCenterPanel(readMode, closed) {
        const {focus, arrRegion, rulDataTypes, calendarTypes, descItemTypes, packetTypes} = this.props;
        const showRegisterJp = arrRegion.showRegisterJp;
        const showDaosJp = arrRegion.showDaosJp;
        const activeFund = this.getActiveFund(this.props);

        if (arrRegion.extendedView) {   // extended view - jiné větší zobrazení stromu, renderuje se zde
            return (
                <FundTreeMain
                    focus={focus}
                    className="extended-tree"
                    fund={activeFund}
                    cutLongLabels={false}
                    versionId={activeFund.versionId}
                    {...activeFund.fundTree}
                    actionAddons={<Button onClick={() => {this.handleSetExtendedView(false)}} className='extended-view-toggle'><Icon glyph='fa-compress'/></Button>}
                />
            )
        } else if(activeFund.nodes.activeIndex === null){
            return (
                <div className='arr-output-detail-container'>
                   <div className="unselected-msg">
                        <div className="title">{i18n('arr.node.noSelection.title')}</div>
                        <div className="msg-text">{i18n('arr.node.noSelection.message')}</div>
                    </div>
                </div>
            );
        } else {    // standardní zobrazení pořádání - záložky node
            var packets = [];
            var fundId = activeFund.id;
            if (fundId && arrRegion.packets[fundId]) {
                packets = arrRegion.packets[fundId].items;
            }

            return (
                <NodeTabs
                    versionId={activeFund.versionId}
                    fund={activeFund}
                    closed={activeFund.closed}
                    nodes={activeFund.nodes.nodes}
                    activeIndex={activeFund.nodes.activeIndex}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                    descItemTypes={descItemTypes}
                    packetTypes={packetTypes}
                    packets={packets}
                    fundId={fundId}
                    showRegisterJp={showRegisterJp}
                    showDaosJp={showDaosJp}
                />
            )
        }
    }

    renderLeftPanel(readMode, closed) {
        const {focus, arrRegion} = this.props;
        const activeFund = this.getActiveFund(this.props);

        if (arrRegion.extendedView) {   // extended view - jiné větší zobrazení stromu, ale renderuje se v center panelu, tento bude prázdný
            return null;
        } else {    // standardní zobrazení pořádání, strom AS
            return (
                <FundTreeMain
                    className="fund-tree-container"
                    fund = {activeFund}
                    cutLongLabels={true}
                    versionId={activeFund.versionId}
                    {...activeFund.fundTree}
                    ref='tree'
                    focus={focus}
                    actionAddons={<Button onClick={() => {this.handleSetExtendedView(true)}} className='extended-view-toggle'><Icon glyph='fa-arrows-alt'/></Button>}
                />
            )
        }
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasRdPage(activeFund ? activeFund.id : null);
    }

    renderRightPanel() {
        return this.renderPanel();
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail, tab} = state
    return {
        splitter,
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        packetTypes: refTables.packetTypes,
        ruleSet: refTables.ruleSet,
        tab,
    }
}

export default connect(mapStateToProps)(ArrPage);
