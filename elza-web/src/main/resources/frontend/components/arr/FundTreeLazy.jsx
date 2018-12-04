import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {VirtualList, NoFocusButton, AbstractReactComponent, i18n, HorizontalLoader, StoreHorizontalLoader, Icon, SearchWithGoto, Utils} from 'components/shared';
import {Nav, Input, NavItem, Button, DropdownButton} from 'react-bootstrap';
const classNames = require('classnames');
import {propsEquals} from 'components/Utils.jsx'
import {indexById} from 'stores/app/utils.jsx'
import {createReferenceMark, getNodeIcon, getNodePrevSibling, getNodeNextSibling, getNodeParent, getNodeFirstChild} from 'components/arr/ArrUtils.jsx'
import './FundTreeLazy.less';
import {Shortcuts} from 'react-shortcuts';
import {PropTypes} from 'prop-types';
import defaultKeymap from './FundTreeLazyKeymap.jsx';

// Na kolik znaků se má název položky stromu oříznout, jen pokud je nastaven vstupní atribut, že se má název ořezávat
const TREE_NAME_MAX_CHARS = 60

// Odsazení odshora, musí být definováno, jinak nefunguje ensureItemVisible
const TREE_TOP_PADDING = 0

/**
 * Strom archivních souborů.
 */
class FundTreeLazy extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };

    static defaultProps = {
        showSearch: true,
        showCountStats: false,
        showCollapseAll: true,
        onLinkClick:null
    };

    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }

    state = {};

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
        showSearch: React.PropTypes.bool,
        showCountStats: React.PropTypes.bool,
        showCollapseAll: React.PropTypes.bool,
        onLinkClick: React.PropTypes.func
    };
    selectorMoveUp = ()=>{
        const {nodes, selectedId, multipleSelection} = this.props;
        if (!multipleSelection && selectedId !== null) {
            var index = indexById(nodes, selectedId)
            if (index !== null && index > 0) {
                this.handleNodeClick(nodes[index - 1], true)
            }
        }
    }
    selectorMoveDown = ()=>{
        const {nodes, selectedId, multipleSelection} = this.props;
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
    }
    selectorMoveToParentOrClose = ()=>{
        const {nodes, selectedId, multipleSelection, expandedIds, onOpenCloseNode} = this.props;
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
    }
    selectorMoveToChildOrOpen = ()=>{
        const {nodes, selectedId, multipleSelection, expandedIds, onOpenCloseNode} = this.props;
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
    handleNodeClick = (node, ensureItemVisible, e)=>{
        const {onNodeClick} = this.props;
        onNodeClick && onNodeClick(node, ensureItemVisible, e)
    }
    handleNodeDoubleClick = (node, ensureItemVisible, e)=>{
        const {onNodeDoubleClick} = this.props;
        onNodeDoubleClick && onNodeDoubleClick(node, ensureItemVisible, e)
    }
    actionMap = {
        "MOVE_UP": this.selectorMoveUp,
        "MOVE_DOWN": this.selectorMoveDown,
        "MOVE_TO_PARENT_OR_CLOSE":this.selectorMoveToParentOrClose,
        "MOVE_TO_CHILD_OR_OPEN":this.selectorMoveToChildOrOpen
    }
    handleShortcuts = (action,e)=>{
        if(this.actionMap && typeof this.actionMap[action] === "function"){
            e.stopPropagation();
            e.preventDefault();
            this.actionMap[action](e);
        }
    }

    componentDidMount() {
        this.setState({treeContainer: ReactDOM.findDOMNode(this.refs.treeContainer)});
    }

    shouldComponentUpdate(nextProps, nextState) {
        if (this.state !== nextState) {
            return true;
        }
        var eqProps = ['ensureItemVisible', 'filterText', 'expandedIds', 'selectedId', 'selectedIds', 'nodes', 'focusId', 'isFetching',
            'fetched', 'searchedIds', 'filterCurrentIndex', 'handleNodeClick', 'handleNodeDoubleClick', 'colorCoded']
        return !propsEquals(this.props, nextProps, eqProps);
    }

    focus = () => {
        ReactDOM.findDOMNode(this.refs.treeWrapper).focus()
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
            onClick: (e)=>this.handleNodeClick(node, false, e),
            onDoubleClick: (e)=>this.handleNodeDoubleClick(node, false, e),
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
            "node-color": this.props.colorCoded
        });
        const iconClass = classNames({
            "node-icon": true,
            "node-icon-color": this.props.colorCoded
        });
        var levels = createReferenceMark(node, clickProps);

        let name = node.name ? node.name : i18n('fundTree.node.name.undefined', node.id);
        const title = name;
        if (this.props.cutLongLabels) {
            if (name.length > TREE_NAME_MAX_CHARS) {
                name = name.substring(0, TREE_NAME_MAX_CHARS - 3) + '...'
            }
        }
        
        const iconProps = getNodeIcon(this.props.colorCoded, node.icon);

        return <div key={node.id} className={cls}>
            {levels}
            {expCol}
            <Icon {...clickProps} className={iconClass} {...iconProps}/>
            <div
                title={title}
                className='node-label'
                {...clickProps}
                onContextMenu={onContextMenu ? onContextMenu.bind(this, node) : null}>
                {name}
                {this.props.showCountStats && node.count && <span className="count-label">({node.count})</span>}
                {this.props.onLinkClick && node.link && <Icon glyph="fa-sign-out fa-lg" onClick={() => this.props.onLinkClick(node)}/>}
            </div>
        </div>;
    };

    render() {
        const {fetched, isFetching, className, actionAddons, multipleSelection, onFulltextNextItem, onFulltextPrevItem, onFulltextSearch,
            onFulltextChange, filterText, searchedIds, filterCurrentIndex, filterResult,
            extendedSearch, onClickExtendedSearch, extendedReadOnly, showCollapseAll} = this.props;

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
                {this.props.showSearch && <SearchWithGoto
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
                />}
            </div>
            <Shortcuts className="fa-tree-wrapper" name="FundTreeLazy" tabIndex={0} handler={(action,e)=>this.handleShortcuts(action,e)} ref="treeWrapper" >
                <div className="fa-tree-lazy-actions">
                    {showCollapseAll && <Button className="tree-collapse" onClick={this.props.onCollapse}><Icon glyph='fa-compress'/>Sbalit vše</Button>}
                    {actionAddons}
                </div>
                <div className='fa-tree-lazy-container' ref="treeContainer">
                    <StoreHorizontalLoader store={{fetched, isFetching}} />
                    {this.state.treeContainer && <VirtualList
                        scrollTopPadding={TREE_TOP_PADDING}
                        tagName='div'
                        scrollToIndex={index}
                        container={this.state.treeContainer}
                        items={this.props.nodes}
                        renderItem={this.renderNode}
                    />}
                </div>
            </Shortcuts>
        </div>
    }
}

export default connect(null, null, null, { withRef: true })(FundTreeLazy);
