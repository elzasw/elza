/**
 * Strom AS.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';
import { AbstractReactComponent, i18n } from 'components/shared';
import FundTreeLazy from './FundTreeLazy';
import * as types from 'actions/constants/ActionTypes.js';
import { AppActions } from 'stores/index.jsx';

import {
    fundTreeCollapse
} from 'actions/arr/fundTree.jsx';

import { propsEquals } from 'components/Utils.jsx';
import { canSetFocus, focusWasSet, isFocusFor } from 'actions/global/focus.jsx';
import { fundTreeSelectNode } from '../../actions/arr/fundTree';

class FundTreeUsage extends AbstractReactComponent {
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
            'trySetFocus'
        );
    }

    componentDidMount() {
        this.trySetFocus(this.props);
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);
    }

    trySetFocus(props) {
        var { focus } = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {
                // focus po ztrátě
                if (this.refs.treeUsage) {
                    // ještě nemusí existovat
                    this.setState({}, () => {
                        this.refs.treeUsage.getWrappedInstance().focus();
                        focusWasSet();
                    });
                }
            } else if (isFocusFor(focus, 'arr', 1, 'treeUsage') || isFocusFor(focus, 'arr', 1)) {
                this.setState({}, () => {
                    this.refs.treeUsage.getWrappedInstance().focus();
                    focusWasSet();
                });
            }
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        return true;
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = [
            'focus',
            'ensureItemVisible',
            'dirty',
            'expandedIds',
            'fetched',
            'searchedIds',
            'nodes',
            'selectedId',
            'selectedIds',
            'fetchingIncludeIds',
            'filterCurrentIndex',
            'filterText',
            'focusId',
            'isFetching'
        ];
        return !propsEquals(this.props, nextProps, eqProps);
    }

    /**
     * Klik na uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, ensureItemVisible, e) {
        e.shiftKey && this.unFocus();
        this.props.onNodeClick(node);
        console.log(node)
        if (!node.hasChildren) {
            this.dispatch(
                fundTreeSelectNode(
                    types.FUND_TREE_AREA_USAGE,
                    this.props.versionId,
                    node.id,
                    e.ctrlKey,
                    e.shiftKey,
                    null,
                    ensureItemVisible
                )
            );
        }
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
        this.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_USAGE, this.props.versionId, this.props.fund));
    }

    render() {
        const { className, cutLongLabels } = this.props;
        return (
            <FundTreeLazy
                {...this.props}
                showSearch={false}
                showCollapseAll={false}
                ref="treeUsage"
                className={className}
                cutLongLabels={cutLongLabels}
                onOpenCloseNode={this.props.handleOpenCloseNode}
                onContextMenu={this.handleContextMenu}
                onNodeClick={this.handleNodeClick}
                showCountStats={true}
            />
        );
    }
}

export default connect()(FundTreeUsage);
