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
import {Tabs, Icon, Ribbon, i18n} from 'components';
import {FundExtendedView, FundForm, BulkActionsDialog, VersionValidationDialog, RibbonMenu, RibbonGroup, RibbonSplit,
    ToggleContent, AbstractReactComponent, ModalDialog, NodeTabs, FundTreeTabs, ListBox, LazyListBox,
    VisiblePolicyForm, Loading} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {showRegisterJp} from 'actions/arr/fund'
import {scopesDirty} from 'actions/refTables/scopesData'
import {versionValidate} from 'actions/arr/versionValidation'
import {packetsFetchIfNeeded} from 'actions/arr/packets'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes'
import {developerNodeScenariosRequest} from 'actions/global/developer'
import {Utils} from 'components'
import {barrier} from 'components/Utils';
import {isFundRootId} from 'components/arr/ArrUtils';
import {setFocus} from 'actions/global/focus'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes'
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy'
import {propsEquals} from 'components/Utils'
import {fundSelectSubNode} from 'actions/arr/nodes'
import {createFundRoot} from 'components/arr/ArrUtils.jsx'
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');

var _selectedTab = 0

var keyModifier = Utils.getKeyModifier()

var keymap = {
    Arr: {
        bulkActions: keyModifier + 'h',
        registerJp: keyModifier + 'j',
        area0: keyModifier + '0',
        area1: keyModifier + '1',
        area2: keyModifier + '2',
        area3: keyModifier + '3',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

var ArrPage = class ArrPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getActiveInfo', 'buildRibbon', 'handleRegisterJp',
            'getActiveFundId', 'handleBulkActionsDialog', 'handleSelectVisiblePoliciesNode', 'handleShowVisiblePolicies',
            'handleValidationDialog', 'handleShortcuts', 'renderFundErrors', 'renderFundVisiblePolicies', 'handleSetVisiblePolicy',
            'renderPanel', 'renderDeveloperDescItems', 'handleShowHideSpecs', 'handleTabSelect', 'handleSelectErrorNode');

        this.state = {developerExpandedSpecsIds: {}};
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        var fundId = this.getActiveFundId();
        if (fundId !== null) {
            this.dispatch(packetsFetchIfNeeded(fundId));
        }
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        var fundId = this.getActiveFundId();
        if (fundId !== null) {
            this.dispatch(packetsFetchIfNeeded(fundId));
        }
        var activeFund = this.getActiveInfo(nextProps.arrRegion).activeFund;
        if (activeFund) {
            /*
            var validation = activeFund.versionValidation;
            this.requestValidationData(validation.isDirty, validation.isFetching, activeFund.versionId);
             */

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
                        key: node.nodeKey,
                        version: node.subNodeForm.data.node.version
                    }, activeFund.versionId));
                }
            }
        }
    }

    requestValidationData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, false))
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'bulkActions':
                this.handleBulkActionsDialog()
                break
            case 'registerJp':
                this.handleRegisterJp()
                break
            case 'area0':
                this.dispatch(setFocus('arr', 0))
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
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    getActiveFundId() {
        var arrRegion = this.props.arrRegion;
        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
        if (activeFund) {
            return activeFund.id;
        } else {
            return null;
        }
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

    handleValidationDialog() {
        this.dispatch(modalDialogShow(this, i18n('arr.fund.title.versionValidation'), <VersionValidationDialog />));
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        var activeInfo = this.getActiveInfo();

        var altActions = [];

        var itemActions = [];

        if (activeInfo.activeFund && !activeInfo.activeFund.closed) {
            itemActions.push(
                <Button key="bulk-actions" onClick={this.handleBulkActionsDialog}><Icon glyph="fa-cogs"/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fund.bulkActions')}</span></div>
                </Button>,
                <Button key="fas" onClick={()=>{}}><Icon glyph="fa-cogs"/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fund.fas')}</span></div>
                </Button>,
                <Button key="validation" onClick={this.handleValidationDialog}>
                    <Icon className={activeInfo.activeFund.versionValidation.isFetching ? "fa-spin" : ""} glyph={
                    activeInfo.activeFund.versionValidation.isFetching ? "fa-refresh" : (
                        activeInfo.activeFund.versionValidation.count > 0 ? "fa-exclamation-triangle" : "fa-check"
                    )
                }/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fund.validation')}</span></div>
                </Button>
            )
        }

        var show = this.props.arrRegion.showRegisterJp;

        itemActions.push(
            <Button active={show} onClick={this.handleRegisterJp} key="toggle-record-jp">
                <Icon glyph="fa-th-list"/>
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.show-register-jp')}</span>
                </div>
            </Button>
        )

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="large">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="large">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr altSection={altSection} itemSection={itemSection}/>
        )
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
        // TODO: aktivni node

        return (
            <div className="errors-listbox-container">
                <LazyListBox
                    getItems={(fromIndex, toIndex) => {
                                return WebApi.getValidationItems(activeFund.versionId, fromIndex, toIndex)
                            }}
                    renderItemContent={(item) => item !== null ? <div>{item.name}</div> : '...'}
                    selectedItem={activeNode ? activeNode.selectedSubNodeId : null}
                    itemHeight={32} // nutne dat stejne cislo i do css jako .pokusny-listbox-container .listbox-item { height: 24px; }
                    /*onFocus={item=>{console.log("FOCUS", item)}}*/
                    onSelect={this.handleSelectErrorNode.bind(this, activeFund)}
                    /*onDoubleClick={item=>{console.log("DOUBLECLICK", item)}}*/
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

    handleSelectVisiblePoliciesNode(available, activeFund, index) {
        var node = available[index];
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
        this.dispatch(setVisiblePolicyRequest(node.selectedSubNodeId, versionId, mapIds));
    }

    renderFundVisiblePolicies(activeFund) {

        var nodesPolicy = activeFund.fundNodesPolicy;

        if (!nodesPolicy.fetched) {
            return <Loading />
        }
        var activeIndex = -1;
        var activeNode = null;
        if (activeFund.nodes.activeIndex != null) {
            activeNode = activeFund.nodes.nodes[activeFund.nodes.activeIndex];
        }

        if (activeNode != null) {
            nodesPolicy.items.forEach((item, index)=>{
                if (activeNode.selectedSubNodeId == item.id) {
                    activeIndex = index;
                }
            });

        }

        if (nodesPolicy.items.length == 0) {
            return <div>no items</div>
        }

        return (
            <div className="visiblePolicies-container">
                <ListBox className="visiblePolicies-listbox"
                    items={nodesPolicy.items}
                         activeIndex={activeIndex}
                    renderItemContent={(node, isActive) => <div key={node.id} className="visiblePolicies-item">{node.name}</div>}
                    onChangeSelection={this.handleSelectVisiblePoliciesNode.bind(this, nodesPolicy.items, activeFund)}
                    onDoubleClick={this.handleShowVisiblePolicies.bind(this, activeFund)}
                />
            </div>
        )
    }

    renderDeveloperDescItems(activeFund, node) {
        var rows = []
        if (node.subNodeForm.fetched) {
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
        const {arrRegion} = this.props;
        _selectedTab = item.id;

        if (_selectedTab === 1) {
            var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
            this.dispatch(fundNodesPolicyFetchIfNeeded(activeFund.versionId));
        }

        this.setState({});
    }

    renderPanel() {
        const {developer, arrRegion} = this.props;
        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        var node
        if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex]
        }

        var items = [];
        items.push({id: 0, title: i18n('arr.panel.title.errors')});
        items.push({id: 1, title: i18n('arr.panel.title.visiblePolicies')});

        // pouze v developer modu
        if (developer.enabled && node) {
            items.push({id: 2, title: i18n('developer.title.descItems')});
            items.push({id: 3, title: i18n('developer.title.scenarios')});
        }

        return (
            <Tabs.Container>

                <Tabs.Tabs items={items}
                           activeItem={{id: _selectedTab}}
                           onSelect={this.handleTabSelect}
                />
                <Tabs.Content>
                    {_selectedTab === 0 && this.renderFundErrors(activeFund)}
                    {_selectedTab === 1 && this.renderFundVisiblePolicies(activeFund)}
                    {developer.enabled && node && _selectedTab === 2 && this.renderDeveloperDescItems(activeFund, node)}
                    {developer.enabled && node && _selectedTab === 3 && this.renderDeveloperScenarios(activeFund, node)}
                </Tabs.Content>
            </Tabs.Container>
        )
    }

    render() {
        const {focus, splitter, arrRegion, rulDataTypes, calendarTypes, descItemTypes, packetTypes} = this.props;

        var showRegisterJp = arrRegion.showRegisterJp;

        var funds = arrRegion.funds;
        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;
        var leftPanel;
        if (!arrRegion.extendedView && activeFund) {
            leftPanel = (
                <FundTreeTabs
                    funds={funds}
                    activeFund={activeFund}
                    focus={focus}
                />
            )


        }

        var packets = [];
        var fundId = this.getActiveFundId();
        if (fundId && arrRegion.packets[fundId]) {
            packets = arrRegion.packets[fundId].items;
        }

        var centerPanel;
        if (activeFund) {
            if (arrRegion.extendedView) {   // rozšířené zobrazení stromu AS
                centerPanel = (
                    <FundExtendedView
                        fund={activeFund}
                        versionId={activeFund.versionId}
                        descItemTypes={descItemTypes}
                        packetTypes={packetTypes}
                        rulDataTypes={rulDataTypes}
                    />
                )
            } else if (activeFund.nodes) {
                centerPanel = (
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
                    />
                )
            }
        } else {
            centerPanel = (
                <div className="fund-noselect">{i18n('arr.fund.noselect')}</div>
            )
        }

        var rightPanel;
        if (activeFund) {
            rightPanel = this.renderPanel()
        }

        return (
            <Shortcuts name='Arr' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    className='fa-page'
                    ribbon={this.buildRibbon()}
                    leftPanel={leftPanel}
                    centerPanel={centerPanel}
                    rightPanel={rightPanel}
                />
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, form, focus, developer} = state
    return {
        splitter,
        arrRegion,
        focus,
        developer,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        packetTypes: refTables.packetTypes,
    }
}

ArrPage.propTypes = {
    splitter: React.PropTypes.object.isRequired,
    arrRegion: React.PropTypes.object.isRequired,
    developer: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
}

ArrPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(ArrPage);
