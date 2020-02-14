/**
 * Strom AS.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n} from 'components/shared';
import FundTreeLazy from './FundTreeLazy';
import ArrSearchForm from './ArrSearchForm';
import * as types from 'actions/constants/ActionTypes.js';
import {MenuItem} from 'react-bootstrap';
import {fundTreeFulltextChange, fundTreeFulltextSearch, fundTreeFocusNode, fundTreeFetchIfNeeded, fundTreeNodeExpand, fundTreeFulltextNextItem, fundTreeFulltextPrevItem, fundTreeNodeCollapse, fundTreeCollapse} from 'actions/arr/fundTree.jsx'
import {fundSelectSubNode} from 'actions/arr/node.jsx'
import {createFundRoot, getParentNode} from './ArrUtils.jsx'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu.jsx'
import {propsEquals} from 'components/Utils.jsx'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {FOCUS_KEYS} from "../../constants.tsx";
import PersistentSortDialog from "./PersisetntSortDialog";
import {WebApi} from "../../actions/WebApi";

class FundTreeMain extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callFundSelectSubNode', 'handleNodeClick', 'handleSelectInNewTab',
        'handleContextMenu', 'handleFulltextChange', 'handleFulltextSearch',
        'handleFulltextPrevItem', 'handleFulltextNextItem', 'handleCollapse',
        'trySetFocus');
    }

    componentDidMount() {
        const {versionId, expandedIds} = this.props;
        this.requestFundTreeData(versionId, expandedIds);
        this.trySetFocus(this.props)
    }

    componentWillReceiveProps(nextProps) {
        const {versionId, expandedIds} = nextProps;
        this.requestFundTreeData(versionId, expandedIds);
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

            } else if (isFocusFor(focus, FOCUS_KEYS.ARR, 1, 'tree') || isFocusFor(focus, FOCUS_KEYS.ARR, 1)) {
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

    requestFundTreeData(versionId, expandedIds) {
        this.props.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_MAIN, versionId, expandedIds));
    }

    /**
     * Zobrazení kontextového menu pro daný uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleContextMenu(node, e) {
        const {readMode} = this.props;
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('fundTree.action.openInNewTab')}</MenuItem>
                {!readMode && <MenuItem onClick={() => this.handleOpenPersistentSortDialog(node)}>{i18n('arr.functions.persistentSort')}</MenuItem>}
                {!readMode && <MenuItem onClick={() => this.computeAndVizualizeEJ(node)}>{i18n('arr.functions.computeAndVizualizeEJ')}</MenuItem>}
            </ul>
        )

        this.props.dispatch(fundTreeFocusNode(types.FUND_TREE_AREA_MAIN, this.props.versionId, node));
        this.props.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param node {Object} uzel
     */
    handleSelectInNewTab(node) {
        this.props.dispatch(contextMenuHide());

        this.callFundSelectSubNode(node, true, false);
    }

    handleOpenPersistentSortDialog = (node) => {
        const {fund} = this.props;
        this.props.dispatch(contextMenuHide());

        this.props.dispatch(modalDialogShow(this, i18n("arr.functions.persistentSort"), <PersistentSortDialog versionId={fund.versionId} node={node}/>));
    };

    computeAndVizualizeEJ = (node) => {
        const {fund} = this.props;
        this.props.dispatch(contextMenuHide());

        WebApi.queueBulkActionWithIds(fund.versionId, "ZP2015_INTRO_VYPOCET_EJ", [node.id]);
    };

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
        this.props.dispatch(fundSelectSubNode(this.props.versionId, node.id, parentNode, openNewTab, null, ensureItemVisible));
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
        this.props.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_MAIN, this.props.versionId, value));
    }

    handleFulltextSearch() {
        const {fund} = this.props;
        this.props.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_MAIN, this.props.versionId, null, fund.fundTree.searchFormData ? fund.fundTree.searchFormData : {type: "FORM"}));
    }

    handleFulltextPrevItem() {
        this.props.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_MAIN, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.props.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_MAIN, this.props.versionId));
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.props.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_MAIN, this.props.versionId, this.props.fund))
    }

    handleExtendedSearch = () => {
        const {fund} = this.props;
        this.props.dispatch(modalDialogShow(this, i18n('search.extended.title'),
            <ArrSearchForm
                onSubmitForm={this.handleExtendedSearchData}
                initialValues={fund.fundTree.searchFormData ? fund.fundTree.searchFormData : {type: "FORM"}}
            />
        ));
    };

    handleExtendedSearchData = (result) => {
        const {versionId} = this.props;
        let params = [];

        switch (result.type) {
            case "FORM": {
                result.condition.forEach((conditionItem, index) => {
                    let param = {};
                    param.type = conditionItem.type;
                    param.value = conditionItem.value;
                    switch (conditionItem.type) {
                        case "TEXT": {
                            param["@class"] = ".TextSearchParam";
                            break;
                        }
                        case "UNITDATE": {
                            param["@class"] = ".UnitdateSearchParam";
                            param.calendarId = parseInt(conditionItem.calendarTypeId);
                            param.condition = conditionItem.condition;
                            break;
                        }
                    }
                    params.push(param);
                });
                break;
            }

            case "TEXT": {
                this.props.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_MAIN, this.props.versionId, result.text));
                break;
            }
        }

        return this.props.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_MAIN, versionId, params, result, true));
    };

    render() {
        const {actionAddons, className, fund, cutLongLabels} = this.props;
        const searchText = typeof fund.fundTree.searchText !== 'undefined'
            ? fund.fundTree.searchText
            : fund.fundTree.filterText;

        return (
            <FundTreeLazy
                ref='tree'
                className={className}
                actionAddons={actionAddons}
                {...this.props}
                cutLongLabels={cutLongLabels}
                onOpenCloseNode={(node, expand) => {expand ? this.props.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_MAIN, node)) : this.props.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_MAIN, this.props.versionId, node))}}
                onContextMenu={this.handleContextMenu}
                onNodeClick={this.handleNodeClick}
                onFulltextChange={this.handleFulltextChange}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
                onCollapse={this.handleCollapse}
                extendedSearch
                filterText={fund.fundTree.luceneQuery ? i18n('search.extended.label') : searchText}
                extendedReadOnly={fund.fundTree.luceneQuery}
                onClickExtendedSearch={this.handleExtendedSearch}
            />
        )
    }
}

export default connect()(FundTreeMain);

