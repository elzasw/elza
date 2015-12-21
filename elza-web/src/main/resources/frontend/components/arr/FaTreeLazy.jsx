/**
 * Strom archivních souborů.
 */

require ('./FaTreeLazy.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {VirtualList, AbstractReactComponent, i18n, Loading} from 'components';
import {Nav, NavItem} from 'react-bootstrap';
var classNames = require('classnames');
import {faTreeFetchIfNeeded, faTreeNodeExpand, faTreeNodeCollapse} from 'actions/arr/faTree'
import {faSelectNodeTab, faSelectSubNode} from 'actions/arr/nodes'
import {ResizeStore} from 'stores';

var FaTreeLazy = class FaTreeLazy extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderNode', 'handleToggle', 'handleNodeClick', 'handleNodeDoubleClick');

        this.dispatch(faTreeFetchIfNeeded(props.faId, props.versionId, props.expandedIds));

        ResizeStore.listen(status => {
            this.setState({});
        });

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(faTreeFetchIfNeeded(nextProps.faId, nextProps.versionId, nextProps.expandedIds));
    }

    componentDidMount() {
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
    }

    handleToggle(node, expand) {
        expand ? this.dispatch(faTreeNodeExpand(node)) : this.dispatch(faTreeNodeCollapse(node));
    }

    handleNodeDoubleClick(node) {
        var parentNode = this.props.nodeMap[node.parentId];
        if (parentNode != null) {
            //this.dispatch(faSelectNodeTab(parentNode));
            this.dispatch(faSelectSubNode(node.id, parentNode, true));
        }
    }

    handleNodeClick(node) {
        var parentNode = this.props.nodeMap[node.parentId];
        if (parentNode != null) {
            //this.dispatch(faSelectNodeTab(parentNode));
            this.dispatch(faSelectSubNode(node.id, parentNode, false));
        }
    }

    renderNode(node) {
        var expanded = node.hasChildren && this.props.expandedIds['n_' + node.id];

        var expCol;
        if (node.hasChildren) {
            expCol = <span className='exp-col' onClick={this.handleToggle.bind(this, node, !expanded)}>{expanded ? '-' : '+'}</span>
        } else {
            expCol = <span className='exp-col'>&nbsp;</span>
        }

        var cls = classNames({
            node: true,
            ['level' + node.depth]: true,
            opened: expanded,
            closed: !expanded,
            active: this.props.selectedId === node.id
        })
        return (
            <div key={node.id} className={cls}>
                {expCol}
                <span onClick={this.handleNodeClick.bind(this, node)} onDoubleClick={this.handleNodeDoubleClick.bind(this, node)}>{node.name}</span>
            </div>
        )
    }

    render() {
        return (
            <div className='fa-tree-lazy-container' ref="treeContainer">
                <VirtualList tagName='div' container={this.state.treeContainer} items={this.props.nodes} renderItem={this.renderNode} itemHeight={this.props.rowHeight} />
            </div>
        )

        var rows;
        if (this.props.fetched) {
            rows = this.props.nodes.map(node => {
                return this.renderNode(node);
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

FaTreeLazy.defaultProps = {
    rowHeight: 22
}

module.exports = connect()(FaTreeLazy);
