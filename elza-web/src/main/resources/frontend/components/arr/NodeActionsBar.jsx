/**
 * Lišta akcí pro jednotku popisu
 *
 * @author Jakub Randák
 * @author Tomáš Pytelka
 * @since 31.8.2016
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {WebApi} from 'actions/index.jsx';
import {isFundRootId} from './ArrUtils.jsx';
import {getOneSettings} from 'components/arr/ArrUtils.jsx';
import {fundNodeSubNodeFulltextSearch, fundSubNodesNextPage, fundSubNodesPrevPage} from 'actions/arr/node.jsx';
import {Icon, AbstractReactComponent, i18n, Loading, AddNodeCross, Search, GoToPositionForm} from 'components';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx';
import {fundSelectSubNode} from 'actions/arr/nodes.jsx';

require ('./NodePanel.less');
require ('./NodeActionsBar.less');

const NodeActionsBar = class NodeActionsBar extends AbstractReactComponent {
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

        var index = form.position - 1;
        var subNodeId = node.childNodes[index].id;

        this.dispatch(fundSelectSubNode(versionId, subNodeId, node));
    }

    /**
     * Akce pro vybrání JP podle pozice.
     */
    handleFindPosition() {
        const {node} = this.props;

        var count = 0;
        if (node.childNodes) {
            count = node.childNodes.length;
        }

        this.dispatch(modalDialogShow(this, i18n('arr.fund.subNodes.findPosition'),
                        <GoToPositionForm onSubmitForm={this.handleFindPositionSubmit} maxPosition={count} />
                )
        )
    }

    render() {
      const {node, selectedSubNodeIndex, versionId, userDetail, fundId, closed} = this.props;
      var selectedSubNodeNumber = selectedSubNodeIndex + 1; // pořadí vybraného záznamu v akordeonu

      return(
        <div key='actions' className='actions-container'>
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
                        {selectedSubNodeNumber} / {node.childNodes.length}
                        </div>
                        <div
                          className='btn btn-default'
                          onClick={this.handleFindPosition}
                          title={i18n('arr.fund.subNodes.findPosition')}
                        >
                            <Icon glyph="fa-hand-o-down" />
                        </div>
                        <div
                          className='btn btn-default'
                          disabled={node.viewStartIndex == 0}
                          onClick={()=>this.dispatch(fundSubNodesPrevPage(versionId, node.id, node.routingKey))}
                        >
                            <Icon glyph="fa-backward" />
                        </div>
                        <div
                          className='btn btn-default'
                          disabled={node.viewStartIndex + node.pageSize >= node.childNodes.length}
                          onClick={()=>this.dispatch(fundSubNodesNextPage(versionId, node.id, node.routingKey))}
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
module.exports = connect()(NodeActionsBar);
