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
import {Tabs, Icon, Ribbon, i18n} from 'components/index.jsx';
import {FundExtendedView, BulkActionsDialog, RibbonGroup,
    AbstractReactComponent, NodeTabs, FundTreeTabs, ListBox2, LazyListBox,
    VisiblePolicyForm, Loading, FundPackets} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {showRegisterJp} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {packetsFetchIfNeeded} from 'actions/arr/packets.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
import {Utils} from 'components/index.jsx';
import {isFundRootId} from 'components/arr/ArrUtils.jsx';
import {setFocus} from 'actions/global/focus.jsx'
import {descItemTypesFetchIfNeeded} from 'actions/refTables/descItemTypes.jsx'
import {fundNodesPolicyFetchIfNeeded} from 'actions/arr/fundNodesPolicy.jsx'
import {fundActionFormChange, fundActionFormShow} from 'actions/arr/fundAction.jsx'
import {fundSelectSubNode} from 'actions/arr/nodes.jsx'
import {createFundRoot} from 'components/arr/ArrUtils.jsx'
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy.jsx'
import {routerNavigate} from 'actions/router.jsx'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';

var _selectedTab = 0

var keyModifier = Utils.getKeyModifier()

var keymap = {
    Arr: {
        bulkActions: keyModifier + 'h',
        registerJp: keyModifier + 'j',
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
            'handleShortcuts', 'renderFundErrors', 'renderFundVisiblePolicies', 'handleSetVisiblePolicy',
            'renderPanel', 'renderDeveloperDescItems', 'handleShowHideSpecs', 'handleTabSelect', 'handleSelectErrorNode',
            'renderFundPackets', 'handleErrorPrevious', 'handleErrorNext', 'trySetFocus', 'handleOpenFundActionForm');

        this.state = {
            developerExpandedSpecsIds: {},
            fundNodesError: null,
        };
    }

    componentDidMount() {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        var fundId = this.getActiveFundId();
        if (fundId !== null) {
            this.dispatch(packetsFetchIfNeeded(fundId));
        }
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(descItemTypesFetchIfNeeded());
        this.dispatch(packetTypesFetchIfNeeded());
        this.dispatch(calendarTypesFetchIfNeeded());
        var fundId = this.getActiveFundId();
        if (fundId !== null) {
            this.dispatch(packetsFetchIfNeeded(fundId));
        }
        var activeFund = this.getActiveInfo(nextProps.arrRegion).activeFund;
        if (activeFund) {
            if (_selectedTab === 1) {
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
                        key: node.nodeKey,
                        version: node.subNodeForm.data.node.version
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
        
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'arr', 3)) {
                switch (_selectedTab) {
                    case 0:
                        this.refs.fundErrors && this.setState({}, () => {
                            ReactDOM.findDOMNode(this.refs.fundErrors).focus()
                        })
                        break
                    case 1:
                        this.refs.fundVisiblePolicies && this.setState({}, () => {
                            ReactDOM.findDOMNode(this.refs.fundVisiblePolicies).focus()
                        })
                        break
                    case 2:
                        this.refs.fundPackets && this.refs.fundPackets.getWrappedInstance().focus()
                        break
                    default:
                        ref = null
                        break
                }
                focusWasSet()
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

    handleOpenFundActionForm(versionId, subNode) {
        this.dispatch(fundActionFormChange(versionId, {nodes: [subNode]}));
        this.dispatch(fundActionFormShow(versionId));
        this.dispatch(routerNavigate('/arr/actions'));
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        const {arrRegion} = this.props;

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

        var show = this.props.arrRegion.showRegisterJp;

        altActions.push(
            <Button active={show} onClick={this.handleRegisterJp} key="toggle-record-jp">
                <Icon glyph="fa-th-list"/>
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.show-register-jp')}</span>
                </div>
            </Button>
        )

        var indexFund = arrRegion.activeIndex;
        if (indexFund !== null) {
            var activeFund = arrRegion.funds[indexFund];

            var nodeIndex = activeFund.nodes.activeIndex;
            if (nodeIndex !== null) {
                var activeNode = activeFund.nodes.nodes[nodeIndex];

                if (activeNode.selectedSubNodeId !== null) {
                    itemActions.push(
                        <Button key="next-error" onClick={this.handleErrorPrevious.bind(this, activeFund.versionId, activeNode.selectedSubNodeId)}><Icon glyph="fa-arrow-left"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.validation.error.previous')}</span></div>
                        </Button>,
                        <Button key="previous-error" onClick={this.handleErrorNext.bind(this, activeFund.versionId, activeNode.selectedSubNodeId)}><Icon glyph="fa-arrow-right"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.validation.error.next')}</span></div>
                        </Button>,
                        <Button key="prepareFundAction" onClick={this.handleOpenFundActionForm.bind(this, activeFund.versionId, activeInfo.activeSubNode)}><Icon glyph="fa-cog"/>
                            <div><span className="btnText">{i18n('ribbon.action.arr.fund.newFundAction')}</span></div>
                        </Button>
                    )
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
            <Ribbon arr fundId={activeInfo.activeFund ? activeInfo.activeFund.id : null} altSection={altSection} itemSection={itemSection}/>
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
        this.dispatch(setVisiblePolicyRequest(node.selectedSubNodeId, versionId, mapIds));
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
        const {developer, arrRegion, userDetail} = this.props;
        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        var node
        if (activeFund.nodes && activeFund.nodes.activeIndex !== null) {
            node = activeFund.nodes.nodes[activeFund.nodes.activeIndex]
        }

        // -----------
        // Záložky a obsah aktuálně vybrané založky
        var items = [];
        var tabContent
        var tabIndex = 0

        items.push({id: tabIndex, title: i18n('arr.panel.title.errors')});
        if (_selectedTab === tabIndex) tabContent = this.renderFundErrors(activeFund)
        tabIndex++;

        items.push({id: tabIndex, title: i18n('arr.panel.title.visiblePolicies')});
        if (_selectedTab === tabIndex) tabContent = this.renderFundVisiblePolicies(activeFund)
        tabIndex++;

        if (userDetail.hasOne(perms.FUND_ARR_ALL, {type: perms.FUND_ARR, fundId: activeFund.id})) {
            items.push({id: tabIndex, title: i18n('arr.panel.title.packets')});
            if (_selectedTab === tabIndex) tabContent = this.renderFundPackets(activeFund)
            tabIndex++;
        }

        // pouze v developer modu
        if (developer.enabled && node) {
            items.push({id: tabIndex, title: i18n('developer.title.descItems')});
            if (_selectedTab === tabIndex) tabContent = this.renderDeveloperDescItems(activeFund, node)
            tabIndex++;

            items.push({id: tabIndex, title: i18n('developer.title.scenarios')});
            if (_selectedTab === tabIndex) tabContent = this.renderDeveloperScenarios(activeFund, node)
            tabIndex++;
        }
        // -----------

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

    renderFundPackets() {
        const {arrRegion, packetTypes} = this.props;
        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

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

    render() {
        const {focus, splitter, arrRegion, userDetail, rulDataTypes, calendarTypes, descItemTypes, packetTypes} = this.props;

        var showRegisterJp = arrRegion.showRegisterJp;

        var funds = arrRegion.funds;
        var activeFund = arrRegion.activeIndex != null ? arrRegion.funds[arrRegion.activeIndex] : null;

        if (userDetail.hasArrPage(activeFund ? activeFund.id : null)) { // má právo na tuto stránku
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

            var centerPanel;
            if (activeFund) {
                if (arrRegion.extendedView) {   // rozšířené zobrazení stromu AS
                    centerPanel = (
                        <FundExtendedView
                            fund={activeFund}
                            versionId={activeFund.versionId}
                            descItemTypes={descItemTypes}
                            packetTypes={packetTypes}
                            calendarTypes={calendarTypes}
                            rulDataTypes={rulDataTypes}
                        />
                    )
                } else if (activeFund.nodes) {
                    var packets = [];
                    var fundId = this.getActiveFundId();
                    if (fundId && arrRegion.packets[fundId]) {
                        packets = arrRegion.packets[fundId].items;
                    }

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
        } else {
            centerPanel = <div>{i18n('global.insufficient.right')}</div>
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
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail} = state
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
    userDetail: React.PropTypes.object.isRequired,
}

ArrPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(ArrPage);
