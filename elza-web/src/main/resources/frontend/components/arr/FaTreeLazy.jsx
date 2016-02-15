/**
 * Strom archivních souborů.
 */

require ('./FaTreeLazy.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {VirtualList, AbstractReactComponent, i18n, Loading, Icon, Search} from 'components';
import {Nav, Input, NavItem, Button, DropdownButton} from 'react-bootstrap';
var classNames = require('classnames');
import {ResizeStore} from 'stores';
import {propsEquals} from 'components/Utils'
import {indexById} from 'stores/app/utils.jsx'
import {createReferenceMark, getGlyph, getNodePrevSibling, getNodeNextSibling, getNodeParent, getNodeFirstChild} from 'components/arr/ArrUtils'
var Shortcuts = require('react-shortcuts/component')
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'

var keyDownHandlers = {
    ArrowUp: function(e) {
        const {nodes, selectedId, multipleSelection, onNodeClick} = this.props

        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null && index > 0) {
                onNodeClick(nodes[index - 1])
            }
        }
    },
    ArrowDown: function(e) {
        const {nodes, selectedId, multipleSelection, onNodeClick} = this.props

        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null && index < nodes.length) {
                onNodeClick(nodes[index + 1])
            }
        }
    },
    ArrowLeft: function(e) {
        const {nodes, selectedId, multipleSelection, expandedIds, onOpenCloseNode, onNodeClick} = this.props
        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null) {
                var node = nodes[index]
                if (node.hasChildren && expandedIds[node.id]) { // je rozbalen, zabalíme ho
                    onOpenCloseNode(node, false)
                } else {    // jdeme na parenta
                    var parent = getNodeParent(nodes, selectedId)
                    parent && onNodeClick(parent)
                }
            }
        }
    },
    ArrowRight: function(e) {
        const {nodes, selectedId, multipleSelection, expandedIds, onOpenCloseNode, onNodeClick} = this.props
        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null) {
                var node = nodes[index]
                if (node.hasChildren) { 
                    if (!expandedIds[node.id]) {    // je zabalen, rozbalíme ho
                        onOpenCloseNode(node, true)
                    } else {    // jdeme na prvního potomka
                        var firstChild = getNodeFirstChild(nodes, selectedId);
                        firstChild && onNodeClick(firstChild)
                    }
                } else {    // nemá potomky, nic neděláme
                }
            }
        }
    }
}

var FaTreeLazy = class FaTreeLazy extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods(
            'renderNode', 'handleKeyDown', 'trySetFocus'
        );

        this.state = {};
    }

    componentWillReceiveProps(nextProps) {
        this.trySetFocus(nextProps)
    }

    componentDidMount() {
        this.unsubscribe = ResizeStore.listen(status => {
            this.setState({});
        });
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
        this.trySetFocus(this.props)
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['focus', 'ensureItemVisible', 'filterText', 'expandedIds', 'selectedId', 'selectedIds', 'nodes', 'focusId', 'isFetching', 'fetched', 'searchedIds', 'searchedParents', 'filterCurrentIndex']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    componentWillUnmount() {
        this.unsubscribe();
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, 'arr', 1, 'tree') || isFocusFor(focus, 'arr', 1)) {
                this.setState({}, () => {
                   ReactDOM.findDOMNode(this.refs.treeContainer).focus()
                   focusWasSet()
                })
            }
        }
    }

    handleKeyDown(event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
        }
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

        var levels = createReferenceMark(node);

        var name = node.name ? node.name : i18n('faTree.node.name.undefined', node.id);

        var icon = <Icon className="node-icon" glyph={getGlyph(node.icon)} />

        var label = (
            <span
                title={name}
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

        var actionAddons = []
        if (searchedIds.length > 0 && filterCurrentIndex !== -1) {
            var searchedInfo = (
                <div className='fa-tree-lazy-search-info'>
                    ({filterCurrentIndex + 1} z {searchedIds.length})
                </div>
            )

            if (searchedIds.length > 1) {
                var prevButtonEnabled = filterCurrentIndex > 0;
                var nextButtonEnabled = filterCurrentIndex < searchedIds.length - 1;

                actionAddons.push(<Button disabled={!nextButtonEnabled} className="next" onClick={this.props.onFulltextNextItem}><Icon glyph='fa-chevron-down'/></Button>)
                actionAddons.push(<Button disabled={!prevButtonEnabled} className="prev" onClick={this.props.onFulltextPrevItem}><Icon glyph='fa-chevron-up'/></Button>)
            }
            actionAddons.push(searchedInfo)
        }

        var index;
        if (this.props.ensureItemVisible) {
            index = indexById(this.props.nodes, this.props.selectedId);
        }

        return (

            <div className='fa-tree-lazy-main-container'>
                <div className='fa-traa-header-container'>
                    <Search
                        placeholder={i18n('search.input.search')}
                        filterText={this.props.filterText}
                        onChange={e => this.props.onFulltextChange(e.target.value)}
                        onClear={e => {this.props.onFulltextChange(''); this.props.onFulltextSearch()}}
                        onSearch={this.props.onFulltextSearch}
                        actionAddons={actionAddons}
                    />
                </div>
                <div className='fa-tree-lazy-container' ref="treeContainer" onKeyDown={this.handleKeyDown} tabIndex={-11}>
                    <Button className="tree-collapse" onClick={this.props.onCollapse}><Icon glyph='fa-compress'/>Sbalit vše</Button>
                    {this.state.treeContainer && <VirtualList
                        tagName='div'
                        scrollToIndex={index}
                        container={this.state.treeContainer}
                        items={this.props.nodes}
                        renderItem={this.renderNode}
                        itemHeight={this.props.rowHeight}
                    />}
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
    rowHeight: 16
}

FaTreeLazy.propTypes = {
    expandedIds: React.PropTypes.object.isRequired,
    selectedId: React.PropTypes.number,
    selectedIds: React.PropTypes.object,
    filterText: React.PropTypes.string,
    searchedIds: React.PropTypes.array,
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
    focus: React.PropTypes.object,
}

module.exports = connect()(FaTreeLazy);
