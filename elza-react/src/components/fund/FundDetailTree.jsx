/**
 * Strom AS.
 */

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n} from 'components/shared';
import * as types from 'actions/constants/ActionTypes.js';
import {Dropdown} from 'react-bootstrap';
import {
    fundTreeCollapse,
    fundTreeFetchIfNeeded,
    fundTreeFocusNode,
    fundTreeFulltextChange,
    fundTreeFulltextNextItem,
    fundTreeFulltextPrevItem,
    fundTreeFulltextSearch,
    fundTreeNodeCollapse,
    fundTreeNodeExpand,
    fundTreeSelectNode,
} from 'actions/arr/fundTree.jsx';
import {fundSelectSubNode} from 'actions/arr/node.jsx';
import {createFundRoot, getParentNode} from './../arr/ArrUtils.jsx';
import {contextMenuHide, contextMenuShow} from 'actions/global/contextMenu.jsx';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx';
import {getFundFromFundAndVersion} from 'components/arr/ArrUtils.jsx';
import {selectFundTab} from 'actions/arr/fund.jsx';
import {routerNavigate} from 'actions/router.jsx';
import FundTreeLazy from '../arr/FundTreeLazy';
import {FOCUS_KEYS} from '../../constants.tsx';

class FundDetailTree extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callFundSelectSubNode', 'handleNodeClick', 'handleNodeDoubleClick', 'handleSelectInNewTab', 'handleSelectInTab',
            'handleContextMenu', 'handleFulltextChange', 'handleFulltextSearch',
            'handleFulltextPrevItem', 'handleFulltextNextItem', 'handleCollapse',
            'trySetFocus');
    }

    componentDidMount() {
        const { versionId, expandedIds } = this.props;
        this.requestFundTreeData(versionId, expandedIds);
        this.trySetFocus(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const { versionId, expandedIds } = nextProps;
        this.requestFundTreeData(versionId, expandedIds);
        this.trySetFocus(nextProps);
    }

    trySetFocus(props) {
        var { focus } = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.tree) {   // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.tree.getWrappedInstance().focus();
                        focusWasSet();
                    });
                }

            } else if (isFocusFor(focus, FOCUS_KEYS.ARR, 1, 'tree') || isFocusFor(focus, FOCUS_KEYS.ARR, 1)) {
                this.setState({}, () => {
                    this.refs.tree.getWrappedInstance().focus();
                    focusWasSet();
                });
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        // if (this.state !== nextState) {
        //     return true;
        // }
        // var eqProps = ['focus', 'ensureItemVisible', 'dirty', 'expandedIds', 'fund', 'fetched', 'searchedIds', 'nodes', 'selectedId', 'selectedIds', 'fetchingIncludeIds', 'filterCurrentIndex', 'filterText', 'focusId', 'isFetching']
        // return !propsEquals(this.props, nextProps, eqProps);
    }

    requestFundTreeData(versionId, expandedIds) {
        this.props.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, versionId, expandedIds));
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
                <Dropdown.Item
                    onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('fundTree.action.openInNewTab')}</Dropdown.Item>
                <Dropdown.Item
                    onClick={this.handleSelectInTab.bind(this, node)}>{i18n('fundTree.action.open')}</Dropdown.Item>
            </ul>
        );

        this.props.dispatch(fundTreeFocusNode(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId, node));
        this.props.dispatch(contextMenuShow(this, menu, { x: e.clientX, y: e.clientY }));
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param node {Object} uzel
     */
    handleSelectInNewTab(node) {
        this.props.dispatch(contextMenuHide());

        this.callFundSelectSubNode(node, true);
    }

    /**
     * Otevření uzlu v aktuální záložce.
     * @param node {Object} uzel
     */
    handleSelectInTab(node) {
        this.props.dispatch(contextMenuHide());

        this.callFundSelectSubNode(node, false);
    }

    /**
     * Otevření uzlu v záložce.
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    callFundSelectSubNode(node, openNewTab) {
        // Přepnutí na stránku pořádání
        this.props.dispatch(routerNavigate('/arr'));

        // Otevření archivního souboru
        const { fund } = this.props;
        var fundObj = getFundFromFundAndVersion(fund, fund.versions[0]);
        this.props.dispatch(selectFundTab(fundObj));

        // Vybrání položky - jako formulář
        var parentNode = getParentNode(node, this.props.nodes);
        if (parentNode == null) {   // root
            parentNode = createFundRoot(this.props.fund);
        }
        this.props.dispatch(fundSelectSubNode(this.props.versionId, node.id, parentNode, openNewTab, null, false));
    }

    /**
     * Označení uzlu ve stromu.
     * @param node
     * @param e
     */
    handleNodeClick(node, ensureItemVisible, e) {
        this.props.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId, node.id, false, false, null));
    }

    /**
     * Otevření uzlu v aktuální záložce (pokud aktuální není, otevře se v nové).
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeDoubleClick(node, ensureItemVisible, e) {
        this.callFundSelectSubNode(node, false, ensureItemVisible);
    }

    handleFulltextChange(value) {
        this.props.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId, value));
    }

    handleFulltextSearch() {
        this.props.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId));
    }

    handleFulltextPrevItem() {
        this.props.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.props.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId));
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.props.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId, this.props.fund));
    }

    render() {
        const { cutLongLabels } = this.props;

        return (
            <FundTreeLazy
                ref='tree'
                {...this.props}
                className={this.props.className}
                cutLongLabels={cutLongLabels}
                onOpenCloseNode={(node, expand) => {
                    expand ? this.props.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, node)) : this.props.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_FUNDS_FUND_DETAIL, this.props.versionId, node));
                }}
                onContextMenu={this.handleContextMenu}
                onNodeClick={this.handleNodeClick}
                onNodeDoubleClick={this.handleNodeDoubleClick}
                onFulltextChange={this.handleFulltextChange}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
                onCollapse={this.handleCollapse}
            />
        );
    }
}

export default connect()(FundDetailTree);

