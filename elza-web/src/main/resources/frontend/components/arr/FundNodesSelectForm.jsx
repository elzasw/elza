
import React from 'react';
import {connect} from 'react-redux'
import {i18n, FundTreeLazy, AbstractReactComponent} from 'components/index.jsx';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Modal, Button, Input, Form} from 'react-bootstrap';
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
    fundTreeNodeCollapse,
    fundTreeConfigure
} from 'actions/arr/fundTree.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import './FundNodesSelectForm.less';

/**
 * Formulář vybrání uzlů v konkrétní verzi souboru - výběr uzlů na základě konfigurace - např. single nebo multiple select.
 * Implicitně pokud se neuvede, je výběr multiselect libovolných položek ve stromu.
 */
class FundNodesSelectForm extends AbstractReactComponent {

    static propTypes = {
        multipleSelection: React.PropTypes.bool,
        multipleSelectionOneLevel: React.PropTypes.bool,
    };

    static defaultProps = {
        multipleSelection: true,
        multipleSelectionOneLevel: false,
    };

    state = {
        nodes: {}
    };

    componentDidMount() {
        const fund = this.getActiveFund(this.props);
        const fundTreeNodes = fund.fundTreeNodes;
        const versionId = fund.versionId;

        const {multipleSelection, multipleSelectionOneLevel} = this.props;
        this.props.dispatch(fundTreeConfigure(types.FUND_TREE_AREA_NODES, versionId, multipleSelection, multipleSelectionOneLevel));

        this.requestFundTreeData(versionId, fundTreeNodes.expandedIds, fundTreeNodes.selectedIds);
    }

    componentWillReceiveProps(nextProps){
        const fund = this.getActiveFund(nextProps);
        const fundTreeNodes = fund.fundTreeNodes;
        const versionId = fund.versionId;
        this.requestFundTreeData(versionId, fundTreeNodes.expandedIds, fundTreeNodes.selectedIds);
    };

    requestFundTreeData = (versionId, expandedIds, selectedIds) => {
        var selectedId = null;
        if (Object.keys(selectedIds).length == 1) {
            selectedId = Object.keys(selectedIds)[0];
        }

        this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_NODES, versionId, expandedIds, selectedId));
    };

    handleNodeClick = (node, ensureItemVisible, e) => {
        const fund = this.getActiveFund(this.props);
        e.shiftKey && this.unFocus();
        this.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_NODES, fund.versionId, node.id, e.ctrlKey, e.shiftKey, null, ensureItemVisible));
    };

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges()
        }
    }    

    handleFulltextChange = (value) => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_NODES, fund.versionId, value));
    };

    handleFulltextSearch = () => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_NODES, fund.versionId));
    };

    handleFulltextPrevItem = () => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_NODES, fund.versionId));
    };

    handleFulltextNextItem = () => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_NODES, fund.versionId));
    };

    handleSubmit = () => {
        const {multipleSelection, onSubmitForm} = this.props;
        const fund = this.getActiveFund(this.props);
        const fundTreeNodes = fund.fundTreeNodes;

        if (multipleSelection) {
            const nodesMap = getMapFromList(fundTreeNodes.nodes);
            const nodes = Object.keys(fundTreeNodes.selectedIds).map(id => nodesMap[id]);

            onSubmitForm(Object.keys(fundTreeNodes.selectedIds), nodes);
        } else {
            const nodesMap = getMapFromList(fundTreeNodes.nodes);
            const node = nodesMap[fundTreeNodes.selectedId];

            onSubmitForm(fundTreeNodes.selectedId, node);
        }
    };

    getActiveFund = (props) => {
        var arrRegion = props.arrRegion;
        var activeFund = null;
        if (arrRegion.activeIndex != null) {
            activeFund = arrRegion.funds[arrRegion.activeIndex];
        }
        return activeFund
    };

    handleCollapse = () => {
        const fund = this.getActiveFund(this.props);
        this.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_NODES,fund.versionId, fund))
    };

    render() {
        const {multipleSelection, onClose} = this.props;
        const fund = this.getActiveFund(this.props);
        const fundTreeNodes = fund.fundTreeNodes;
        const versionId = fund.versionId;

        let someSelected;
        if (multipleSelection) {
            someSelected = Object.keys(fundTreeNodes.selectedIds).length > 0;
        } else {
            someSelected = fundTreeNodes.selectedId !== null;
        }

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
                    <Button disabled={!someSelected} onClick={this.handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state;
    return {
        arrRegion,
    }
}

export default connect(mapStateToProps)(FundNodesSelectForm);

