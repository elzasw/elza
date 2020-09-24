/**
 * Strom AS.
 */

import React from 'react';
import {connect} from 'react-redux';
import {AbstractReactComponent} from 'components/shared';
import {
    fundTreeCollapse,
    fundTreeFetchIfNeeded,
    fundTreeFocusNode,
    fundTreeFulltextChange,
    fundTreeFulltextNextItem,
    fundTreeFulltextPrevItem,
    fundTreeFulltextSearch,
    fundTreeNodeCollapse,
    fundTreeNodeExpand,
    fundTreeSelectNode,
} from 'actions/arr/fundTree.jsx';

import {contextMenuShow} from 'actions/global/contextMenu.jsx';
import FundTreeLazy from './FundTreeLazy';
import './FundTreeDaos.scss';

class FundTreeDaos extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'handleNodeClick',
            'handleContextMenu',
            'handleCollapse',
            'handleFulltextChange',
            'handleFulltextSearch',
            'handleFulltextPrevItem',
            'handleFulltextNextItem',
        );
    }

    componentDidMount() {
        const {versionId, expandedIds} = this.props;
        this.requestFundTreeData(versionId, expandedIds);
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        const {versionId, expandedIds} = nextProps;
        this.requestFundTreeData(versionId, expandedIds);
    }

    requestFundTreeData(versionId, expandedIds) {
        this.props.dispatch(fundTreeFetchIfNeeded(this.props.area, versionId, expandedIds));
    }

    handleFulltextChange(value) {
        this.props.dispatch(fundTreeFulltextChange(this.props.area, this.props.versionId, value));
    }

    handleFulltextSearch() {
        this.props.dispatch(fundTreeFulltextSearch(this.props.area, this.props.versionId));
    }

    handleFulltextPrevItem() {
        this.props.dispatch(fundTreeFulltextPrevItem(this.props.area, this.props.versionId));
    }

    handleFulltextNextItem() {
        this.props.dispatch(fundTreeFulltextNextItem(this.props.area, this.props.versionId));
    }

    /**
     * Zobrazení kontextového menu pro daný uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleContextMenu(node, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = <ul className="dropdown-menu"></ul>;

        this.props.dispatch(fundTreeFocusNode(this.props.area, this.props.versionId, node));
        this.props.dispatch(contextMenuShow(this, menu, {x: e.clientX, y: e.clientY}));
    }

    /**
     * Klik na uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleNodeClick(node, ensureItemVisible, e) {
        this.props.dispatch(
            fundTreeSelectNode(this.props.area, this.props.versionId, node.id, false, false, null, ensureItemVisible),
        );
    }

    /**
     * Zabalení stromu
     */
    handleCollapse() {
        this.props.dispatch(fundTreeCollapse(this.props.area, this.props.versionId, this.props.fund));
    }

    render() {
        return (
            <FundTreeLazy
                {...this.props}
                className="fund-tree-daos"
                cutLongLabels={true}
                onOpenCloseNode={(node, expand) => {
                    expand
                        ? this.props.dispatch(fundTreeNodeExpand(this.props.area, node))
                        : this.props.dispatch(fundTreeNodeCollapse(this.props.area, this.props.versionId, node));
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

export default connect()(FundTreeDaos);
