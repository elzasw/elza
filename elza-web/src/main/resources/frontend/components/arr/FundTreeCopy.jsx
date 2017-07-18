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
import {fundTreeSelectNode} from "../../actions/arr/fundTree";

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
    const { fund } = this.props;
    this.dispatch(
      fundTreeFulltextSearch(
        types.FUND_TREE_AREA_COPY,
        this.props.versionId,
        null,
        fund.fundTree.searchFormData
          ? fund.fundTree.searchFormData
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

  handleExtendedSearch = () => {
    const { fund } = this.props;
    this.dispatch(
      modalDialogShow(
        this,
        i18n('search.extended.title'),
        <ArrSearchForm
          onSubmitForm={this.handleExtendedSearchData}
          initialValues={
            fund.fundTree.searchFormData
              ? fund.fundTree.searchFormData
              : { type: 'FORM' }
          }
        />
      )
    );
  };

  handleExtendedSearchData = result => {
    const { versionId } = this.props;
    let params = [];

    switch (result.type) {
      case 'FORM': {
        result.condition.forEach((conditionItem, index) => {
          let param = {};
          param.type = conditionItem.type;
          param.value = conditionItem.value;
          switch (conditionItem.type) {
            case 'TEXT': {
              param['@class'] = '.TextSearchParam';
              break;
            }
            case 'UNITDATE': {
              param['@class'] = '.UnitdateSearchParam';
              param.calendarId = parseInt(conditionItem.calendarTypeId);
              param.condition = conditionItem.condition;
              break;
            }
          }
          params.push(param);
        });
        break;
      }

      case 'TEXT': {
        this.dispatch(
          fundTreeFulltextChange(
            types.FUND_TREE_AREA_COPY,
            this.props.versionId,
            result.text
          )
        );
        break;
      }
    }

    return this.dispatch(
      fundTreeFulltextSearch(
        types.FUND_TREE_AREA_COPY,
        versionId,
        params,
        result,
        true
      )
    );
  };

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
        extendedReadOnly={fund.fundTree.luceneQuery}
        onClickExtendedSearch={this.handleExtendedSearch}
      />
    );
  }
}

export default connect()(FundTreeCopy);
