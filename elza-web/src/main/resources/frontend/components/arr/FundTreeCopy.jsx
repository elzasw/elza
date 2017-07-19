/**
 * Strom AS.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';
import { AbstractReactComponent, i18n } from 'components/shared';
import FundTreeLazy from './FundTreeLazy';
import ArrSearchForm from './ArrSearchForm';
import * as types from 'actions/constants/ActionTypes.js';
import { AppActions } from 'stores/index.jsx';
import { MenuItem } from 'react-bootstrap';
import {
  fundTreeFulltextChange,
  fundTreeFulltextSearch,
  fundTreeFocusNode,
  fundTreeFetchIfNeeded,
  fundTreeNodeExpand,
  fundTreeFulltextNextItem,
  fundTreeFulltextPrevItem,
  fundTreeNodeCollapse,
  fundTreeCollapse
} from 'actions/arr/fundTree.jsx';
import { fundSelectSubNode } from 'actions/arr/node.jsx';
import { createFundRoot, getParentNode } from './ArrUtils.jsx';
import {
  contextMenuShow,
  contextMenuHide
} from 'actions/global/contextMenu.jsx';
import { propsEquals } from 'components/Utils.jsx';
import { canSetFocus, focusWasSet, isFocusFor } from 'actions/global/focus.jsx';
import {
  modalDialogShow,
  modalDialogHide
} from 'actions/global/modalDialog.jsx';
import { fundTreeSelectNode } from '../../actions/arr/fundTree';
import {fundExtendedView} from "../../actions/arr/fundExtended";

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
      'trySetFocus'
    );
  }

  componentDidMount() {
    const { versionId, expandedIds } = this.props;
    this.requestFundTreeData(versionId, expandedIds);
    this.trySetFocus(this.props);
  }

  componentWillReceiveProps(nextProps) {
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
        isFocusFor(focus, 'arr', 1, 'treeCopy') ||
        isFocusFor(focus, 'arr', 1)
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
    if (this.state !== nextState) {
      return true;
    }
    var eqProps = [
      'focus',
      'ensureItemVisible',
      'dirty',
      'expandedIds',
      'fund',
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

  requestFundTreeData(versionId, expandedIds) {
    this.dispatch(
      fundTreeFetchIfNeeded(types.FUND_TREE_AREA_COPY, versionId, expandedIds)
    );
  }

  /**
     * Klik na uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
  handleNodeClick(node, ensureItemVisible, e) {
    e.shiftKey && this.unFocus();
    this.dispatch(
      fundTreeSelectNode(
        types.FUND_TREE_AREA_COPY,
        this.props.versionId,
        node.id,
        e.ctrlKey,
        e.shiftKey,
        null,
        ensureItemVisible
      )
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
    this.dispatch(
      fundTreeCollapse(
        types.FUND_TREE_AREA_COPY,
        this.props.versionId,
        this.props.fund
      )
    );
  }

  handleFulltextChange(value) {
    this.dispatch(
      fundTreeFulltextChange(
        types.FUND_TREE_AREA_COPY,
        this.props.versionId,
        value
      )
    );
  }

  handleFulltextSearch() {
    const { searchFormData } = this.props;
      this.dispatch(fundExtendedView(false));

      this.dispatch(
      fundTreeFulltextSearch(
        types.FUND_TREE_AREA_COPY,
        this.props.versionId,
        null,
        searchFormData
          ? searchFormData
          : { type: 'FORM' }
      )
    );
  }

  handleFulltextPrevItem() {
    this.dispatch(
      fundTreeFulltextPrevItem(types.FUND_TREE_AREA_COPY, this.props.versionId)
    );
  }

  handleFulltextNextItem() {
    this.dispatch(
      fundTreeFulltextNextItem(types.FUND_TREE_AREA_COPY, this.props.versionId)
    );
  }

  render() {
    const { actionAddons, className, fund, cutLongLabels } = this.props;
    return (
      <FundTreeLazy
        ref="treeCopy"
        className={className}
        actionAddons={actionAddons}
        {...this.props}
        cutLongLabels={cutLongLabels}
        onOpenCloseNode={(node, expand) => {
          expand
            ? this.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_COPY, node))
            : this.dispatch(
                fundTreeNodeCollapse(
                  types.FUND_TREE_AREA_COPY,
                  this.props.versionId,
                  node
                )
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
