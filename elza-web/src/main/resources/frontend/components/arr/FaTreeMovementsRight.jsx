/**
 * Strom AP.
 */

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs, FaTreeLazy} from 'components';
import * as types from 'actions/constants/actionTypes';
import {AppActions} from 'stores';
import {MenuItem} from 'react-bootstrap';
import {selectFaTab, closeFaTab} from 'actions/arr/fa'
import {faTreeFulltextChange, faTreeFulltextSearch, faTreeFulltextNextItem, faTreeFulltextPrevItem, faTreeSelectNode, faTreeCollapse, faTreeFocusNode, faTreeFetchIfNeeded, faTreeNodeExpand, faTreeNodeCollapse} from 'actions/arr/faTree'
import {faSelectSubNode} from 'actions/arr/nodes'
import {createFaRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'

var FaTreeMovementsRight = class FaTreeMovementsRight extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleNodeClick', 'handleContextMenu', 'handleCollapse', 'handleFulltextChange', 'handleFulltextSearch',
            'handleFulltextPrevItem', 'handleFulltextNextItem');
    }

    componentDidMount() {
        const {versionId, expandedIds, selectedId} = this.props;
        this.requestFaTreeData(versionId, expandedIds, selectedId);
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, expandedIds, selectedId} = nextProps;
        this.requestFaTreeData(versionId, expandedIds, selectedId);
    }

    requestFaTreeData(versionId, expandedIds, selectedId) {
        this.dispatch(faTreeFetchIfNeeded(types.FA_TREE_AREA_MOVEMENTS_RIGHT, versionId, expandedIds, selectedId));
    }

    handleFulltextChange(value) {
        this.dispatch(faTreeFulltextChange(types.FA_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId, value));
    }

    handleFulltextSearch() {
        this.dispatch(faTreeFulltextSearch(types.FA_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId));
    }

    handleFulltextPrevItem() {
        this.dispatch(faTreeFulltextPrevItem(types.FA_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.dispatch(faTreeFulltextNextItem(types.FA_TREE_AREA_MOVEMENTS_RIGHT, this.props.versionId));
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

        this.dispatch(faTreeFocusNode(types.FA_TREE_AREA_MOVEMENTS_RIGHT, node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Klik na uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, e) {
        this.dispatch(faTreeSelectNode(types.FA_TREE_AREA_MOVEMENTS_RIGHT, node.id, e.ctrlKey, e.shiftKey));
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.dispatch(faTreeCollapse(types.FA_TREE_AREA_MOVEMENTS_RIGHT, this.props.fa))
    }

    render() {
        const {fa} = this.props;

        return (
            <FaTreeLazy 
                {...this.props}
                onOpenCloseNode={(node, expand) => {expand ? this.dispatch(faTreeNodeExpand(types.FA_TREE_AREA_MOVEMENTS_RIGHT, node)) : this.dispatch(faTreeNodeCollapse(types.FA_TREE_AREA_MOVEMENTS_RIGHT, node))}}
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

module.exports = connect()(FaTreeMovementsRight);

