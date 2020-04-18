import React from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import ListItem from './list-item/ListItem.jsx';
import {Shortcuts} from 'react-shortcuts';
import {addShortcutManager} from 'components/Utils';
import defaultKeymap from './TreeListKeymap.jsx';
import scrollIntoView from 'dom-scroll-into-view';
import getItemName from 'components/shared/utils/getItemName.jsx';
import './TreeList.scss';

// node state codes
const nodeStates = {
    EXPANDED: 1,
    COLLAPSED: 2,
};

// example of items object
const exampleItems = {
    1: {
        id: 1,
        depth: 0,
        parent: null,
        /* ... item data*/
    },
    2: {
        id: 2,
        depth: 1,
        parent: 1,
        /* ... item data*/
    },
    ids: [1, 2],
};

// example of groups object
const exampleGroups = [
    {
        name: 'main', // required
        title: 'Main group',
        hideWhenEmpty: true,
        hideTitle: true,
        ignoreDepth: true,
        ids: ['1', '2', '3'], // optional - when ids are undefined, takes all
    },
];

class TreeList extends React.Component {
    static contextTypes = {shortcuts: PropTypes.object};
    static childContextTypes = {shortcuts: PropTypes.object.isRequired};

    static propTypes = {
        items: PropTypes.object.isRequired,
        groups: PropTypes.array,
        renderItem: PropTypes.func,
        allowSelectItem: PropTypes.func,
        allowFocusItem: PropTypes.func,
        renderIdDelimiter: PropTypes.string,
        loop: PropTypes.bool,
        tree: PropTypes.bool,
    };

    // default group configuration object
    static defaultGroup = {
        name: 'all',
        hideTitle: true,
    };

    static defaultProps = {
        items: {ids: []},
        groups: [TreeList.defaultGroup],
        renderItem: props => {
            const {item, ...otherProps} = props;
            return <ListItem {...otherProps} />;
        },
        allowSelectItem: item => {
            return true;
        },
        allowFocusItem: item => {
            return true;
        },
        renderIdDelimiter: '@',
        loop: false,
    };

    getChildContext() {
        return {shortcuts: this.shortcutManager};
    }

    constructor(props) {
        super(props);
        this.itemRefs = {ids: []};
        this.prevHeight = 0;
        this.scrollTop = 0;
        this.state = {
            highlightedItem: {
                index: null,
                id: null,
            },
            items: props.items,
            expandedIds: {},
        };
    }

    componentDidUpdate(prevProps, prevState) {
        const {onContentChange, highlightedItemId} = this.props;
        const {highlightedItem} = this.state;

        // scroll item into view after item selection changed
        this.scrollItemIntoView(highlightedItem.id);

        // restore scrollTop on list after expanded ids changed
        const scrollTop = this.scrollTop;
        if (prevState.expandedIds !== this.state.expandedIds) {
            onContentChange && onContentChange(this.list);
            this.list.scrollTop = scrollTop;
        }

        // highlight item specified in props, but only when it doesn't
        // match the currently highlighted item
        if (prevProps.highlightedItemId !== highlightedItemId) {
            this.highlightItem(highlightedItemId);
        }
    }

    componentDidMount() {
        this.highlightItem(this.props.highlightedItemId);
    }

    /**
     * Combines id and groupname to create ids for rendered items
     */
    buildId = (id, groupName) => {
        const {renderIdDelimiter} = this.props;
        if (!id && id !== 0) {
            console.warn('Id not specified');
            return null;
        }
        return [id, groupName].join(renderIdDelimiter);
    };

    /**
     * Restores the id and groupname from rendered item id
     */
    restoreId = renderId => {
        const {renderIdDelimiter} = this.props;
        if (!renderId && renderId !== 0) {
            console.warn('Invalid rendered item id', renderId);
            return [];
        }
        return renderId.split(renderIdDelimiter);
    };

    /**
     * Decides which item has higher priority
     * (items must have priority property for this to have any effect)
     */
    orderByPriority = (a, b) => {
        if (a.priority) {
            if (b.priority) {
                if (b.priority < a.priority) {
                    return -1;
                }
                return 1;
            }
            return -1;
        }
        if (b.priority) {
            return 1;
        }
        return 0;
    };

    /**
     * Create render id from group with the highest priority
     * where the specified id exists
     */
    getHighPriorityRenderedId = (id, force = false) => {
        const {highlightedItemId, groups} = this.props;

        // order groups by priority
        let orderedGroups = [...groups];
        orderedGroups.sort(this.orderByPriority);

        // search the priority ordered groups for specified id
        for (let g = 0; g < orderedGroups.length; g++) {
            const groupId = orderedGroups[g].name;
            const renderId = this.buildId(id, groupId);
            // return the first matching id (has highest priority)
            if (this.itemRefs[renderId] || force) {
                return renderId;
            }
        }
        return null;
    };

