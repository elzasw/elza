/**
 * Strom archivních souborů.
 */

require ('./FaTreeLazy.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {VirtualList, AbstractReactComponent, i18n, Loading} from 'components';
import {Nav, NavItem, DropdownButton, MenuItem} from 'react-bootstrap';
var classNames = require('classnames');
import {faTreeFocusNode, faTreeFetchIfNeeded, faTreeNodeExpand, faTreeNodeCollapse} from 'actions/arr/faTree'
import {contextMenuShow, contextMenuHide} from 'actions/global/contextMenu'
import {faSelectSubNode} from 'actions/arr/nodes'
import {ResizeStore} from 'stores';
import {indexById} from 'stores/app/utils.jsx'

var FaTreeLazy = class FaTreeLazy extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'renderNode', 'handleToggle', 'handleNodeClick',
            'handleContextMenu', 'getParentNode', 'handleSelectInNewTab',
            'callFaSelectSubNode'
        );

        this.dispatch(faTreeFetchIfNeeded(props.versionId, props.expandedIds, props.selectedId));

        ResizeStore.listen(status => {
            this.setState({});
        });

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(faTreeFetchIfNeeded(nextProps.versionId, nextProps.expandedIds, nextProps.selectedId));
    }

    componentDidMount() {
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
    }

    handleToggle(node, expand) {
        expand ? this.dispatch(faTreeNodeExpand(node)) : this.dispatch(faTreeNodeCollapse(node));
    }

    getParentNode(node) {
        var index = indexById(this.props.nodes, node.id);
        while (--index >= 0) {
            if (this.props.nodes[index].depth < node.depth) {
                return this.props.nodes[index];
            }
        }
        return null;
    }

    handleContextMenu(node, e) {
        e.preventDefault();
        e.stopPropagation();

        console.log(e);
        console.log(e.clientX);
        console.log(e.clientY);

        var menu = (
            <ul className="dropdown-menu">
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('faTree.action.openInNewTab')}</MenuItem>
            </ul>
        )

        this.dispatch(faTreeFocusNode(node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    handleSelectInNewTab(node) {
        this.dispatch(contextMenuHide());

        this.callFaSelectSubNode(node, true);
    }

    callFaSelectSubNode(node, openNewTab) {
        var parentNode = this.getParentNode(node);
        if (parentNode != null) {
            this.dispatch(faSelectSubNode(node.id, parentNode, openNewTab));
        }
    }

    handleNodeClick(node) {
        this.callFaSelectSubNode(node, false);
    }

    renderNode(node) {
        var expanded = node.hasChildren && this.props.expandedIds[node.id];

        var expCol;
        if (node.hasChildren) {
            var expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');
            expCol = <span className={expColCls} onClick={this.handleToggle.bind(this, node, !expanded)}></span>
        } else {
            expCol = <span className='exp-col'>&nbsp;</span>
        }

        var cls = classNames({
            node: true,
            opened: expanded,
            closed: !expanded,
            active: this.props.selectedId === node.id,
            focus: this.props.focusId === node.id,
        })

        var levels = [];
        if (node.referenceMark) {
            node.referenceMark.forEach((i, index) => {
                if (i < 1000) {
                    levels.push(<span className="level">{i}</span>)
                } else {
                    levels.push(<span className="level">.{i % 1000}</span>)
                }
                if (index + 1 < node.referenceMark.length) {
                    levels.push(<span className="separator"></span>)
                }
            });
        }

        var name = node.name ? node.name : <i>{i18n('faTree.node.name.undefined', node.id)}</i>;

        var icon = <span className="node-icon fa fa-briefcase"></span>

        var label = (
            <span
                className='node-label'
                onClick={this.handleNodeClick.bind(this, node)}
                onContextMenu={this.handleContextMenu.bind(this, node)}
                >
                {name}
            </span>
        )

        return (
            <div key={node.id} className={cls}>
                {levels}
                {expCol}
                {icon}
                {label}
            </div>
        )
    }

    render() {
        return (
            <div className='fa-tree-lazy-container' ref="treeContainer">
                {true && <VirtualList tagName='div' container={this.state.treeContainer} items={this.props.nodes} renderItem={this.renderNode} itemHeight={this.props.rowHeight} />}
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
