import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {VirtualList, NoFocusButton, AbstractReactComponent, i18n, Loading, Icon, SearchWithGoto} from 'components/index.jsx';
import {Nav, Input, NavItem, Button, DropdownButton} from 'react-bootstrap';
const classNames = require('classnames');
import {propsEquals} from 'components/Utils.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {createReferenceMark, getGlyph, getNodePrevSibling, getNodeNextSibling, getNodeParent, getNodeFirstChild} from 'components/arr/ArrUtils.jsx'
import './FundTreeLazy.less';

// Na kolik znaků se má název položky stromu oříznout, jen pokud je nastaven vstupní atribut, že se má název ořezávat
const TREE_NAME_MAX_CHARS = 60

// Odsazení odshora, musí být definováno, jinak nefunguje ensureItemVisible
const TREE_TOP_PADDING = 0

const keyDownHandlers = {
    ArrowUp: function(e) {
        const {nodes, selectedId, multipleSelection} = this.props
        e.stopPropagation()
        e.preventDefault()

        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null && index > 0) {
                this.handleNodeClick(nodes[index - 1], true)
            }
        }
    },
    ArrowDown: function(e) {
        const {nodes, selectedId, multipleSelection} = this.props
        e.stopPropagation()
        e.preventDefault()

        if (!multipleSelection) {
            if (selectedId !== null) {  // něco je označeno
                var index = indexById(nodes, selectedId)
                if (index !== null && index + 1 < nodes.length) {
                    this.handleNodeClick(nodes[index + 1], true)
                }
            } else {    // není nic označeno, označíme první položku stromu
                if (nodes.length > 0) {
                    this.handleNodeClick(nodes[0], true)
                }
            }
        }
    },
    ArrowLeft: function(e) {
        const {nodes, selectedId, multipleSelection, expandedIds, onOpenCloseNode} = this.props
        e.stopPropagation()
        e.preventDefault()

        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null) {
                var node = nodes[index]
                if (node.hasChildren && expandedIds[node.id]) { // je rozbalen, zabalíme ho
                    onOpenCloseNode(node, false)
                } else {    // jdeme na parenta
                    var parent = getNodeParent(nodes, selectedId)
                    parent && this.handleNodeClick(parent, true)
                }
            }
        }
    },
    ArrowRight: function(e) {
        const {nodes, selectedId, multipleSelection, expandedIds, onOpenCloseNode} = this.props
        e.stopPropagation()
        e.preventDefault()

        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null) {
                var node = nodes[index]
                if (node.hasChildren) {
                    if (!expandedIds[node.id]) {    // je zabalen, rozbalíme ho
                        onOpenCloseNode(node, true)
                    } else {    // jdeme na prvního potomka
                        var firstChild = getNodeFirstChild(nodes, selectedId);
                        firstChild && parseInt(firstChild.id) && this.handleNodeClick(firstChild, true)
                    }
                } else {    // nemá potomky, nic neděláme
                }
            }
        }
    }
};

/**
 * Strom archivních souborů.
 */
class FundTreeLazy extends AbstractReactComponent {
    state = {};

    static defaultProps = {
        rowHeight: 16
    };

    static PropTypes = {
        expandedIds: React.PropTypes.object.isRequired,
        cutLongLabels: React.PropTypes.bool.isRequired,
        selectedId: React.PropTypes.number,
        selectedIds: React.PropTypes.object,
        filterText: React.PropTypes.string,
        searchedIds: React.PropTypes.array,
        filterCurrentIndex: React.PropTypes.number,
        nodes: React.PropTypes.array.isRequired,
        focusId: React.PropTypes.number,
        rowHeight: React.PropTypes.number.isRequired,
        isFetching: React.PropTypes.bool.isRequired,
        fetched: React.PropTypes.bool.isRequired,
        onNodeClick: React.PropTypes.func,
        onNodeDoubleClick: React.PropTypes.func,
        onOpenCloseNode: React.PropTypes.func,
        onContextMenu: React.PropTypes.func,
        actionAddons: React.PropTypes.object,
    };


