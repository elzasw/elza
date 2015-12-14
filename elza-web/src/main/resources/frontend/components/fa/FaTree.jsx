/**
 * Strom archivních souborů.
 */

require ('./FaTree.less');

import React from 'react';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {Nav, NavItem} from 'react-bootstrap';
var classNames = require('classnames');
import {fetchFaTreeIfNeeded} from 'actions/fa/faTreeData'
import {expandFaTreeNode, collapseFaTreeNode} from 'actions/fa/faTree'
import {selectNode} from 'actions/fa/nodes'

var FaTree = class FaTree extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderNode', 'handleToggle', 'handleNodeClick');

        this.dispatch(fetchFaTreeIfNeeded(props.faId, props.versionId));
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fetchFaTreeIfNeeded(nextProps.faId, nextProps.versionId));
    }

    handleToggle(node, expand) {
        expand ? this.dispatch(expandFaTreeNode(node)) : this.dispatch(collapseFaTreeNode(node));
    }

    handleNodeClick(node) {
        var parentNode = this.props.nodeMap[node.parentId];
        if (parentNode != null) {
            this.dispatch(selectNode(parentNode));
        }
    }

    renderNode(node, level) {
        var expanded = node.children && this.props.expandedIds[node.id];

        var children;
        if (expanded) {
            children = node.children.map(child => {return this.renderNode(child, level + 1)});
        }

        var expCol;
        if (node.children && node.children.length > 0) {
            expCol = <span className='exp-col' onClick={this.handleToggle.bind(this, node, !expanded)}>{expanded ? '-' : '+'}</span>
        } else {
            expCol = <span className='exp-col'>&nbsp;</span>
        }

        var cls = classNames({
            ['level' + level]: true,
            opened: expanded,
            closed: !expanded,
            active: this.props.selectedId === node.id
        })

        return (
            <div className={cls}>
                {expCol}
                <span onClick={this.handleNodeClick.bind(this, node)}>{node.name}</span>
                {children}
            </div>
        )
    }

    render() {
        var rows;
        if (this.props.fetched) {
            rows = this.props.nodes.map(node => {
                return this.renderNode(node, 0);
            });
        }

        return (
            <div className='fa-tree'>
                {(this.props.isFetching || !this.props.fetched) && <Loading/>}
                {(!this.props.isFetching && this.props.fetched) && rows}
            </div>
        )
    }
}

module.exports = connect()(FaTree);
