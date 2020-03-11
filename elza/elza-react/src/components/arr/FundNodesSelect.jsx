import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { AbstractReactComponent } from 'components/shared';
import * as types from 'actions/constants/ActionTypes.js';
import {
    fundTreeCollapse,
    fundTreeConfigure,
    fundTreeFetchIfNeeded,
    fundTreeFulltextChange,
    fundTreeFulltextNextItem,
    fundTreeFulltextPrevItem,
    fundTreeFulltextSearch,
    fundTreeNodeCollapse,
    fundTreeNodeExpand,
    fundTreeSelectNode,
} from 'actions/arr/fundTree.jsx';
import { getMapFromList } from 'stores/app/utils.jsx';
import './FundNodesSelect.scss';
import FundTreeLazy from './FundTreeLazy';

/**
 * Formulář vybrání uzlů v konkrétní verzi souboru - jen vlastní obsah formuláře - výběr uzlů na základě konfigurace - např. single nebo multiple select.
 * Implicitně pokud se neuvede, je výběr multiselect libovolných položek ve stromu.
 */
class FundNodesSelect extends AbstractReactComponent {

    static propTypes = {
        multipleSelection: PropTypes.bool,
        multipleSelectionOneLevel: PropTypes.bool,
        selectedId: PropTypes.number, // pokud je předáno (není undefined) a jedná se o ne multiselect tree, bude daná položka zobrazena jako vybraná (v případě null se vše odznačí) - strom se nainicializuje s touto položkou
        onChange: PropTypes.func, // funkce, která předává ([ids], [nodes])
    };

    static defaultProps = {
        multipleSelection: true,
        multipleSelectionOneLevel: false,
        selectedId: null,
        fundId: null,
        area: types.FUND_TREE_AREA_NODES,
    };

    state = {
        nodes: {},
    };

    componentDidMount() {
        const { multipleSelection, multipleSelectionOneLevel, selectedId, fund: { fundTreeNodes, versionId }, onChange, area } = this.props;

        this.props.dispatch(fundTreeConfigure(area, versionId, multipleSelection, multipleSelectionOneLevel));
        this.props.dispatch(fundTreeSelectNode(area, versionId, selectedId, false, false));

        this.handleChange(versionId, fundTreeNodes.expandedIds, multipleSelection, onChange);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const { fund: { fundTreeNodes, versionId }, multipleSelection, onChange } = nextProps;

        const selectionChanged = this.checkSelectionChanged(this.props.fund.fundTreeNodes, fundTreeNodes, multipleSelection);
        selectionChanged && this.handleChange(versionId, fundTreeNodes.expandedIds, multipleSelection, onChange);
    };

    checkSelectionChanged(prevFundTreeNodes, fundTreeNodes, multipleSelection) {
        if (multipleSelection) {
            // convert simple objects to string for easier comparison
            const selectedIds = JSON.stringify(fundTreeNodes.selectedIds);
            const prevSelectedIds = JSON.stringify(prevFundTreeNodes.selectedIds);
            return selectedIds !== prevSelectedIds;
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
    };

    requestFundTreeData = (versionId, expandedIds) => {
        const { area } = this.props;
        return this.props.dispatch(fundTreeFetchIfNeeded(area, versionId, expandedIds));
    };

    handleNodeClick = (node, ensureItemVisible, e) => {
        const { fund, area } = this.props;
        e.shiftKey && this.unFocus();
        this.props.dispatch(fundTreeSelectNode(area, fund.versionId, node.id, e.ctrlKey, e.shiftKey, null, ensureItemVisible));
    };

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges();
        }
    }

    handleFulltextChange = (value) => {
        const { fund, area } = this.props;
        this.props.dispatch(fundTreeFulltextChange(area, fund.versionId, value));
    };

    handleFulltextSearch = () => {
        const { fund, area } = this.props;
        this.props.dispatch(fundTreeFulltextSearch(area, fund.versionId));
    };

    handleFulltextPrevItem = () => {
        const { fund, area } = this.props;
        this.props.dispatch(fundTreeFulltextPrevItem(area, fund.versionId));
    };

    handleFulltextNextItem = () => {
        const { fund, area } = this.props;
        this.props.dispatch(fundTreeFulltextNextItem(area, fund.versionId));
    };

    handleCollapse = () => {
        const { fund, area } = this.props;
        this.props.dispatch(fundTreeCollapse(area, fund.versionId, fund));
    };

    handleNodeExpandCollapse = (node, expand) => {
        const { fund: { versionId }, area } = this.props;
        if (expand) {
            this.props.dispatch(fundTreeNodeExpand(area, node));
        } else {
            this.props.dispatch(fundTreeNodeCollapse(area, versionId, node));
        }
    };

    render() {
        const { fund: { fundTreeNodes } } = this.props;
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
        );
    }
}

function mapStateToProps(state, props) {
    const { arrRegion } = state;
    return {
        fund: props.fund ? props.fund : arrRegion.funds[arrRegion.activeIndex],
    };
}

export default connect(mapStateToProps)(FundNodesSelect);
