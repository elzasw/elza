/**
 * Komponenta rozšířeného zobrazení AS.
 */

require ('./FundExtendedView.less');

import React from 'react';
import {connect} from 'react-redux'
import {FundDataGrid, DataGrid, DataGridPagination, AbstractReactComponent, i18n, Tabs, Icon, FundTreeMain, FundTreeMovementsLeft, FundTreeMovementsRight} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {Button} from 'react-bootstrap';
import {moveNodesUnder, moveNodesBefore, moveNodesAfter} from 'actions/arr/nodes'
import {fundExtendedView} from 'actions/arr/fund'
import {indexById} from 'stores/app/utils.jsx'
import {getNodeParents, getNodeParent} from './ArrUtils'

var FundExtendedView = class FundExtendedView extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleMoveUnder', 'getMoveInfo', 'handleMoveAfter',
            'handleMoveBefore', 'getDestNode', 'handleToggleExtendedView');

        this.tabItems = [{id:0, title: 'Strom AS'}, {id: 1, title: 'Hromadné úpravy JP'}, {id: 2, title: 'Přesuny JP'}];
        this.state = { selectedTabItem: this.tabItems[1] }
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    getDestNode() {
        const {fund} = this.props;
        return fund.fundTreeMovementsRight.nodes[indexById(fund.fundTreeMovementsRight.nodes, fund.fundTreeMovementsRight.selectedId)];
    }

    getMoveInfo() {
        const {versionId, fund} = this.props;

        //  Zjištění seznamu označených NODE na přesun
        var nodes = Object.keys(fund.fundTreeMovementsLeft.selectedIds).map(id => fund.fundTreeMovementsLeft.nodes[indexById(fund.fundTreeMovementsLeft.nodes, id)]);
        var nodesParent = getNodeParent(fund.fundTreeMovementsLeft.nodes, nodes[0].id);

        // Zjištění node kam přesouvat
        var dest = this.getDestNode();
        var destParent = getNodeParent(fund.fundTreeMovementsRight.nodes, dest.id);

        return {versionId, nodes, nodesParent, dest, destParent}
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
        const {fund} = this.props;

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
        const {fund} = this.props;

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

    handleToggleExtendedView() {
        this.dispatch(fundExtendedView(false));
    }

    render() {
        const {fund, descItemTypes, rulDataTypes} = this.props;

        var tabContent = [];
        var tabContentClassName;
        if (this.state.selectedTabItem.id == 0) {
            tabContent.push(
                <FundTreeMain
                    fund={fund}
                    cutLongLabels={false}
                    versionId={fund.versionId}
                    {...fund.fundTree}
                />
            )
        } else if (this.state.selectedTabItem.id == 1) {
            tabContentClassName = 'movements'
            tabContent.push(
                <FundDataGrid
                    versionId={fund.versionId}
                    fundDataGrid={fund.fundDataGrid}
                    descItemTypes={descItemTypes}
                    rulDataTypes={rulDataTypes}
                />
            )
        } else if (this.state.selectedTabItem.id == 2) {
            tabContentClassName = 'movements'
            tabContent.push(
                <div key={1} className='tree-left-container'>
                    <FundTreeMovementsLeft
                        fund={fund}
                        versionId={fund.versionId}
                        {...fund.fundTreeMovementsLeft}
                    />
                </div>
            )

            var leftHasSelection = Object.keys(fund.fundTreeMovementsLeft.selectedIds).length > 0;
            var rightHasSelection = fund.fundTreeMovementsRight.selectedId != null;
            var active = leftHasSelection && rightHasSelection;
            var moveUnder = active && this.checkMoveUnder();
            var moveBeforeAfter = active && this.checkMoveBeforeAfter();
            tabContent.push(
                <div key={2} className='tree-actions-container'>
                    <Button onClick={this.handleMoveUnder} disabled={!moveUnder}>Přesunout do<Icon glyph="fa-arrow-circle-o-right"/></Button>
                    <Button onClick={this.handleMoveBefore} disabled={!moveBeforeAfter}>Přesunout před<Icon glyph="fa-arrow-circle-o-right"/></Button>
                    <Button onClick={this.handleMoveAfter} disabled={!moveBeforeAfter}>Přesunout za<Icon glyph="fa-arrow-circle-o-right"/></Button>
                </div>
            )
            tabContent.push(
                <div key={3} className='tree-right-container'>
                    <FundTreeMovementsRight
                        fund={fund}
                        versionId={fund.versionId}
                        {...fund.fundTreeMovementsRight}
                    />
                </div>
            )
        }

        return (
            <Tabs.Container className='fa-extended-view-container'>
                <Button onClick={this.handleToggleExtendedView} className='extended-view-toggle'><Icon glyph='fa-compress'/></Button>
                <Tabs.Tabs items={this.tabItems} activeItem={this.state.selectedTabItem}
                    onSelect={item=>this.setState({selectedTabItem: item})}
                />
                <Tabs.Content className={tabContentClassName}>
                    {tabContent}
                </Tabs.Content>
            </Tabs.Container>
        );
    }
}

FundExtendedView.propTypes = {
    fund: React.PropTypes.object.isRequired,
    versionId: React.PropTypes.number.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
}

module.exports = connect()(FundExtendedView);

