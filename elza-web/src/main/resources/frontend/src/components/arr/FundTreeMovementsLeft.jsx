/**
 * Strom AS.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs} from 'components/shared';
import FundTreeLazy from './FundTreeLazy'
import * as types from 'actions/constants/ActionTypes.js';
import {MenuItem} from 'react-bootstrap';
import {selectFundTab, closeFundTab} from 'actions/arr/fund.jsx'
import {fundTreeFulltextChange, fundTreeFulltextSearch, fundTreeFulltextNextItem, fundTreeFulltextPrevItem, fundTreeSelectNode, fundTreeCollapse, fundTreeFocusNode, fundTreeFetchIfNeeded, fundTreeNodeExpand, fundTreeNodeCollapse} from 'actions/arr/fundTree.jsx'
import {createFundRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu.jsx'

class FundTreeMovementsLeft extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleNodeClick', 'handleContextMenu', 'handleCollapse', 'handleFulltextChange', 'handleFulltextSearch',
            'handleFulltextPrevItem', 'handleFulltextNextItem');
    }

    componentDidMount() {
        const {versionId, expandedIds} = this.props;
        this.requestFundTreeData(versionId, expandedIds);
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, expandedIds} = nextProps;
        this.requestFundTreeData(versionId, expandedIds);
    }

    requestFundTreeData(versionId, expandedIds) {
        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MOVEMENTS_LEFT, versionId, expandedIds));
    }

    handleFulltextChange(value) {
        this.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId, value));
    }

    handleFulltextSearch() {
        this.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId));
    }

    handleFulltextPrevItem() {
        this.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId));
    }

    /**
     * Zobrazení kontextového menu pro daný uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleContextMenu(node, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
            </ul>
        )

        this.dispatch(fundTreeFocusNode(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId, node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Klik na uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, ensureItemVisible, e) {
        e.shiftKey && this.unFocus()
        this.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId, node.id, e.ctrlKey, e.shiftKey, null, ensureItemVisible));
    }

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges()
        }
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId, this.props.fund))
    }

    render() {
        return (
            <FundTreeLazy
                {...this.props}
                cutLongLabels={true}
                onOpenCloseNode={(node, expand) => {expand ? this.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_MOVEMENTS_LEFT, node)) : this.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_MOVEMENTS_LEFT, this.props.versionId, node))}}
                onContextMenu={this.handleContextMenu}
                onNodeClick={this.handleNodeClick}
                onCollapse={this.handleCollapse}
                onFulltextChange={this.handleFulltextChange}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
            />
        )
    }
}

export default connect()(FundTreeMovementsLeft);

