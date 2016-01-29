/**
 * Strom archivních souborů.
 */

require ('./FaTreeLazy.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {VirtualList, AbstractReactComponent, i18n, Loading} from 'components';
import {Nav, Input, NavItem, Button, DropdownButton} from 'react-bootstrap';
var classNames = require('classnames');
import {ResizeStore} from 'stores';
import {propsEquals} from 'components/Utils'

var FaTreeLazy = class FaTreeLazy extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'renderNode'
        );

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.unsubscribe = ResizeStore.listen(status => {
            this.setState({});
        });
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
    }

    shouldComponentUpdate(nextProps, nextState) {
        var eqProps = ['filterText', 'expandedIds', 'selectedId', 'selectedIds', 'nodes', 'focusId', 'isFetching', 'fetched', 'searchedIds', 'searchedParents', 'filterCurrentIndex']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    componentWillUnmount() {
        this.unsubscribe();
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
                    levels.push(<span key={'level' + index} className="level">{i}</span>)
                } else {
                    levels.push(<span key={'level' + index} className="level">.{i % 1000}</span>)
                }
                if (index + 1 < node.referenceMark.length) {
                    levels.push(<span  key={'sep' + index} className="separator"></span>)
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
        const {searchedIds, searchedParents, filterCurrentIndex} = this.props;

        var searchedInfo;
        if (searchedIds.length > 0 && filterCurrentIndex !== -1) {
            searchedInfo = (
                <div className='fa-tree-lazy-search-info'>
                    ({filterCurrentIndex + 1}-{searchedIds.length})
                </div>
            )
        }

        return (
            <div className='fa-tree-lazy-main-container'>
                <div className='fa-traa-header-container'>
                    <Input type='search' value={this.props.filterText} onChange={e => this.props.onFulltextChange(e.target.value)} />
                    {searchedInfo}
                    <Button onClick={this.props.onFulltextSearch}>Hledat</Button>
                    <Button onClick={this.props.onFulltextPrevItem}>Předchozí</Button>
                    <Button onClick={this.props.onFulltextNextItem}>Další</Button>
                    <Button onClick={this.props.onCollapse}>Zabalit</Button>
                </div>
                <div className='fa-tree-lazy-container' ref="treeContainer">
                    {true && <VirtualList tagName='div' container={this.state.treeContainer} items={this.props.nodes} renderItem={this.renderNode} itemHeight={this.props.rowHeight} />}
                </div>
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
    expandedIds: React.PropTypes.object.isRequired,
    selectedId: React.PropTypes.number,
    selectedIds: React.PropTypes.object,
    filterText: React.PropTypes.string,
    searchedIds: React.PropTypes.object,
    searchedParents: React.PropTypes.object,
    filterCurrentIndex: React.PropTypes.number,
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
