/**
 * Lišta akcí pro jednotku popisu
 *
 * @author Jakub Randák
 * @author Tomáš Pytelka
 * @since 31.8.2016
 */

import PropTypes from 'prop-types';

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

import './NodeActionsBar.scss';

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

        this.props.dispatch(fundSelectSubNode(versionId, null, node, false, null, false, index));
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
            this.props.dispatch(modalDialogShow(this, i18n('arr.fund.subNodes.findPosition'),
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
        const {simplified, node, selectedSubNodeIndex, versionId, userDetail, fundId, closed, onSwitchNode, arrPerm} = this.props;
        var selectedSubNodeNumber = selectedSubNodeIndex + 1; // pořadí vybraného záznamu v akordeonu
        var gotoTitle = this.isFilterUsed() ? i18n('arr.fund.subNodes.findPosition.filterActive') : i18n('arr.fund.subNodes.findPosition')

        let text = node.nodeCount;
        if (node.selectedSubNodeId && node.nodeIndex !== null) {
            text = (node.nodeIndex + 1) + " / " + text;
        }

        const onNextAction = (e) => {
            if (simplified) onSwitchNode('nextItem', e);
            else this.props.dispatch(fundSubNodesNextPage(versionId, node.id, node.routingKey));
        }

        const onPrevAction = (e) => {
        if (simplified) onSwitchNode('prevItem', e);
        else this.props.dispatch(fundSubNodesPrevPage(versionId, node.id, node.routingKey));
        }

        return(
            <div key='actions' className='node-actions-bar'>
                <div key='actions' className='actions'>
                    <AddNodeCross node={node} selectedSubNodeIndex={selectedSubNodeIndex} versionId={versionId} userDetail={userDetail} fundId={fundId} arrPerm={arrPerm} closed={closed}/>
                    <div className="button-wrap">
                        <div className="left-side">
                            {!simplified &&
                            <Search
                                tabIndex={-1}
                                ref='search'
                                className='search-input'
                                placeholder={i18n('search.input.filter')}
                                value={node.filterText}
                                onClear={() => {this.props.dispatch(fundNodeSubNodeFulltextSearch(''))}}
                                onSearch={(value) => {this.props.dispatch(fundNodeSubNodeFulltextSearch(value))}}
                                filter
                            />
                            }
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
                            disabled={!simplified && node.viewStartIndex === 0}
                            onClick={(e) => onPrevAction(e)}
                            title={i18n(
                                `arr.fund.subNodes.prev${simplified ? '' : 'Page'}`,
                                simplified ? node.pageSize : null
                            )}
                            >
                                <Icon glyph={simplified ? "fa-caret-left" : "fa-backward"} />
                            </div>
                            <div
                            className='btn btn-default'
                            disabled={!simplified && node.viewStartIndex + node.pageSize >= node.nodeCount}
                            onClick={(e) => onNextAction(e)}
                            title={i18n(
                                `arr.fund.subNodes.next${simplified ? '' : 'Page'}`,
                                simplified ? node.pageSize : null
                            )}
                            >
                                <Icon glyph={simplified ? "fa-caret-right" : "fa-forward"} />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
        }
}

NodeActionsBar.propTypes = {
    simplified: PropTypes.bool.isRequired,
    arrPerm: PropTypes.bool.isRequired,
    node: PropTypes.any.isRequired,
    versionId: PropTypes.any.isRequired,
    userDetail: PropTypes.object.isRequired,
    fundId: PropTypes.any.isRequired,
    closed: PropTypes.any.isRequired,
    selectedSubNodeIndex: PropTypes.number.isRequired,
    onSwitchNode: PropTypes.func,
};
export default connect()(NodeActionsBar);