    /**
     * Checks if items object has data with tree structure
     * (individual items have different depth)
     */
    checkIfTree = items => {
        const {tree} = this.props;
        let hasDepth = tree;
        if (typeof tree == 'undefined') {
            let depth;
            // looks through all items
            for (let i = 0; i < items.ids.length; i++) {
                const itemId = items.ids[i];
                const item = items[itemId];
                // stops when depth difference is found
                if (typeof depth !== 'undefined' && depth !== item.depth) {
                    hasDepth = true;
                    break;
                }
                depth = item.depth;
            }
        }
        this.setState({
            isTree: hasDepth,
        });
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.checkIfTree(nextProps.items);
        this.setState({
            items: nextProps.items,
            expandedIds: {},
        });
    }

    /**
     * Sets the parents of the specified id as expanded
     */
    expandParents = id => {
        const {items, expandedIds} = this.state;
        let newExpandedIds = {...expandedIds};
        const restoredId = this.restoreId(id);
        const itemId = restoredId[0];
        const itemGroup = restoredId[1];

        if (itemId >= 0) {
            const includeItem = items[itemId];

            if (includeItem) {
                let parentId = includeItem.parent;
                let parentRenderId;

                while (parentId) {
                    parentRenderId = this.buildId(parentId, itemGroup);
                    newExpandedIds[parentRenderId] = nodeStates.EXPANDED;
                    parentId = items[parentId].parent;
                }
            }
        }

        this.setState({expandedIds: newExpandedIds});
    };

    UNSAFE_componentWillMount() {
        const {includeId, items} = this.props;
        this.checkIfTree(items);

        const renderedId = this.getHighPriorityRenderedId(includeId, true);
        this.expandParents(renderedId);

        addShortcutManager(this, defaultKeymap);
    }

    focus = () => {
        ReactDOM.findDOMNode(this.list).focus();
    };

    highlightItem = id => {
        let itemId = id;
        //console.log("highlight item "+id);
        if (!this.itemRefs[id]) {
            itemId = this.getHighPriorityRenderedId(id);
        }
        if (!itemId && itemId !== 0) {
            itemId = this.state.highlightedItem.id || this.itemRefs.ids[0];
        }
        this.setState({
            highlightedItem: {
                index: this.itemRefs.ids.indexOf(id),
                id: itemId,
            },
        });
    };

    /**
     * Scrolls the view to show specified item
     */
    scrollItemIntoView = renderId => {
        var itemNode = ReactDOM.findDOMNode(this.itemRefs[renderId]);

        if (itemNode) {
            var listNode = ReactDOM.findDOMNode(this.list);
            scrollIntoView(itemNode, listNode, {onlyScrollIfNeeded: true, allowHorizontalScroll: false});
        }
    };

    /**
     * Toggles the node's expand/collapse state
     * @param id id of the node
     * @param expand true - force expand, false - force collapse
     */
    toggleNode = (id, expand = null) => {
        const {expandedIds} = this.state;
        const {onToggleNode} = this.props;

        // save the current scroll top
        const listNode = ReactDOM.findDOMNode(this.list);
        this.scrollTop = listNode.scrollTop;

        const node = this.getNodeById(id);
        const isExpanded = this.isExpanded(id);
        // expand if the node is not expanded or is not being forcibly collapsed
        if ((expand === null && !isExpanded) || expand) {
            this.expandNode(id, node);
        } else {
            this.collapseNode(id, node);
        }

        onToggleNode && onToggleNode(node);
    };

    expandNode = (id, node) => {
        const {onExpandNode} = this.props;
        this.changeNodeState(id, nodeStates.EXPANDED);
        onExpandNode && onExpandNode(node);
    };

    collapseNode = (id, node) => {
        const {onCollapseNode} = this.props;
        this.changeNodeState(id, nodeStates.COLLAPSED);
        onCollapseNode && onCollapseNode(node);
    };

    changeNodeState = (id, state) => {
        const {expandedIds} = this.state;
        let newExpandedIds = {...expandedIds};
        newExpandedIds[id] = state;
        this.setState({
            expandedIds: newExpandedIds,
        });
    };

