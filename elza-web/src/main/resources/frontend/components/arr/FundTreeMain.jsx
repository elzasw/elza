/**
 * Strom AS.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, FundTreeLazy} from 'components/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';
import {AppActions} from 'stores/index.jsx';
import {MenuItem} from 'react-bootstrap';
import {fundTreeFulltextChange, fundTreeFulltextSearch, fundTreeFocusNode, fundTreeFetchIfNeeded, fundTreeNodeExpand, fundTreeFulltextNextItem, fundTreeFulltextPrevItem, fundTreeNodeCollapse, fundTreeCollapse} from 'actions/arr/fundTree.jsx'
import {fundSelectSubNode} from 'actions/arr/nodes.jsx'
import {createFundRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu.jsx'
import {propsEquals} from 'components/Utils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'

var FundTreeMain = class FundTreeMain extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callFundSelectSubNode', 'handleNodeClick', 'handleSelectInNewTab',
        'handleContextMenu', 'handleFulltextChange', 'handleFulltextSearch',
        'handleFulltextPrevItem', 'handleFulltextNextItem', 'handleCollapse',
        'trySetFocus');
    }

    componentDidMount() {
        const {versionId, expandedIds, selectedId} = this.props;
        this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, expandedIds, selectedId} = nextProps;
        this.requestFundTreeData(versionId, expandedIds, selectedId);
        this.trySetFocus(nextProps)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.tree) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.tree.getWrappedInstance().focus()
                        focusWasSet()
                    })
                }

            } else if (isFocusFor(focus, 'arr', 1, 'tree') || isFocusFor(focus, 'arr', 1)) {
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
        var eqProps = ['focus', 'ensureItemVisible', 'dirty', 'expandedIds', 'fund', 'fetched', 'searchedIds', 'nodes', 'selectedId', 'selectedIds', 'fetchingIncludeIds', 'filterCurrentIndex', 'filterText', 'focusId', 'isFetching']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    requestFundTreeData(versionId, expandedIds, selectedId) {
        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MAIN, versionId, expandedIds, selectedId));
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
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('fundTree.action.openInNewTab')}</MenuItem>
            </ul>
        )

        this.dispatch(fundTreeFocusNode(types.FUND_TREE_AREA_MAIN, this.props.versionId, node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param node {Object} uzel
     */
    handleSelectInNewTab(node) {
        this.dispatch(contextMenuHide());

        this.callFundSelectSubNode(node, true, false);
    }

    /**
     * Otevření uzlu v záložce.
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    callFundSelectSubNode(node, openNewTab, ensureItemVisible) {
        var parentNode = getParentNode(node, this.props.nodes);
        if (parentNode == null) {   // root
            parentNode = createFundRoot(this.props.fund);
        }
        this.dispatch(fundSelectSubNode(this.props.versionId, node.id, parentNode, openNewTab, null, ensureItemVisible));
    }

    /**
     * Otevření uzlu v aktuální záložce (pokud aktuální není, otevře se v nové).
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, ensureItemVisible, e) {
        this.callFundSelectSubNode(node, false, ensureItemVisible);
    }

    handleFulltextChange(value) {
        this.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_MAIN, this.props.versionId, value));
    }

    handleFulltextSearch() {
        this.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_MAIN, this.props.versionId));
    }

    handleFulltextPrevItem() {
        this.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_MAIN, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_MAIN, this.props.versionId));
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_MAIN, this.props.versionId, this.props.fund))
    }

    render() {
        const {fund, cutLongLabels} = this.props;

        return (
            <FundTreeLazy 
                ref='tree'
                {...this.props}
                cutLongLabels={cutLongLabels}
                onOpenCloseNode={(node, expand) => {expand ? this.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_MAIN, node)) : this.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_MAIN, this.props.versionId, node))}}
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

module.exports = connect()(FundTreeMain);

