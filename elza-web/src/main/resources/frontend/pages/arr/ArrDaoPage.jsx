/**
 * Stránka archivních pomůcek.
 */

require('./ArrPage.less');
require('./ArrDaoPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {FundSettingsForm, Tabs, Icon, Search, Ribbon, i18n, FundTreeMovementsLeft, FundTreeDaos, ArrFundPanel, ArrDaos} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {getNodeParents, getNodeParent} from 'components/arr/ArrUtils.jsx'
import {moveNodesUnder, moveNodesBefore, moveNodesAfter} from 'actions/arr/nodes.jsx'

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
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
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
import {setVisiblePolicyRequest} from 'actions/arr/visiblePolicy.jsx'
import {routerNavigate} from 'actions/router.jsx'
import {fundTreeFetchIfNeeded} from 'actions/arr/fundTree.jsx'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {selectTab} from 'actions/global/tab.jsx'
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'

const keyModifier = Utils.getKeyModifier()
const keymap = ArrParentPage.mergeKeymap({});

const shortcutManager = new ShortcutsManager(keymap)

let _selectedTab = 0;

const ArrDaoPage = class ArrDaoPage extends ArrParentPage {
    constructor(props) {
        super(props, "fa-page");
    }

    componentDidMount() {
        super.componentDidMount();
    }

    componentWillReceiveProps(nextProps) {
        super.componentWillReceiveProps(nextProps);
    }

    getDestNode() {
        const fund = this.getActiveFund(this.props);
        return fund.fundTreeDaosRight.nodes[indexById(fund.fundTreeDaosRight.nodes, fund.fundTreeDaosRight.selectedId)];
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts ArrDaoPage", '[' + action + ']', this);
        super.handleShortcuts(action);
    }

    handleCreateUnder = () => {
        console.log("handleCreateUnder");
    };

    handlePinned = () => {
        console.log("handlePinned");
    };

    handleTabSelect = (item) => {
        _selectedTab = item.id;
        this.setState({});
    };

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon = (readMode, closed) => {
        const activeFund = this.getActiveFund(this.props);

        var altActions = [];

        var itemActions = [];

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="small">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr subMenu fundId={activeFund ? activeFund.id : null} altSection={altSection} itemSection={itemSection}/>
        )
    }

    hasPageShowRights = (userDetail, activeFund) => {
        return userDetail.hasArrPage(activeFund ? activeFund.id : null);
    };

    renderPackage = () => {
        return (
            <Search key={'dao-search'}
                    placeholder={i18n('search.input.search')}
                    filterText={null}
                    onChange={() => console.log("onChange")}
                    onSearch={() => console.log("onSearch")}
                    onClear={() => console.log("onClear")}
            />
        )
    };

    renderUnresolvePackage = () => {
        return (
            <Search key={'dao-search'}
                    placeholder={i18n('search.input.search')}
                    filterText={null}
                    onChange={console.log("onChange")}
                    onSearch={console.log("onSearch")}
                    onClear={console.log("onClear")}
            />
        )
    };

    renderLeftTree = () => {
        const fund = this.getActiveFund(this.props);

        return (
            <FundTreeMovementsLeft key={'tree-left'}
                                   fund={fund}
                                   versionId={fund.versionId}
                                   {...fund.fundTreeMovementsLeft}
            />
        )
    };

    handleRightNodeSelect = (node) => {
        this.setState({nodeRight: node});
    };

    renderCenterPanel = (readMode, closed) => {
        const {userDetail} = this.props;
        const fund = this.getActiveFund(this.props);

        let rightHasSelection = fund.fundTreeDaosRight.selectedId != null;
        let active = rightHasSelection && !readMode && !fund.closed;
        let node = this.getDestNode();
        let classRight = "tree-right-container";
        if (!rightHasSelection) {
            classRight += " daos-hide";
        }

        let tabs = [];
        tabs.push({
            id: 0,
            key: 0,
            title: 'Nepřiřazené entity',
            desc: 'zbývá 300'
        });

        tabs.push({
            id: 1,
            key: 1,
            title: 'Balíčky'
        });

        tabs.push({
            id: 2,
            key: 2,
            title: 'Archivní strom'
        });

        let content;
        switch (_selectedTab) {
            case 0:
                content = this.renderPackage();
                break;
            case 1:
                content = this.renderUnresolvePackage();
                break;
            case 2:
                content = this.renderLeftTree();
                break;
            default:
                break;
        }

        console.log(this.state);

        return (
            <div className="daos-content-container">
                <div key={1} className='tree-left-container'>

                    <Tabs.Container ref='tabs' className='daos-tabs-container'>
                        <Tabs.Tabs items={tabs} activeItem={{id: _selectedTab}} onSelect={this.handleTabSelect} />
                        <Tabs.Content>
                            {content}
                        </Tabs.Content>
                    </Tabs.Container>

                    <ArrDaos />
                </div>
                <div key={2} className='tree-actions-container'>
                    <Button onClick={this.handlePinned} disabled={!active}><Icon glyph="fa-thumb-tack"/><div>{i18n('arr.daos.pinned')}</div></Button>
                    <Button onClick={this.handleCreateUnder} disabled={!active}><Icon glyph="ez-move-under"/><div>{i18n('arr.daos.create.under')}</div></Button>
                </div>
                <div key={3} className={classRight}>

                    <FundTreeDaos
                        fund={fund}
                        versionId={fund.versionId}
                        area={types.FUND_TREE_AREA_DAOS_RIGHT}
                        {...fund.fundTreeDaosRight}
                    />

                    {rightHasSelection && <ArrDaos node={node} versionId={fund.versionId} />}
                </div>
            </div>
        )
    };
};

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

ArrDaoPage.propTypes = {
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
}

module.exports = connect(mapStateToProps)(ArrDaoPage);