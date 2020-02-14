import './ArrPage.scss';
import './ArrMovementsPage.scss';

/**
 * Stránka archivních pomůcek.
 */

import PropTypes from 'prop-types';

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';

import * as types from 'actions/constants/ActionTypes.js';
import {getNodeParents, getNodeParent} from 'components/arr/ArrUtils.jsx'
import {moveNodesUnder, moveNodesBefore, moveNodesAfter, moveNodes} from 'actions/arr/nodes.jsx'

import ArrParentPage from "./ArrParentPage.jsx";

import {
    RibbonGroup,
    AbstractReactComponent,
    ListBox2,
    LazyListBox,
    Loading,
    Tabs,
    Icon,
    i18n,
    Utils
} from 'components/shared';
import {
    Ribbon,
    FundSettingsForm,
    NodeTabs,
    FundPackets,
    FundFiles,
    FundTreeMain,
    FundTreeMovementsLeft,
    FundTreeMovementsRight,
    ArrFundPanel
} from 'components/index.jsx';
import {ButtonGroup, Button, DropdownButton, MenuItem, Collapse} from 'react-bootstrap';
import PageLayout from "../shared/layout/PageLayout";
import {WebApi} from 'actions/index.jsx';
import {modalDialogShow} from 'actions/global/modalDialog.jsx'
import {showRegisterJp, fundsFetchIfNeeded} from 'actions/arr/fund.jsx'
import {versionValidate, versionValidationErrorNext, versionValidationErrorPrevious} from 'actions/arr/versionValidation.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'
import {developerNodeScenariosRequest} from 'actions/global/developer.jsx'
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
import {Shortcuts} from 'react-shortcuts';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';
import {userDetailsSaveSettings} from 'actions/user/userDetail.jsx'

const ArrMovementsPage = class ArrMovementsPage extends ArrParentPage {
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

    handleShortcuts(action,e) {
        console.log("#handleShortcuts ArrMovementsPage", '[' + action + ']', this);
        super.handleShortcuts(action,e);
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
        this.dispatch(moveNodes("UNDER",info.versionId,info.nodes,info.nodesParent,info.dest,info.destParent));
    }

    handleMoveAfter() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodes("AFTER",info.versionId,info.nodes,info.nodesParent,info.dest,info.destParent));
    }

    handleMoveBefore() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodes("BEFORE",info.versionId,info.nodes,info.nodesParent,info.dest,info.destParent));
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
    buildRibbon(readMode, closed) {
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

    renderCenterPanel(readMode, closed) {
        const {userDetail} = this.props;
        const fund = this.getActiveFund(this.props);
        var leftHasSelection = Object.keys(fund.fundTreeMovementsLeft.selectedIds).length > 0;
        var rightHasSelection = fund.fundTreeMovementsRight.selectedId != null;
        var active = leftHasSelection && rightHasSelection && !readMode && !fund.closed && !fund.moving;
        var moveUnder = active && this.checkMoveUnder();
        var moveBeforeAfter = active && this.checkMoveBeforeAfter();

        return (
            <div className="movements-content-container">
                <div className={fund.moving ? "moving-overlay visible" : "moving-overlay"}><Icon glyph="fa-cog fa-spin"/><div>Probíhá přesun</div></div>
                <div key={1} className='tree-left-container'>

                    <FundTreeMovementsLeft
                        fund={fund}
                        versionId={fund.versionId}
                        {...fund.fundTreeMovementsLeft}
                    />
                </div>
                <div key={2} className='tree-actions-container'>

                    <Button onClick={this.handleMoveBefore} disabled={!moveBeforeAfter}><Icon glyph="ez-move-before2"/><div>{i18n('arr.movements.move.before')}</div></Button>
                    <Button onClick={this.handleMoveUnder} disabled={!moveUnder}><Icon glyph="ez-move-under"/><div>{i18n('arr.movements.move.under')}</div></Button>
                    <Button onClick={this.handleMoveAfter} disabled={!moveBeforeAfter}><Icon glyph="ez-move-after2"/><div>{i18n('arr.movements.move.after')}</div></Button>

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
        ruleSet: refTables.ruleSet,
        tab,
    }
}

ArrMovementsPage.propTypes = {
    splitter: PropTypes.object.isRequired,
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    calendarTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
}

export default connect(mapStateToProps)(ArrMovementsPage);
