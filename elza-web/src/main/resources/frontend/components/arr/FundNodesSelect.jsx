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
    };

    state = {
        nodes: {}
    };

    componentDidMount() {
        const {multipleSelection, multipleSelectionOneLevel, selectedId, fund} = this.props;
        const fundTreeNodes = fund.fundTreeNodes;
        const versionId = fund.versionId;

        this.props.dispatch(fundTreeConfigure(types.FUND_TREE_AREA_NODES, versionId, multipleSelection, multipleSelectionOneLevel));

        if (!multipleSelection && typeof selectedId !== "undefined") {
            this.props.dispatch(fundTreeSelectNode(types.FUND_TREE_AREA_NODES, versionId, selectedId, false, false));
        }

        this.requestFundTreeData(versionId, multipleSelection, fundTreeNodes.expandedIds);

        // Zavolání onChange metody
        this.handleChange(this.props);
    }

    componentWillReceiveProps(nextProps){
        const {fund, multipleSelection} = nextProps;
        const fundTreeNodes = fund.fundTreeNodes;
        const versionId = fund.versionId;
        this.requestFundTreeData(versionId, multipleSelection, fundTreeNodes.expandedIds).then((fundTree) => {
            const nodesMap = getMapFromList(fundTree.nodes);
            const node = nodesMap[fundTreeNodes.selectedId];
            this.props.onChange([fundTree.selectedId],[node])
        });

        // Zavolání onChange metody
        let selectionChanged = false;
        const prevFund = this.props.fund;
        const prevFundTreeNodes = prevFund.fundTreeNodes;
        if (multipleSelection) {
            selectionChanged = fundTreeNodes.selectedIds !== prevFundTreeNodes.selectedIds;
        } else {
            selectionChanged = fundTreeNodes.selectedId !== prevFundTreeNodes.selectedId;
        }
        selectionChanged && this.handleChange(nextProps);
    };

    handleChange = (props) => {
        const {multipleSelection, onChange, fund} = props;

        if (!onChange) {
            return;
        }

        const fundTreeNodes = fund.fundTreeNodes;

        if (multipleSelection) {
            const nodesMap = getMapFromList(fundTreeNodes.nodes);
            const nodes = Object.keys(fundTreeNodes.selectedIds).map(id => nodesMap[id]);

            onChange(Object.keys(fundTreeNodes.selectedIds), nodes);
        } else {
            const nodesMap = getMapFromList(fundTreeNodes.nodes);
            const node = nodesMap[fundTreeNodes.selectedId];

            if (fundTreeNodes.selectedId != null) {
                onChange([fundTreeNodes.selectedId], [node]);
            } else {
                onChange([], []);
            }
        }
    }

    requestFundTreeData = (versionId, multipleSelection, expandedIds) => {
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

    render() {
        const {fund, multipleSelection, onClose} = this.props;
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