    /**
     * Gets id of the item relatively placed to the item with the specified id
     * @param {number} id id of the item
     * @param {number} step the step by how many items the selection will move (negative value goes in the other direction)
     * @param {bool} loop when true, loops to the other end when one end is reached
     * @return {number} id of the next possible item. when no possible item, returns null.
     */
    getRelativeSelectableItemId = (id, step, loop = false) => {
        const {allowFocusItem} = this.props;
        const items = this.itemRefs.ids;
        let index = this.itemRefs.ids.indexOf(id);
        const isDecrementing = step < 0;

        if (!items) {
            return null;
        }

        index = index > 0 ? index : 0;
        var nextIndex = index + step;
        while (nextIndex !== index) {
            if (loop) {
                if (nextIndex >= items.length) {
                    // Loop to the beginning
                    nextIndex = 0;
                } else if (nextIndex < 0) {
                    // Loop to the end
                    nextIndex = items.length - 1;
                }
            } else if ((!loop && nextIndex >= items.length) || (!loop && nextIndex < 0)) {
                return items[index];
            }
            var item = items[nextIndex];
            if (allowFocusItem(item)) {
                return items[nextIndex];
            }
            isDecrementing ? nextIndex-- : nextIndex++;
        }
        return null;
    };

    selectorMoveToChildOrOpen = e => {
        const {highlightedItem, expandedIds, items} = this.state;
        const restoredId = this.restoreId(highlightedItem.id);
        const nodeId = restoredId[0];
        const node = items[nodeId];
        const isExpanded = this.isExpanded(highlightedItem.id);

        if (node) {
            if (node.children && node.children.length > 0) {
                if (isExpanded) {
                    // Move to child, if expanded
                    const nextNodeId = this.getRelativeSelectableItemId(highlightedItem.id, 1);
                    if (nextNodeId) {
                        this.highlightItem(nextNodeId);
                    }
                } else {
                    // Expand, if collapsed
                    this.toggleNode(highlightedItem.id, true);
                }
            }
        }
    };

    selectorMoveToParentOrClose = e => {
        const {items, highlightedItem, expandedIds} = this.state;
        const restoredId = this.restoreId(highlightedItem.id);
        const groupName = restoredId[1];
        const nodeId = restoredId[0];
        const node = items[nodeId];
        const isExpanded = this.isExpanded(highlightedItem.id);

        if (node) {
            if (isExpanded) {
                // Collapse node, if expanded and has children
                this.toggleNode(highlightedItem.id, false);
            } else {
                // Move to parent, if collapsed
                const parent = node.parent && node.parent;
                const renderedParentId = this.buildId(parent, groupName);
                this.highlightItem(renderedParentId);
            }
        }
    };

    getHighlightedNode = () => {
        const {
            highlightedItem: {id, index},
        } = this.state;
        const nodeId = id;

        if (id !== null) {
            return this.getNodeById(nodeId);
        }

        return null;
    };

    getNodeById = id => {
        const {items} = this.state;
        const restoredId = this.restoreId(id);
        const nodeId = restoredId[0];

        if (id !== null) {
            return items[nodeId];
        } else {
            return null;
        }
    };

    selectorMoveDown = e => {
        this.selectorMoveRelative(1);
    };

    selectorMoveUp = e => {
        this.selectorMoveRelative(-1);
    };

    selectorMoveRelative = step => {
        const {highlightedItem} = this.state;
        const {loop} = this.props;
        const index = this.getRelativeSelectableItemId(highlightedItem.id, step, loop);

        this.highlightItem(index);
    };

    selectItem = item => {
        const {allowSelectItem, onChange} = this.props;

        if (item) {
            if (allowSelectItem(item)) {
                this.setState(
                    {
                        value: item,
                    },
                    () => {
                        onChange && onChange(item);
                    },
                );
            }
        } else {
            onChange && onChange(null);
        }
    };

    selectHighlightedItem = () => {
        const item = this.getHighlightedNode();
        this.selectItem(item);
    };

    handleExpandCollapse = (id, e) => {
        e.stopPropagation();
        e.preventDefault();

        this.toggleNode(id);
    };

    /**
     * Determines whether the node should be expanded
     */
    isExpanded = itemId => {
        const {expandedIds} = this.state;
        const {expandAll} = this.props;
        const node = this.getNodeById(itemId);
        const hasChildren = node.children && node.children.length > 0;
        const isExpanded = expandedIds[itemId] === nodeStates.EXPANDED;
        // if expandAll is set and the node's state is not COLLAPSED
        const isNotCollapsed = expandAll && expandedIds[itemId] != nodeStates.COLLAPSED;

        // returns true if the node is either EXPANDED or if it is not explicitly set
        // to COLLAPSED, has children and expandAll prop is true
        return isExpanded || (isNotCollapsed && hasChildren);
    };

