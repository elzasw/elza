import './ArrPage.scss';
import './ArrMovementsPage.scss';
import PropTypes from 'prop-types';

import React from 'react';
import {indexById} from '../../stores/app/utils';
import {connect} from 'react-redux';
import {getNodeParent, getNodeParents} from '../../components/arr/ArrUtils';
import {moveNodes} from '../../actions/arr/nodes';

import ArrParentPage from './ArrParentPage';

import {i18n, Icon, RibbonGroup} from '../../components/shared';
import {FundTreeMovementsLeft, FundTreeMovementsRight, Ribbon} from '../../components/index';
import {Button} from '../../components/ui';

/**
 * Stránka archivních pomůcek.
 */

const ArrMovementsPage = class ArrMovementsPage extends ArrParentPage {
    constructor(props) {
        super(props, 'fa-page');

        this.bindMethods('handleMoveUnder', 'getMoveInfo', 'handleMoveAfter', 'handleMoveBefore', 'getDestNode');
    }

    componentDidMount() {
        super.componentDidMount();
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        super.UNSAFE_componentWillReceiveProps(nextProps);
    }

    getDestNode() {
        const fund = this.getActiveFund(this.props);
        return fund.fundTreeMovementsRight.nodes[
            indexById(fund.fundTreeMovementsRight.nodes, fund.fundTreeMovementsRight.selectedId)
        ];
    }

    handleShortcuts(action, e) {
        console.log('#handleShortcuts ArrMovementsPage', '[' + action + ']', this);
        super.handleShortcuts(action, e);
    }

    getMoveInfo() {
        const fund = this.getActiveFund(this.props);

        //  Zjištění seznamu označených NODE na přesun
        const nodes = Object.keys(fund.fundTreeMovementsLeft.selectedIds).map(
            id => {
                const index = indexById(fund.fundTreeMovementsLeft.nodes, parseInt(id));
                return fund.fundTreeMovementsLeft.nodes[index];
            }
        );

        // Serazeni nodu jak byly v puvodnim seznamu
        nodes.sort((nodeA, nodeB)=>{
            const indexA = indexById(fund.fundTreeMovementsLeft.nodes, nodeA.id);
            const indexB = indexById(fund.fundTreeMovementsLeft.nodes, nodeB.id);
            return indexA - indexB;
        })

        const nodesParent = getNodeParent(fund.fundTreeMovementsLeft.nodes, nodes[0].id);

        // Zjištění node kam přesouvat
        const dest = this.getDestNode();
        const destParent = getNodeParent(fund.fundTreeMovementsRight.nodes, dest.id);

        return {versionId: fund.versionId, nodes, nodesParent, dest, destParent};
    }

    handleMoveUnder() {
        const info = this.getMoveInfo();
        this.props.dispatch(
            moveNodes('UNDER', info.versionId, info.nodes, info.nodesParent, info.dest, info.destParent),
        );
    }

    handleMoveAfter() {
        const info = this.getMoveInfo();
        this.props.dispatch(
            moveNodes('AFTER', info.versionId, info.nodes, info.nodesParent, info.dest, info.destParent),
        );
    }

    handleMoveBefore() {
        const info = this.getMoveInfo();
        this.props.dispatch(
            moveNodes('BEFORE', info.versionId, info.nodes, info.nodesParent, info.dest, info.destParent),
        );
    }

    checkMoveUnder() {
        const fund = this.getActiveFund(this.props);

        const destNode = this.getDestNode();
        if (!destNode) return false;

        const parents = getNodeParents(fund.fundTreeMovementsRight.nodes, destNode.id);
        parents.push(destNode);

        // Test, zda se nepřesouvá sám pod sebe
        let ok = true;
        for (let a = 0; a < parents.length; a++) {
            if (fund.fundTreeMovementsLeft.selectedIds[parents[a].id]) {
                // přesouvaný je v nadřazených v cíli, takto nelze přesouvat
                ok = false;
                break;
            }
        }
        return ok;
    }

    checkMoveBeforeAfter() {
        const fund = this.getActiveFund(this.props);

        const destNode = this.getDestNode();
        if (!destNode) return false;

        if (destNode.depth == 1) {
            // u kořene nelze tuto akci volat
            return false;
        }

        const parents = getNodeParents(fund.fundTreeMovementsRight.nodes, destNode.id);
        parents.push(destNode);

        // Test, zda se nepřesouvá sám pod sebe
        let ok = true;
        for (let a = 0; a < parents.length; a++) {
            if (fund.fundTreeMovementsLeft.selectedIds[parents[a].id]) {
                // přesouvaný je v nadřazených v cíli, takto nelze přesouvat
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

        const altActions = [];

        const itemActions = [];

        let altSection;
        if (altActions.length > 0) {
            altSection = (
                <RibbonGroup key="alt" className="small">
                    {altActions}
                </RibbonGroup>
            );
        }

        let itemSection;
        if (itemActions.length > 0) {
            itemSection = (
                <RibbonGroup key="item" className="small">
                    {itemActions}
                </RibbonGroup>
            );
        }

        return (
            <Ribbon
                arr
                subMenu
                fundId={activeFund ? activeFund.id : null}
                altSection={altSection}
                itemSection={itemSection}
            />
        );
    }

    hasPageShowRights(userDetail, activeFund) {
        return userDetail.hasArrPage(activeFund ? activeFund.id : null);
    }

    renderCenterPanel(readMode, closed) {
        const {userDetail} = this.props;
        const fund = this.getActiveFund(this.props);
        const leftHasSelection = Object.keys(fund.fundTreeMovementsLeft.selectedIds).length > 0;
        const rightHasSelection = fund.fundTreeMovementsRight.selectedId != null;
        const active = leftHasSelection && rightHasSelection && !readMode && !fund.closed && !fund.moving;
        const moveUnder = active && this.checkMoveUnder();
        const moveBeforeAfter = active && this.checkMoveBeforeAfter();

        return (
            <div className="movements-content-container">
                <div className={fund.moving ? 'moving-overlay visible' : 'moving-overlay'}>
                    <Icon glyph="fa-cog fa-spin" />
                    <div>Probíhá přesun</div>
                </div>
                <div key={1} className="tree-left-container">
                    <FundTreeMovementsLeft fund={fund} versionId={fund.versionId} {...fund.fundTreeMovementsLeft} />
                </div>
                <div key={2} className="tree-actions-container">
                    <Button onClick={this.handleMoveBefore} disabled={!moveBeforeAfter}>
                        <Icon glyph="ez-move-before2" />
                        <div>{i18n('arr.movements.move.before')}</div>
                    </Button>
                    <Button onClick={this.handleMoveUnder} disabled={!moveUnder}>
                        <Icon glyph="ez-move-under" />
                        <div>{i18n('arr.movements.move.under')}</div>
                    </Button>
                    <Button onClick={this.handleMoveAfter} disabled={!moveBeforeAfter}>
                        <Icon glyph="ez-move-after2" />
                        <div>{i18n('arr.movements.move.after')}</div>
                    </Button>
                </div>
                <div key={3} className="tree-right-container">
                    <FundTreeMovementsRight fund={fund} versionId={fund.versionId} {...fund.fundTreeMovementsRight} />
                </div>
            </div>
        );
    }
};

function mapStateToProps(state) {
    const {splitter, arrRegion, refTables, form, focus, developer, userDetail, tab} = state;
    return {
        splitter,
        arrRegion,
        focus,
        developer,
        userDetail,
        rulDataTypes: refTables.rulDataTypes,
        descItemTypes: refTables.descItemTypes,
        ruleSet: refTables.ruleSet,
        tab,
    };
}

ArrMovementsPage.propTypes = {
    splitter: PropTypes.object.isRequired,
    arrRegion: PropTypes.object.isRequired,
    developer: PropTypes.object.isRequired,
    rulDataTypes: PropTypes.object.isRequired,
    descItemTypes: PropTypes.object.isRequired,
    focus: PropTypes.object.isRequired,
    userDetail: PropTypes.object.isRequired,
    ruleSet: PropTypes.object.isRequired,
};

export default connect(mapStateToProps)(ArrMovementsPage);
