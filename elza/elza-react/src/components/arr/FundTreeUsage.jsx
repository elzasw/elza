/**
 * Strom AS.
 */

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent} from 'components/shared';
import FundTreeLazy from './FundTreeLazy';
import * as types from 'actions/constants/ActionTypes.js';

import {fundTreeCollapse} from 'actions/arr/fundTree.jsx';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx';
import {FOCUS_KEYS} from '../../constants.tsx';

class FundTreeUsage extends AbstractReactComponent {

    treeRef = null;

    componentDidMount() {
        this.trySetFocus(this.props);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps);
    }

    trySetFocus = (props) => {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {
                // focus po ztrátě
                if (this.treeRef) {
                    // ještě nemusí existovat
                    this.setState({}, () => {
                        this.treeRef.getWrappedInstance().focus();
                        focusWasSet();
                    });
                }
            } else if (isFocusFor(focus, FOCUS_KEYS.ARR, 1, 'treeUsage') || isFocusFor(focus, FOCUS_KEYS.ARR, 1)) {
                this.setState({}, () => {
                    this.treeRef.getWrappedInstance().focus();
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
        // var eqProps = [
        //     'focus',
        //     'ensureItemVisible',
        //     'dirty',
        //     'expandedIds',
        //     'fetched',
        //     'searchedIds',
        //     'nodes',
        //     'selectedId',
        //     'selectedIds',
        //     'fetchingIncludeIds',
        //     'filterCurrentIndex',
        //     'filterText',
        //     'focusId',
        //     'isFetching'
        // ];
        // return !propsEquals(this.props, nextProps, eqProps);
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
    handleCollapse = () => {
        this.props.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_USAGE, this.props.versionId, this.props.fund));
    }

    render() {
        const {className, cutLongLabels} = this.props;
        return (
            <FundTreeLazy
                {...this.props}
                showSearch={false}
                showCollapseAll={false}
                ref={ref => this.treeRef = ref}
                className={className}
                cutLongLabels={cutLongLabels}
                onOpenCloseNode={this.props.handleOpenCloseNode}
                onContextMenu={this.handleContextMenu}
                showCountStats={true}
                onLinkClick={this.props.onLinkClick}
            />
        );
    }
}

export default connect(null, null, null, {forwardRef: true})(FundTreeUsage);
