/**
 * Strom AS.
 */

import React from 'react';
import { connect } from 'react-redux';
import { AbstractReactComponent } from 'components/shared';
import FundTreeLazy from './FundTreeLazy';
import * as types from 'actions/constants/ActionTypes.js';
import {
    fundTreeCollapse,
    fundTreeFetchIfNeeded,
    fundTreeFulltextChange,
    fundTreeFulltextNextItem,
    fundTreeFulltextPrevItem,
    fundTreeFulltextSearch,
    fundTreeNodeCollapse,
    fundTreeNodeExpand,
} from 'actions/arr/fundTree.jsx';
import { canSetFocus, focusWasSet, isFocusFor } from 'actions/global/focus.jsx';

import { fundTreeSelectNode } from '../../actions/arr/fundTree';
import { FOCUS_KEYS } from '../../constants.tsx';

class FundTreeCopy extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'callFundSelectSubNode',
            'handleNodeClick',
            'handleSelectInNewTab',
            'handleContextMenu',
            'handleFulltextChange',
            'handleFulltextSearch',
            'handleFulltextPrevItem',
            'handleFulltextNextItem',
            'handleCollapse',
            'trySetFocus',
        );
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
            if (isFocusFor(focus, null, 1)) {
                // focus po ztrátě
                if (this.refs.treeCopy) {
                    // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.treeCopy.getWrappedInstance().focus();
                        focusWasSet();
                    });
                }
            } else if (
                isFocusFor(focus, FOCUS_KEYS.ARR, 1, 'treeCopy') ||
                isFocusFor(focus, FOCUS_KEYS.ARR, 1)
            ) {
                this.setState({}, () => {
                    this.refs.treeCopy.getWrappedInstance().focus();
                    focusWasSet();
                });
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        // if (this.state !== nextState) {
        //   return true;
        // }
        // var eqProps = [
        //   'focus',
        //   'ensureItemVisible',
        //   'dirty',
        //   'expandedIds',
        //   'fund',
        //   'fetched',
        //   'searchedIds',
        //   'nodes',
        //   'selectedId',
        //   'selectedIds',
        //   'fetchingIncludeIds',
        //   'filterCurrentIndex',
        //   'filterText',
        //   'focusId',
        //   'isFetching'
        // ];
        // return !propsEquals(this.props, nextProps, eqProps);
    }

    requestFundTreeData(versionId, expandedIds) {
        this.props.dispatch(
            fundTreeFetchIfNeeded(types.FUND_TREE_AREA_COPY, versionId, expandedIds),
        );
    }

    /**
     * Klik na uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, ensureItemVisible, e) {
        e.shiftKey && this.unFocus();
        this.props.dispatch(
            fundTreeSelectNode(
                types.FUND_TREE_AREA_COPY,
                this.props.versionId,
                node.id,
                e.ctrlKey,
                e.shiftKey,
                null,
                ensureItemVisible,
            ),
        );
    }

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges();
        }
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.props.dispatch(
            fundTreeCollapse(
                types.FUND_TREE_AREA_COPY,
                this.props.versionId,
                this.props.fund,
            ),
        );
    }

    handleFulltextChange(value) {
        this.props.dispatch(
            fundTreeFulltextChange(
                types.FUND_TREE_AREA_COPY,
                this.props.versionId,
                value,
            ),
        );
    }

    handleFulltextSearch() {
        const { searchFormData } = this.props;

        this.props.dispatch(
            fundTreeFulltextSearch(
                types.FUND_TREE_AREA_COPY,
                this.props.versionId,
                null,
                searchFormData
                    ? searchFormData
                    : { type: 'FORM' },
            ),
        );
    }

    handleFulltextPrevItem() {
        this.props.dispatch(
            fundTreeFulltextPrevItem(types.FUND_TREE_AREA_COPY, this.props.versionId),
        );
    }

    handleFulltextNextItem() {
        this.props.dispatch(
            fundTreeFulltextNextItem(types.FUND_TREE_AREA_COPY, this.props.versionId),
        );
    }

    render() {
        const { actionAddons, className, cutLongLabels } = this.props;
        return (
            <FundTreeLazy
                ref="treeCopy"
                className={className}
                actionAddons={actionAddons}
                {...this.props}
                cutLongLabels={cutLongLabels}
                onOpenCloseNode={(node, expand) => {
                    expand
                        ? this.props.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_COPY, node))
                        : this.props.dispatch(
                        fundTreeNodeCollapse(
                            types.FUND_TREE_AREA_COPY,
                            this.props.versionId,
                            node,
                        ),
                        );
                }}
                onContextMenu={this.handleContextMenu}
                onNodeClick={this.handleNodeClick}
                onCollapse={this.handleCollapse}
                onFulltextChange={this.handleFulltextChange}
                onFulltextSearch={this.handleFulltextSearch}
                onFulltextPrevItem={this.handleFulltextPrevItem}
                onFulltextNextItem={this.handleFulltextNextItem}
            />
        );
    }
}

export default connect()(FundTreeCopy);
