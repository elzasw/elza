import React from 'react';
import {connect} from 'react-redux'
import {i18n, AbstractReactComponent} from 'components/shared';
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
    fundTreeConfigure,
} from 'actions/arr/fundTree.jsx'
import {getMapFromList} from 'stores/app/utils.jsx'
import './FundNodesSelect.less';
import FundTreeLazy from "./FundTreeLazy";

/**
 * Formulář vybrání uzlů v konkrétní verzi souboru - jen vlastní obsah formuláře - výběr uzlů na základě konfigurace - např. single nebo multiple select.
 * Implicitně pokud se neuvede, je výběr multiselect libovolných položek ve stromu.
 */
class FundNodesSelect extends AbstractReactComponent {

    static propTypes = {
        multipleSelection: React.PropTypes.bool,
        multipleSelectionOneLevel: React.PropTypes.bool,
        selectedId: React.PropTypes.number, // pokud je předáno (není undefined) a jedná se o ne multiselect tree, bude daná položka zobrazena jako vybraná (v případě null se vše odznačí) - strom se nainicializuje s touto položkou
        onChange: React.PropTypes.func, // funkce, která předává ([ids], [nodes])
    };

    static defaultProps = {
        multipleSelection: true,
        multipleSelectionOneLevel: false,
        selectedId: null
    };

    state = {
        nodes: {},
    };

    componentDidMount() {
        const {multipleSelection, multipleSelectionOneLevel, selectedId, fund:{fundTreeNodes, versionId}, onChange} = this.props;

        this.props.dispatch(fundTreeConfigure(types.FUND_TREE_AREA_NODES, versionId, multipleSelection, multipleSelectionOneLevel));
        this.props.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_NODES, versionId, selectedId, false, false));

        this.handleChange(versionId, fundTreeNodes.expandedIds, multipleSelection, onChange);
    }

    componentWillReceiveProps(nextProps){
        const {fund:{fundTreeNodes, versionId}, multipleSelection, onChange} = nextProps;

        const selectionChanged = this.checkSelectionChanged(this.props.fund.fundTreeNodes, fundTreeNodes, multipleSelection);
        selectionChanged && this.handleChange(versionId, fundTreeNodes.expandedIds, multipleSelection, onChange);
    };

    checkSelectionChanged(prevFundTreeNodes, fundTreeNodes, multipleSelection){
        if(multipleSelection){
            // convert simple objects to string for easier comparison
            const selectedIds = JSON.stringify(fundTreeNodes.selectedIds);
            const prevSelectedIds = JSON.stringify(prevFundTreeNodes.selectedIds);
            return selectedIds != prevSelectedIds;
        } else {
            return fundTreeNodes.selectedId !== prevFundTreeNodes.selectedId;
        }
    }

    handleChange = (versionId, expandedIds, multipleSelection, onChange) => {
        this.requestFundTreeData(versionId, expandedIds).then((fundTree) => {
            if (!onChange) {
                return;
            }
            // Zavolání onChange metody
            if (multipleSelection) {
                const nodesMap = getMapFromList(fundTree.nodes);
                const selectedIds = fundTree.selectedIds ? Object.keys(fundTree.selectedIds) : [];
                const nodes = selectedIds.map(id => nodesMap[id]);

                onChange(selectedIds, nodes);
            } else {
                const nodesMap = getMapFromList(fundTree.nodes);
                const node = nodesMap[fundTree.selectedId];

                if (fundTree.selectedId != null) {
                    onChange([fundTree.selectedId], [node]);
                } else {
                    onChange([], []);
                }
            }
        });
    }

    requestFundTreeData = (versionId, expandedIds) => {
        return this.dispatch(fundTreeFetchIfNeeded(types.FUND_TREE_AREA_NODES, versionId, expandedIds));
    };

    handleNodeClick = (node, ensureItemVisible, e) => {
        const {fund} = this.props;
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
        const {fund} = this.props;
        this.dispatch(fundTreeFulltextChange(types.FUND_TREE_AREA_NODES, fund.versionId, value));
    };

    handleFulltextSearch = () => {
        const {fund} = this.props;
        this.dispatch(fundTreeFulltextSearch(types.FUND_TREE_AREA_NODES, fund.versionId));
    };

    handleFulltextPrevItem = () => {
        const {fund} = this.props;
        this.dispatch(fundTreeFulltextPrevItem(types.FUND_TREE_AREA_NODES, fund.versionId));
    };

    handleFulltextNextItem = () => {
        const {fund} = this.props;
        this.dispatch(fundTreeFulltextNextItem(types.FUND_TREE_AREA_NODES, fund.versionId));
    };

    handleCollapse = () => {
        const {fund} = this.props;
        this.dispatch(fundTreeCollapse(types.FUND_TREE_AREA_NODES,fund.versionId, fund))
    };

    handleNodeExpandCollapse = (node, expand) => {
        const {fund:{versionId}} = this.props;
        if(expand){
            this.dispatch(fundTreeNodeExpand(types.FUND_TREE_AREA_NODES, node))
        } else {
            this.dispatch(fundTreeNodeCollapse(types.FUND_TREE_AREA_NODES, versionId, node))
        }
    }

    render() {
        const {fund:{fundTreeNodes}} = this.props;
        return (
            <div className="add-nodes-form-container">
                <FundTreeLazy
                    ref='tree'
                    {...fundTreeNodes}
                    cutLongLabels={true}
                    onOpenCloseNode={this.handleNodeExpandCollapse}
                    onNodeClick={this.handleNodeClick}
                    onFulltextChange={this.handleFulltextChange}
                    onFulltextSearch={this.handleFulltextSearch}
                    onFulltextPrevItem={this.handleFulltextPrevItem}
                    onFulltextNextItem={this.handleFulltextNextItem}
                    onCollapse={this.handleCollapse}
                />
            </div>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion} = state;
    return {
        fund: arrRegion.funds[arrRegion.activeIndex]
    }
}

export default connect(mapStateToProps)(FundNodesSelect);
