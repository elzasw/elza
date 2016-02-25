/**
 * Strom AP.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Tabs, FaTreeLazy} from 'components';
import * as types from 'actions/constants/ActionTypes';
import {AppActions} from 'stores';
import {MenuItem} from 'react-bootstrap';
import {faTreeFulltextChange, faTreeFulltextSearch, faTreeFocusNode, faTreeFetchIfNeeded, faTreeNodeExpand, faTreeFulltextNextItem, faTreeFulltextPrevItem, faTreeNodeCollapse, faTreeCollapse} from 'actions/arr/faTree'
import {faSelectSubNode} from 'actions/arr/nodes'
import {createFaRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'
import {propsEquals} from 'components/Utils'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

var FaTreeMain = class FaTreeMain extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callFaSelectSubNode', 'handleNodeClick', 'handleSelectInNewTab',
        'handleContextMenu', 'handleFulltextChange', 'handleFulltextSearch',
        'handleFulltextPrevItem', 'handleFulltextNextItem', 'handleCollapse',
        'trySetFocus');
    }

    componentDidMount() {
        const {versionId, expandedIds, selectedId} = this.props;
        this.requestFaTreeData(versionId, expandedIds, selectedId);
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, expandedIds, selectedId} = nextProps;
        this.requestFaTreeData(versionId, expandedIds, selectedId);
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'arr', 1, 'tree') || isFocusFor(focus, 'arr', 1)) {
                this.setState({}, () => {
                   this.refs.tree.getWrappedInstance().focus()
                   focusWasSet()
                })
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
return true
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['focus', 'ensureItemVisible', 'dirty', 'expandedIds', 'fa', 'fetched', 'searchedIds', 'nodes', 'selectedId', 'selectedIds', 'fetchingIncludeIds', 'filterCurrentIndex', 'filterText', 'focusId', 'isFetching']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    requestFaTreeData(versionId, expandedIds, selectedId) {
        this.dispatch(faTreeFetchIfNeeded(types.FA_TREE_AREA_MAIN, versionId, expandedIds, selectedId));
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
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('faTree.action.openInNewTab')}</MenuItem>
            </ul>
        )

        this.dispatch(faTreeFocusNode(types.FA_TREE_AREA_MAIN, node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param node {Object} uzel
     */
    handleSelectInNewTab(node) {
        this.dispatch(contextMenuHide());

        this.callFaSelectSubNode(node, true);
    }

    /**
     * Otevření uzlu v záložce.
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    callFaSelectSubNode(node, openNewTab) {
        var parentNode = getParentNode(node, this.props.nodes);
        if (parentNode == null) {   // root
            parentNode = createFaRoot(this.props.fa);
        }
        this.dispatch(faSelectSubNode(node.id, parentNode, openNewTab, null, false));
    }

    /**
     * Otevření uzlu v aktuální záložce (pokud aktuální není, otevře se v nové).
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, e) {
        this.callFaSelectSubNode(node, false);
    }

    handleFulltextChange(value) {
        this.dispatch(faTreeFulltextChange(types.FA_TREE_AREA_MAIN, this.props.versionId, value));
    }

    handleFulltextSearch() {
        this.dispatch(faTreeFulltextSearch(types.FA_TREE_AREA_MAIN, this.props.versionId));
    }

    handleFulltextPrevItem() {
        this.dispatch(faTreeFulltextPrevItem(types.FA_TREE_AREA_MAIN, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.dispatch(faTreeFulltextNextItem(types.FA_TREE_AREA_MAIN, this.props.versionId));
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.dispatch(faTreeCollapse(types.FA_TREE_AREA_MAIN, this.props.fa))
    }

    render() {
        const {fa, cutLongLabels} = this.props;

        return (
            <FaTreeLazy 
                ref='tree'
                {...this.props}
                cutLongLabels={cutLongLabels}
                onOpenCloseNode={(node, expand) => {expand ? this.dispatch(faTreeNodeExpand(types.FA_TREE_AREA_MAIN, node)) : this.dispatch(faTreeNodeCollapse(types.FA_TREE_AREA_MAIN, node))}}
                onContextMenu={this.handleContextMenu}
                onNodeClick={this.handleNodeClick}
                onFulltextChange={this.handleFulltextChange}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
                onCollapse={this.handleCollapse}
            />
        )
    }
}

module.exports = connect()(FaTreeMain);

