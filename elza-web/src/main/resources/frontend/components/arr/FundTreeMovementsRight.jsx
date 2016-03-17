/**
 * Strom AS.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs, FundTreeLazy} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {AppActions} from 'stores';
import {MenuItem} from 'react-bootstrap';
import {selectFundTab, closeFundTab} from 'actions/arr/fund'
import {fundTreeFulltextChange, fundTreeFulltextSearch, fundTreeFulltextNextItem, fundTreeFulltextPrevItem, fundTreeSelectNode, fundTreeCollapse, fundTreeFocusNode, fundTreeFetchIfNeeded, fundTreeNodeExpand, fundTreeNodeCollapse} from 'actions/arr/fundTree'
import {createFundRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'

var FundTreeMovementsRight = class FundTreeMovementsRight extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleNodeClick', 'handleContextMenu', 'handleCollapse', 'handleFulltextChange', 'handleFulltextSearch',
            'handleFulltextPrevItem', 'handleFulltextNextItem');
    }

    componentDidMount() {
        const {versionId, expandedIds, selectedId} = this.props;
        this.requestFundTreeData(versionId, expandedIds, selectedId);
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, expandedIds, selectedId} = nextProps;
        this.requestFundTreeData(versionId, expandedIds, selectedId);
    }

    requestFundTreeData(versionId, expandedIds, selectedId) {
        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, versionId, expandedIds, selectedId));
    }

    handleFulltextChange(value) {
        this.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId, value));
    }

    handleFulltextSearch() {
        this.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId));
    }

    handleFulltextPrevItem() {
        this.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId));
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

        this.dispatch(fundTreeFocusNode(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId, node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Klik na uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, e) {
        this.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId, node.id, false, false));
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId, this.props.fund))
    }

    render() {
        const {fund} = this.props;

        return (
            <FundTreeLazy 
                {...this.props}
                cutLongLabels={true}
                onOpenCloseNode={(node, expand) => {expand ? this.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, node)) : this.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId, node))}}
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

module.exports = connect()(FundTreeMovementsRight);

