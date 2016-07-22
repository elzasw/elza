/**
 * Stránka archivních pomůcek.
 */

require('./ArrPage.less');
require('./ArrMovementsPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {FundSettingsForm, Tabs, Icon, Ribbon, i18n, FundTreeMovementsLeft, FundTreeMovementsRight, ArrFundPanel} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {getNodeParents, getNodeParent} from 'components/arr/ArrUtils.jsx'
import {moveNodesUnder, moveNodesBefore, moveNodesAfter} from 'actions/arr/nodes.jsx'

var ArrParentPage = require("./ArrParentPage.jsx");

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

var ArrMovementsPage = class ArrMovementsPage extends ArrParentPage {
    constructor(props) {
        super(props, "fa-page");

        this.bindMethods(
            'handleMoveUnder',
            'getMoveInfo',
            'handleMoveAfter',
            'handleMoveBefore',
            'getDestNode');
    }

    componentDidMount() {
        super.componentDidMount();
    }

    componentWillReceiveProps(nextProps) {
        super.componentWillReceiveProps(nextProps);
    }

    getDestNode() {
        const fund = this.getActiveFund(this.props);
        return fund.fundTreeMovementsRight.nodes[indexById(fund.fundTreeMovementsRight.nodes, fund.fundTreeMovementsRight.selectedId)];
    }

    getMoveInfo() {
        const fund = this.getActiveFund(this.props);

        //  Zjištění seznamu označených NODE na přesun
        var nodes = Object.keys(fund.fundTreeMovementsLeft.selectedIds).map(id => fund.fundTreeMovementsLeft.nodes[indexById(fund.fundTreeMovementsLeft.nodes, id)]);
        var nodesParent = getNodeParent(fund.fundTreeMovementsLeft.nodes, nodes[0].id);

        // Zjištění node kam přesouvat
        var dest = this.getDestNode();
        var destParent = getNodeParent(fund.fundTreeMovementsRight.nodes, dest.id);

        return {versionId: fund.versionId, nodes, nodesParent, dest, destParent}
    }

    handleMoveUnder() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodesUnder(info.versionId, info.nodes, info.nodesParent, info.dest, info.destParent));
    }

    handleMoveAfter() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodesAfter(info.versionId, info.nodes, info.nodesParent, info.dest, info.destParent));
    }

    handleMoveBefore() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodesBefore(info.versionId, info.nodes, info.nodesParent, info.dest, info.destParent));
    }

    checkMoveUnder() {
        const fund = this.getActiveFund(this.props);

        var destNode = this.getDestNode();
        if (!destNode) return false;

        var parents = getNodeParents(fund.fundTreeMovementsRight.nodes, destNode.id);
        parents.push(destNode);

        // Test, zda se nepřesouvá sám pod sebe
        var ok = true;
        for (var a=0; a<parents.length; a++) {
            if (fund.fundTreeMovementsLeft.selectedIds[parents[a].id]) {    // přesouvaný je v nadřazených v cíli, takto nelze přesouvat
                ok = false;
                break;
            }
        }
        return ok;
    }

    checkMoveBeforeAfter() {
        const fund = this.getActiveFund(this.props);

        var destNode = this.getDestNode();
        if (!destNode) return false;

        if (destNode.depth == 1) {  // u kořene nelze tuto akci volat
            return false;
        }

        var parents = getNodeParents(fund.fundTreeMovementsRight.nodes, destNode.id);
        parents.push(destNode);

        // Test, zda se nepřesouvá sám pod sebe
        var ok = true;
        for (var a=0; a<parents.length; a++) {
            if (fund.fundTreeMovementsLeft.selectedIds[parents[a].id]) {    // přesouvaný je v nadřazených v cíli, takto nelze přesouvat
                ok = false;
                break;
            }
        }
        return ok;
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
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

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasArrPage(activeFund ? activeFund.id : null);
    }

    renderCenterPanel() {
        const {userDetail} = this.props;
        const fund = this.getActiveFund(this.props);

        var settings = getOneSettings(userDetail.settings, 'FUND_READ_MODE', 'FUND', fund.id);
        var settingsValues = settings.value != 'false';
        const readMode = settingsValues;

        var leftHasSelection = Object.keys(fund.fundTreeMovementsLeft.selectedIds).length > 0;
        var rightHasSelection = fund.fundTreeMovementsRight.selectedId != null;
        var active = leftHasSelection && rightHasSelection && !readMode && !fund.closed;
        var moveUnder = active && this.checkMoveUnder();
        var moveBeforeAfter = active && this.checkMoveBeforeAfter();

        return (
            <div className="movements-content-container">
                <div key={1} className='tree-left-container'>
                    <FundTreeMovementsLeft
                        fund={fund}
                        versionId={fund.versionId}
                        {...fund.fundTreeMovementsLeft}
                    />
                </div>
                <div key={2} className='tree-actions-container'>
                    <Button onClick={this.handleMoveUnder} disabled={!moveUnder}>Přesunout do<Icon glyph="fa-arrow-circle-o-right"/></Button>
                    <Button onClick={this.handleMoveBefore} disabled={!moveBeforeAfter}>Přesunout před<Icon glyph="fa-arrow-circle-o-right"/></Button>
                    <Button onClick={this.handleMoveAfter} disabled={!moveBeforeAfter}>Přesunout za<Icon glyph="fa-arrow-circle-o-right"/></Button>
                </div>
                <div key={3} className='tree-right-container'>
                    <FundTreeMovementsRight
                        fund={fund}
                        versionId={fund.versionId}
                        {...fund.fundTreeMovementsRight}
                    />
                </div>
            </div>
        )
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

ArrMovementsPage.propTypes = {
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

module.exports = connect(mapStateToProps)(ArrMovementsPage);
