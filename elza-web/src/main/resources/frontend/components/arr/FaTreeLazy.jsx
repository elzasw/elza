/**
 * Strom archivních souborů.
 */

require ('./FaTreeLazy.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {VirtualList, AbstractReactComponent, i18n, Loading} from 'components';
import {Nav, NavItem, DropdownButton} from 'react-bootstrap';
var classNames = require('classnames');
import {ResizeStore} from 'stores';

var FaTreeLazy = class FaTreeLazy extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'renderNode',
        );

        ResizeStore.listen(status => {
            this.setState({});
        });

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
    }

    /**
     * Renderování uzlu.
     * @param node {Object} uzel
     * @return {Object} view
     */
    renderNode(node) {
        var {onNodeClick, onOpenCloseNode, onContextMenu} = this.props;

        var expanded = node.hasChildren && this.props.expandedIds[node.id];

        var expCol;
        if (node.hasChildren) {
            var expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');
            expCol = <span className={expColCls} onClick={onOpenCloseNode.bind(this, node, !expanded)}></span>
        } else {
            expCol = <span className='exp-col'>&nbsp;</span>
        }

        var active = false;
        active |= this.props.selectedId === node.id;
        if (this.props.selectedIds && this.props.selectedIds[node.id]) {
            active = true
        }
        var cls = classNames({
            node: true,
            opened: expanded,
            closed: !expanded,
            active: active,
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
                onClick={onNodeClick.bind(this, node)}
                onContextMenu={onContextMenu.bind(this, node)}
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

FaTreeLazy.propTypes = {
    fa: React.PropTypes.object.isRequired,
    versionId: React.PropTypes.number.isRequired,
    expandedIds: React.PropTypes.object.isRequired,
    selectedId: React.PropTypes.number,
    selectedIds: React.PropTypes.object,
    nodes: React.PropTypes.array.isRequired,
    focusId: React.PropTypes.number,
    rowHeight: React.PropTypes.number.isRequired,
    isFetching: React.PropTypes.bool.isRequired,
    fetched: React.PropTypes.bool.isRequired,
    onNodeClick: React.PropTypes.func,
    onOpenCloseNode: React.PropTypes.func,
    onContextMenu: React.PropTypes.func,
}

module.exports = connect()(FaTreeLazy);
