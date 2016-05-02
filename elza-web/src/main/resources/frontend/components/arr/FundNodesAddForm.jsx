/**
 * Formulář přidání uzlů verse souboru - výběr uzlů ve stromu z jedné úrovně.
 */

require('./FundNodesAddForm.less')

import React from 'react';
import {connect} from 'react-redux'
import {i18n, FundTreeLazy, AbstractReactComponent} from 'components/index.jsx';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Modal, Button, Input} from 'react-bootstrap';
import * as types from 'actions/constants/ActionTypes.js';
import {
    fundTreeFulltextChange,
    fundTreeFulltextSearch,
    fundTreeFulltextNextItem,
    fundTreeFulltextPrevItem,
    fundTreeSelectNode,
    fundTreeCollapse,
    fundTreeFocusNode,
    fundTreeFetchIfNeeded,
    fundTreeNodeExpand,
    fundTreeNodeCollapse
} from 'actions/arr/fundTree.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'

var FundNodesAddForm = class FundNodesAddForm extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSubmit', 'requestFundTreeData', 'getActiveFund',
            'handleFulltextChange', 'handleFulltextSearch', 'handleFulltextPrevItem', 'handleFulltextNextItem', 'handleCollapse',
        'handleNodeClick');

        this.state = {
            nodes: {}
        }
    }

    componentDidMount() {
        const fund = this.getActiveFund(this.props)
        const fundTreeNodes = fund.fundTreeNodes
        const versionId = fund.versionId
        this.requestFundTreeData(versionId, fundTreeNodes.expandedIds, fundTreeNodes.selectedIds);
    }

    componentWillReceiveProps(nextProps) {
        const fund = this.getActiveFund(nextProps)
        const fundTreeNodes = fund.fundTreeNodes
        const versionId = fund.versionId
        this.requestFundTreeData(versionId, fundTreeNodes.expandedIds, fundTreeNodes.selectedIds);
    }

    requestFundTreeData(versionId, expandedIds, selectedIds) {
        var selectedId = null;
        if (Object.keys(selectedIds).length == 1) {
            selectedId = Object.keys(selectedIds)[0];
        }

        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_NODES, versionId, expandedIds, selectedId));
    }

    handleNodeClick(node, ensureItemVisible, e) {
        const fund = this.getActiveFund(this.props)
        e.shiftKey && this.unFocus()
        this.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_NODES, fund.versionId, node.id, e.ctrlKey, e.shiftKey, null, ensureItemVisible));
    }

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges()
        }
    }    

    handleFulltextChange(value) {
        const fund = this.getActiveFund(this.props)
        this.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_NODES, fund.versionId, value));
    }

    handleFulltextSearch() {
        const fund = this.getActiveFund(this.props)
        this.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_NODES, fund.versionId));
    }

    handleFulltextPrevItem() {
        const fund = this.getActiveFund(this.props)
        this.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_NODES, fund.versionId));
    }

    handleFulltextNextItem() {
        const fund = this.getActiveFund(this.props)
        this.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_NODES, fund.versionId));
    }

    handleSubmit() {
        const {onSubmitForm} = this.props
        const fund = this.getActiveFund(this.props)
        const fundTreeNodes = fund.fundTreeNodes
        
        const nodesMap = getMapFromList(fundTreeNodes.nodes)
        const nodes = Object.keys(fundTreeNodes.selectedIds).map(id => nodesMap[id])
        
        onSubmitForm(Object.keys(fundTreeNodes.selectedIds), nodes)
    }

    getActiveFund(props) {
        var arrRegion = props.arrRegion;
        var activeFund = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
        }
        return activeFund
    }

    handleCollapse() {
        const fund = this.getActiveFund(this.props)
        this.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_NODES,fund.versionId, fund))
    }

    render() {
        const { onClose} = this.props
        const fund = this.getActiveFund(this.props)
        const fundTreeNodes = fund.fundTreeNodes
        const versionId = fund.versionId

        return (
            <div className="add-nodes-form-container">
                <Modal.Body>
                    <FundTreeLazy
                        ref='tree'
                        {...fundTreeNodes}
                        cutLongLabels={true}
                        onOpenCloseNode={(node, expand) => {expand ? this.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_NODES, node)) : this.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_NODES, versionId, node))}}
                        onNodeClick={this.handleNodeClick}
                        onFulltextChange={this.handleFulltextChange}
                        onFulltextSearch={this.handleFulltextSearch}
                        onFulltextPrevItem={this.handleFulltextPrevItem}
                        onFulltextNextItem={this.handleFulltextNextItem}
                        onCollapse={this.handleCollapse}
                    />
                </Modal.Body>
                <Modal.Footer>
                    <Button disabled={Object.keys(fundTreeNodes.selectedIds).length === 0} onClick={this.handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

FundNodesAddForm.propTypes = {

}

function mapStateToProps(state) {
    const {arrRegion} = state
    return {
        arrRegion,
    }
}

module.exports = connect(mapStateToProps)(FundNodesAddForm);