    componentDidMount() {
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['ensureItemVisible', 'filterText', 'expandedIds', 'selectedId', 'selectedIds', 'nodes', 'focusId', 'isFetching',
            'fetched', 'searchedIds', 'filterCurrentIndex', 'handleNodeClick', 'handleNodeDoubleClick']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    focus = () => {
        ReactDOM.findDOMNode(this.refs.treeWrapper).focus()
    };

    handleKeyDown = (event) => {
        if (document.activeElement === ReactDOM.findDOMNode(this.refs.treeWrapper)) { // focus má strom
            if (keyDownHandlers[event.key]) {
                keyDownHandlers[event.key].call(this, event)
            }
        }
    };

    /**
     * Renderování uzlu.
     * @param node {Object} uzel
     * @return {Object} view
     */
    renderNode = (node) => {
        const {onNodeDoubleClick, onOpenCloseNode, onContextMenu} = this.props;

        const expanded = node.hasChildren && this.props.expandedIds[node.id];

        const clickProps = {
            onClick: this.handleNodeClick.bind(this, node, false),
            onDoubleClick: this.handleNodeDoubleClick.bind(this, node, false),
        };

        let expCol;
        if (node.hasChildren) {
            const expColCls = 'exp-col ' + (expanded ? 'fa fa-minus-square-o' : 'fa fa-plus-square-o');
            expCol = <span className={expColCls} onClick={onOpenCloseNode.bind(this, node, !expanded)}></span>
        } else {
            expCol = <span {...clickProps} className='exp-col'>&nbsp;</span>
        }

        let active = false;
        active |= this.props.selectedId === node.id;
        if (this.props.selectedIds && this.props.selectedIds[node.id]) {
            active = true
        }
        const cls = classNames({
            node: true,
            opened: expanded,
            closed: !expanded,
            active: active,
            focus: this.props.focusId === node.id,
        });

        var levels = createReferenceMark(node, clickProps);

        let name = node.name ? node.name : i18n('fundTree.node.name.undefined', node.id);
        const title = name;
        if (this.props.cutLongLabels) {
            if (name.length > TREE_NAME_MAX_CHARS) {
                name = name.substring(0, TREE_NAME_MAX_CHARS - 3) + '...'
            }
        }

        return <div key={node.id} className={cls}>
            {levels}
            {expCol}
            <Icon {...clickProps} className="node-icon" glyph={getGlyph(node.icon)} />
            <span
                title={title}
                className='node-label'
                {...clickProps}
                onContextMenu={onContextMenu ? onContextMenu.bind(this, node) : null}>
                {name}
            </span>
        </div>;
    };

    handleNodeClick = (node, ensureItemVisible, e) => {
        const {onNodeClick} = this.props;
        onNodeClick && onNodeClick(node, ensureItemVisible, e)
    };

    handleNodeDoubleClick = (node, ensureItemVisible, e) => {
        const {onNodeDoubleClick} = this.props;
        onNodeDoubleClick && onNodeDoubleClick(node, ensureItemVisible, e)
    };

    render() {
        const {className, actionAddons, multipleSelection, onFulltextNextItem, onFulltextPrevItem, onFulltextSearch,
            onFulltextChange, filterText, searchedIds, filterCurrentIndex, filterResult,
            extendedSearch, onClickExtendedSearch, extendedReadOnly} = this.props;

        let index;
        if (this.props.ensureItemVisible) {
            if (multipleSelection) {
                if (Object.keys(this.props.selectedIds).length === 1) {
                    index = indexById(this.props.nodes, Object.keys(this.props.selectedIds)[0]);
                }
            } else {
                index = indexById(this.props.nodes, this.props.selectedId);
            }
        }

        let cls = 'fa-tree-lazy-main-container';
        if (className) {
            cls += " " + className
        }

        return <div className={cls}>
            <div className='fa-traa-header-container'>
                <SearchWithGoto
                    filterText={filterText}
                    itemsCount={searchedIds ? searchedIds.length : 0}
                    selIndex={filterCurrentIndex}
                    showFilterResult={filterResult}
                    onFulltextChange={onFulltextChange}
                    onFulltextSearch={onFulltextSearch}
                    onFulltextNextItem={onFulltextNextItem}
                    onFulltextPrevItem={onFulltextPrevItem}
                    extendedSearch={extendedSearch}
                    extendedReadOnly={extendedReadOnly}
                    onClickExtendedSearch={onClickExtendedSearch}
                />
            </div>
            <div className="fa-tree-wrapper" tabIndex={0} ref="treeWrapper" onKeyDown={this.handleKeyDown}>
                <div className="fa-tree-lazy-actions">
                    <Button className="tree-collapse" onClick={this.props.onCollapse}><Icon glyph='fa-compress'/>Sbalit vše</Button>
                    {actionAddons}
                </div>
                <div className='fa-tree-lazy-container' ref="treeContainer" >
                    {this.state.treeContainer && <VirtualList
                        scrollTopPadding={TREE_TOP_PADDING}
                        tagName='div'
                        scrollToIndex={index}
                        container={this.state.treeContainer}
                        items={this.props.nodes}
                        renderItem={this.renderNode}
                        itemHeight={this.props.rowHeight}
                    />}
                </div>
            </div>
        </div>
    }
}

export default connect(null, null, null, { withRef: true })(FundTreeLazy);
