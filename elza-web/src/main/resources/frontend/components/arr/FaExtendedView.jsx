/**
 * Komponenta rozšířeného zobrazení AP.
 */

require ('./FaExtendedView.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs, FaTreeMain, FaTreeMovementsLeft, FaTreeMovementsRight} from 'components';
import * as types from 'actions/constants/actionTypes';
import {Button} from 'react-bootstrap';
import {moveNodesUnder} from 'actions/arr/nodes'
import {indexById} from 'stores/app/utils.jsx'
import {getNodeParents} from './ArrUtils'

var FaExtendedView = class FaExtendedView extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleMoveUnder', 'getMoveInfo', 'handleMoveAfter',
            'handleMoveBefore', 'getDestNode');

        this.tabItems = [{id:0, title: 'Strom AP'}, {id: 1, title: 'Hromadné úpravy JP'}, {id: 2, title: 'Přesuny JP'}];
        this.state = { selectedTabItem: this.tabItems[2] }
    }

    componentDidMount() {
    }

    componentWillReceiveProps(nextProps) {
    }

    getDestNode() {
        const {fa} = this.props;
        return fa.faTreeMovementsRight.nodes[indexById(fa.faTreeMovementsRight.nodes, fa.faTreeMovementsRight.selectedId)];
    }

    getMoveInfo() {
        const {fa} = this.props;

        //  Zjištění seznamu označených NODE na přesun
        var nodes = Object.keys(fa.faTreeMovementsLeft.selectedIds).map(id => fa.faTreeMovementsLeft.nodes[indexById(fa.faTreeMovementsLeft.nodes, id)]);

        // Zjištění node kam přesouvat
        var node = this.getDestNode();

        return {nodes, node}
    }

    handleMoveUnder() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodesUnder(info.nodes, info.node));
    }

    handleMoveAfter() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodesAfter(info.nodes, info.node));
    }

    handleMoveBefore() {
        var info = this.getMoveInfo();
        this.dispatch(moveNodesBefore(info.nodes, info.node));
    }

    checkMoveUnder() {
        const {fa} = this.props;

        var destNode = this.getDestNode();
        var parents = getNodeParents(fa.faTreeMovementsRight.nodes, destNode.id);
        parents.push(destNode);

        // Test, zda se nepřesouvá sám pod sebe
        var ok = true;
        for (var a=0; a<parents.length; a++) {
            if (fa.faTreeMovementsLeft.selectedIds[parents[a].id]) {    // přesouvaný je v nadřazených v cíli, takto nelze přesouvat
                ok = false;
                break;
            }
        }
        return ok;
    }

    render() {
        const {fa} = this.props;

        var tabContent = [];
        var tabContentClassName;
        if (this.state.selectedTabItem.id == 0) {
            tabContent.push(
                <FaTreeMain
                    fa={fa}
                    versionId={fa.versionId}
                    {...fa.faTree}
                />
            )
        } else if (this.state.selectedTabItem.id == 2) {
            tabContentClassName = 'movements'
            tabContent.push(
                <div key={1} className='tree-left-container'>
                    <FaTreeMovementsLeft
                        fa={fa}
                        versionId={fa.versionId}
                        {...fa.faTreeMovementsLeft}
                    />
                </div>
            )

            var leftHasSelection = Object.keys(fa.faTreeMovementsLeft.selectedIds).length > 0;
            var rightHasSelection = fa.faTreeMovementsRight.selectedId != null;
            var active = leftHasSelection && rightHasSelection;
            var moveUnder = active && this.checkMoveUnder();
            tabContent.push(
                <div key={2} className='tree-actions-container'>
                    <Button onClick={this.handleMoveUnder} disabled={!moveUnder}>Přesunout do</Button>
                    <Button onClick={this.handleMoveBefore} disabled={!active}>Přesunout před</Button>
                    <Button onClick={this.handleMoveAfter} disabled={!active}>Přesunout za</Button>
                </div>
            )
            tabContent.push(
                <div key={3} className='tree-right-container'>
                    <FaTreeMovementsRight
                        fa={fa}
                        versionId={fa.versionId}
                        {...fa.faTreeMovementsRight}
                    />
                </div>
            )
        }

        return (
            <Tabs.Container className='fa-extended-view-container'>
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

FaExtendedView.propTypes = {
    fa: React.PropTypes.object,
}

module.exports = connect()(FaExtendedView);

