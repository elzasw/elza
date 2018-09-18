/**
 * Lišta akcí pro jednotku popisu
 *
 * @author Jakub Randák
 * @author Tomáš Pytelka
 * @since 31.8.2016
 */

import React from 'react';
import {connect} from 'react-redux'
import {WebApi} from 'actions/index.jsx';
import {getOneSettings} from 'components/arr/ArrUtils.jsx';
import {
    fundNodeSubNodeFulltextSearch,
    fundSelectSubNode,
    fundSubNodesNextPage,
    fundSubNodesPrevPage
} from 'actions/arr/node.jsx';
import {AbstractReactComponent, i18n, Icon, Loading, Search} from 'components/shared';
import AddNodeCross from './AddNodeCross'
import GoToPositionForm from './GoToPositionForm'
import {modalDialogHide, modalDialogShow} from 'actions/global/modalDialog.jsx';

import './NodeActionsBar.less';

class NodeActionsBar extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('handleFindPosition', 'handleFindPositionSubmit');
    }

    /**
     * Akce po úspěšném vybrání pozice JP z formuláře.
     *
     * @param form data z formuláře
     */
    handleFindPositionSubmit(form) {
        const {node, versionId} = this.props;

        const index = form.position - 1;

        this.dispatch(fundSelectSubNode(versionId, null, node, false, null, false, index));
    }

    /**
     * Akce pro vybrání JP podle pozice.
     */
    handleFindPosition() {
        const {node} = this.props;

        if(!this.isFilterUsed()){ // Pokud je aktivní filtr je goto zakázáno
            let count = 0;
            if (node.nodeCount) {
                count = node.nodeCount;
            }
            this.dispatch(modalDialogShow(this, i18n('arr.fund.subNodes.findPosition'),
                    <GoToPositionForm onSubmitForm={this.handleFindPositionSubmit} maxPosition={count} />
                )
            )
        }
    }
    /**
     * Akce kontrolující zda je na uzly použit filtr
     * @return {bool} nodesFiltered
     */
    isFilterUsed(){
        const {node} = this.props;
        var nodesFiltered = node.filterText ? true : false;
        return nodesFiltered;
    }
    render() {
      const {node, selectedSubNodeIndex, versionId, userDetail, fundId, closed} = this.props;
      var selectedSubNodeNumber = selectedSubNodeIndex + 1; // pořadí vybraného záznamu v akordeonu
      var gotoTitle = this.isFilterUsed() ? i18n('arr.fund.subNodes.findPosition.filterActive') : i18n('arr.fund.subNodes.findPosition')

      let text = node.nodeCount;
      if (node.selectedSubNodeId && node.nodeIndex !== null) {
          text = (node.nodeIndex + 1) + " / " + text;
      }

      return(
        <div key='actions' className='node-actions-bar'>
            <div key='actions' className='actions'>
                <AddNodeCross node={node} selectedSubNodeIndex={selectedSubNodeIndex} versionId={versionId} userDetail={userDetail} fundId={fundId} closed={closed}/>
                <div className="button-wrap">
                    <div className="left-side">
                        <Search
                            tabIndex={-1}
                            ref='search'
                            className='search-input'
                            placeholder={i18n('search.input.filter')}
                            value={node.filterText}
                            onClear={() => {this.dispatch(fundNodeSubNodeFulltextSearch(''))}}
                            onSearch={(value) => {this.dispatch(fundNodeSubNodeFulltextSearch(value))}}
                            filter
                        />
                    </div>
                    <div className="right-side">
                        <div>
                            {text}
                        </div>
                        <div
                          className='btn btn-default'
                          onClick={this.handleFindPosition}
                          disabled={this.isFilterUsed()}
                          title={gotoTitle}
                        >
                            <Icon glyph="fa-hand-o-down" />
                        </div>
                        <div
                          className='btn btn-default'
                          disabled={node.viewStartIndex == 0}
                          onClick={()=>this.dispatch(fundSubNodesPrevPage(versionId, node.id, node.routingKey))}
                          title={i18n('arr.fund.subNodes.prevPage',node.pageSize)}
                        >
                            <Icon glyph="fa-backward" />
                        </div>
                        <div
                          className='btn btn-default'
                          disabled={node.viewStartIndex + node.pageSize >= node.nodeCount}
                          onClick={()=>this.dispatch(fundSubNodesNextPage(versionId, node.id, node.routingKey))}
                          title={i18n('arr.fund.subNodes.nextPage',node.pageSize)}
                        >
                            <Icon glyph="fa-forward" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
      );
    }
};

NodeActionsBar.propTypes = {
      node: React.PropTypes.any.isRequired,
      versionId: React.PropTypes.any.isRequired,
      userDetail: React.PropTypes.object.isRequired,
      fundId: React.PropTypes.any.isRequired,
      closed: React.PropTypes.any.isRequired,
      selectedSubNodeIndex: React.PropTypes.number.isRequired
};
export default connect()(NodeActionsBar);
