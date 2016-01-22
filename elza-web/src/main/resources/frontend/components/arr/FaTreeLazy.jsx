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
            'renderNode', 'handleOpenCloseNode', 'handleNodeClick',
            'handleContextMenu', 'getParentNode', 'handleSelectInNewTab',
            'callFaSelectSubNode'
        );

        ResizeStore.listen(status => {
            this.setState({});
        });

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(faTreeFetchIfNeeded(nextProps.versionId, nextProps.expandedIds, nextProps.selectedId));
    }

    componentDidMount() {
        this.dispatch(faTreeFetchIfNeeded(this.props.versionId, this.props.expandedIds, this.props.selectedId));
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
    }

    /**
     * Kliknutí na rozbalovací uzel.
     * @param node {Object} jaký uzel chceme rozbalit/zabalit
     * @param expand {Boolean} true, pokud uzel chceme rozbalit
     */
    handleOpenCloseNode(node, expand) {
        expand ? this.dispatch(faTreeNodeExpand(node)) : this.dispatch(faTreeNodeCollapse(node));
    }

    /**
     * Načtení nadřazeného uzlu k předanému.
     * @param node {Object} uzel, pro který chceme vrátit nadřazený
     * @return {Object} parent nebo null, pokud je předaný uzel kořenový
     */
    getParentNode(node) {
        var index = indexById(this.props.nodes, node.id);
        while (--index >= 0) {
            if (this.props.nodes[index].depth < node.depth) {
                return this.props.nodes[index];
            }
        }
        return null;
    }

    /**
     * Zobrazení kontextového menu pro daný uzel.
     * @param node {Object} uzel
     * @param e {Object} event
     */
    handleContextMenu(node, e) {
        e.preventDefault();
        e.stopPropagation();

        var menu = (
            <ul className="dropdown-menu">
                <MenuItem onClick={this.handleSelectInNewTab.bind(this, node)}>{i18n('faTree.action.openInNewTab')}</MenuItem>
            </ul>
        )

        this.dispatch(faTreeFocusNode(node));
        this.dispatch(contextMenuShow(this, menu, {x: e.clientX, y:e.clientY}));
    }

    /**
     * Otevření uzlu v nové záložce.
     * @param node {Object} uzel
     */
    handleSelectInNewTab(node) {
        this.dispatch(contextMenuHide());

        this.callFaSelectSubNode(node, true);
    }

    /**
     * Otevření uzlu v záložce.
     * @param node {Object} uzel
     * @param openNewTab {Boolean} true, pokud se má otevřít v nové záložce
     */
    callFaSelectSubNode(node, openNewTab) {
        var parentNode = this.getParentNode(node);
        if (parentNode != null) {
            this.dispatch(faSelectSubNode(node.id, parentNode, openNewTab));
        }
    }

    /**
     * Otevření uzlu v aktuální záložce (pokud aktuální není, otevře se v nové).
     * @param node {Object} uzel
     */
    handleNodeClick(node) {
        this.callFaSelectSubNode(node, false);
    }

    /**
     * Renderování uzlu.
     * @param node {Object} uzel
     * @return {Object} view
     */
    renderNode(node) {
        var expanded = node.hasChildren && this.props.expandedIds[node.id];

        var expCol;
        if (node.hasChildren) {
            var expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');
            expCol = <span className={expColCls} onClick={this.handleOpenCloseNode.bind(this, node, !expanded)}></span>
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