    renderGroupTitle = props => {
        const {title, name} = props;
        return (
            <div key={name} className="group-title">
                <div className="title-text">{title}</div>
                <hr />
            </div>
        );
    };

    renderItems = () => {
        const {items, highlightedItem, isTree} = this.state;
        const {
            renderItem,
            allowSelectItem,
            allowFocusItem,
            favoriteItems,
            groups,
            includeId,
            selectedItemId,
        } = this.props;

        let renderedItems = [];
        let currentDepth = 0; // current depth level of the tree
        let isHighlighted = false;
        let isSelected = false;
        //debugger;

        // reset itemRefs
        this.itemRefs = {ids: []};

        // loop through groups
        for (let gid = 0; gid < groups.length; gid++) {
            const group = {
                ...TreeList.defaultGroup,
                ...groups[gid],
            };
            const {name, title, ids, ignoreDepth, hideTitle, hideWhenEmpty} = group;

            // if group doesn't have used items ids defined
            // use all items ids
            const groupsItemsIds = ids ? ids : items.ids;

            // hide the group title when there are no items in the group
            // and it is set in the group props
            if (hideWhenEmpty && groupsItemsIds.length <= 0) {
                continue;
            } else if (!hideTitle) {
                renderedItems.push(this.renderGroupTitle({name, title}));
            }

            // loop through group's items
            for (let i = 0; i < groupsItemsIds.length; i++) {
                const itemId = groupsItemsIds[i];
                const item = items[itemId];

                if (!item) {
                    continue;
                } // skip item if it doesnt exist

                const renderedItemId = this.buildId(itemId, name);
                const isExpanded = this.isExpanded(renderedItemId);

                // render items as tree
                if (!ignoreDepth && isTree) {
                    // skip item if it has higher, than currently
                    // allowed, depth (its parent is not expanded)
                    if (currentDepth < item.depth) {
                        continue;
                    }
                    // when item depth is lower than the currently allowed one,
                    // set the allowed depth the same value (end of a subtree)
                    if (currentDepth > item.depth) {
                        currentDepth = item.depth;
                    }

                    // when the item is expanded, increase the allowed depth level
                    // to render its subtree
                    if (isExpanded) {
                        currentDepth = item.depth + 1;
                    }
                }

                isSelected = item.id && selectedItemId === item.id;
                isHighlighted = highlightedItem.id === renderedItemId;

                let renderedItem = renderItem({
                    name: getItemName(item), // + " " + item.id + " " + renderedItemId + " " + allowSelectItem(item), // debug name
                    item,
                    className: item.className,
                    hasChildren: item.children && item.children.length > 0,
                    depth: item.depth,
                    ignoreDepth: ignoreDepth || !isTree,
                    expanded: isExpanded,
                    highlighted: isHighlighted,
                    selected: isSelected,
                    selectable: allowSelectItem(item),
                    focusable: allowFocusItem(item),
                    onMouseEnter: e => {
                        this.highlightItem(renderedItemId);
                    },
                    onExpandCollapse: e => {
                        this.handleExpandCollapse(renderedItemId, e);
                    },
                    onClick: e => {
                        this.selectItem(item);
                    },
                    key: renderedItemId,
                    ref: ref => {
                        this.itemRefs[renderedItemId] = ref;
                    },
                });

                this.itemRefs.ids.push(renderedItemId);
                renderedItems.push(renderedItem);
            }
        }

        return renderedItems;
    };

    actionMap = {
        MOVE_UP: this.selectorMoveUp,
        MOVE_DOWN: this.selectorMoveDown,
        MOVE_TO_PARENT_OR_CLOSE: this.selectorMoveToParentOrClose,
        MOVE_TO_CHILD_OR_OPEN: this.selectorMoveToChildOrOpen,
        SELECT_ITEM: this.selectHighlightedItem,
    };

    handleShortcuts = (action, e) => {
        if (!this.props.disableShortcuts) {
            e.preventDefault();
            this.actionMap[action](e);
        }
    };

    handleScroll = e => {
        // save the current scroll position
        this.scrollTop = this.list.scrollTop;
    };

    render() {
        return (
            <Shortcuts
                handler={this.handleShortcuts}
                name="List"
                alwaysFireHandler
                className="tree-list"
                key="treeListShortcuts"
            >
                <div
                    ref={list => {
                        this.list = list;
                    }}
                    key="treeList"
                    className="tree-list"
                    onScroll={this.handleScroll}
                >
                    {this.renderItems()}
                </div>
            </Shortcuts>
        );
    }
}

export default TreeList;
