/**
 *  ListBox komponenta.
 *
 **/

import React from 'react';
import {AbstractReactComponent, Utils} from 'components/index.jsx';
import ReactDOM from 'react-dom';
import {Shortcuts} from 'react-shortcuts';
const scrollIntoView = require('dom-scroll-into-view')
import {PropTypes} from 'prop-types';
import defaultKeymap from './ListBoxKeymap.jsx'
require ('./ListBox.less');

var _ListBox_placeholder = document.createElement("div");
var _ListBox_placeholder_cls = "placeholder"
_ListBox_placeholder.className = _ListBox_placeholder_cls;

const PAGE_SIZE = 10;

var ListBox = class ListBox extends AbstractReactComponent {
    static contextTypes = { shortcuts: PropTypes.object };
    static childContextTypes = { shortcuts: PropTypes.object.isRequired };
    componentWillMount(){
        Utils.addShortcutManager(this,defaultKeymap);
    }
    getChildContext() {
        return { shortcuts: this.shortcutManager };
    }

    constructor(props) {
        super(props);

        this.bindMethods('ensureItemVisible', 'getNextSelectableItemIndex', 'getPrevSelectableItemIndex',
            'dragStart', 'dragEnd', 'dragOver', 'handleClick', 'unFocus', 'focus', 'handleDoubleClick')

        if (props.multiselect) {
            var activeIndexes = {}
            if (typeof props.activeIndexes !== 'undefined' && props.activeIndexes !== null) {
                props.activeIndexes.forEach(index => activeIndexes[index] = true)
            }
            this.state = {
                activeIndexes: activeIndexes,
                lastFocus: null,
            }
        } else {
            this.state = {
                activeIndex: this.getActiveIndexForUse(props, {}),
                lastFocus: null,
            }
        }
    }
    selectorMoveUp = (e)=>{
        this.selectorMoveRelative(-1);
    }
    selectorMoveDown = (e)=>{
        this.selectorMoveRelative(1);
    }
    selectorMovePageUp = (e)=>{
        this.selectorMoveRelative(-1*PAGE_SIZE);
    }
    selectorMovePageDown = (e)=>{
        this.selectorMoveRelative(PAGE_SIZE);
    }
    selectorMoveTop = (e)=>{
        this.selectorMoveToIndex(0);
    }
    selectorMoveEnd = (e)=>{
        this.selectorMoveToIndex(this.props.items.length-1);
    }
    selectedItemCheck = (e)=>{
        this.selectedItemOperation(this.props.onCheck);
    }
    selectedItemDelete = (e)=>{
        this.selectedItemOperation(this.props.onDelete);
    }
    selectItem = (e)=>{
        this.selectedItemOperation(this.props.onSelect);
    }
    /**
     * Wrapper for item operations from props. Checks if the operation exists and that an item is selected.
     * @param {function} operation
     */
    selectedItemOperation = (operation)=>{
        const {items} = this.props;
        const {activeIndex} = this.state;
        if(operation && activeIndex !== null){
            operation(items[activeIndex],activeIndex)
        }
    }
    selectorMoveRelative = (step) => {
        const {lastFocus} = this.state;
        var newIndex = this.getRelativeSelectableItemIndex(lastFocus, step)
        this.selectorMoveToIndex(newIndex);
    }
    selectorMoveToIndex = (index) => {
        const {items, multiselect} = this.props;
        if (items.length > 0) {
            if (index !== null) {
                var state = multiselect ? {lastFocus: index, activeIndexes: {[index]: true}} : {lastFocus: index, activeIndex: index};
                this.setState(state, this.ensureItemVisible.bind(this, index));
                this.props.onFocus && this.props.onFocus(items[index], index);
                this.props.onChangeSelection && this.props.onChangeSelection([index]);
            }
        }
    }
    actionMap = {
        "MOVE_UP":this.selectorMoveUp,
        "MOVE_DOWN":this.selectorMoveDown,
        "MOVE_PAGE_UP":this.selectorMovePageUp,
        "MOVE_PAGE_DOWN":this.selectorMovePageDown,
        "MOVE_TOP":this.selectorMoveTop,
        "MOVE_END":this.selectorMoveEnd,
        "ITEM_CHECK":this.selectedItemCheck,
        "ITEM_DELETE":this.selectedItemDelete,
        "ITEM_SELECT":this.selectItem
    }
    handleShortcuts = (action, e)=>{
        e.stopPropagation();
        e.preventDefault();
        console.log(action)
        this.actionMap[action](e);
    }
    componentWillReceiveProps(nextProps) {
        if (nextProps.multiselect) {
            if (typeof nextProps.activeIndexes !== 'undefined') {
                var activeIndexes = {}
                nextProps.activeIndexes !== null && nextProps.activeIndexes.forEach(index => activeIndexes[index] = true)
                this.setState({
                    activeIndexes: activeIndexes,
                })
            }
        } else {
            this.setState({
                activeIndex: this.getActiveIndexForUse(nextProps, this.state),
                lastFocus: this.getActiveIndexForUse(nextProps, this.state),
            })
        }
    }

    handleClick(index, e) {
        const {items, multiselect, canSelectItem} = this.props
        var {activeIndexes, lastFocus} = this.state

        if (multiselect) {
            if (e.ctrlKey) {
                if (activeIndexes[index]) { // je označená, odznačíme ji
                    activeIndexes = {...activeIndexes}
                    delete activeIndexes[index]
                } else {
                    if (canSelectItem(items[index], index)) {
                        activeIndexes = {...activeIndexes, [index]: true}
                    }
                }
            } else if (e.shiftKey) {
                this.unFocus()

                const from = Math.min(index, lastFocus)
                const to = Math.max(index, lastFocus)
                activeIndexes = {...activeIndexes}
                for (var a=from; a<=to; a++) {
                    if (canSelectItem(items[a], a)) {
                        activeIndexes[a] = true
                    }
                }
            } else {
                if (canSelectItem(items[index], index)) {
                    activeIndexes = {[index]: true}
                }
            }
            this.setState({
                activeIndexes: activeIndexes,
                lastFocus: index,
            })
            this.props.onChangeSelection && this.props.onChangeSelection(Object.keys(activeIndexes))
        } else {
            if (canSelectItem(items[index], index)) {
                this.setState({
                    activeIndex: index,
                    lastFocus: index,
                })
                this.props.onFocus && this.props.onFocus(items[index], index)
                if (this.state.activeIndex !== index) {
                    this.props.onChangeSelection && this.props.onChangeSelection([index])
                }
            }
        }
    }

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges()
        }
    }

    dragStart(index, e) {
        const {items, multiselect, canSelectItem} = this.props;

        this.dragged = e.currentTarget;
        e.dataTransfer.effectAllowed = 'move';
        // Firefox requires dataTransfer data to be set
        e.dataTransfer.setData("text/html", e.currentTarget);

        var canSelect = canSelectItem(items[index], index);
    }

    dragEnd(e) {
        this.dragged.style.display = "block";
        this.dragged.parentNode.removeChild(_ListBox_placeholder);
        // Update data
        var data = this.state.data;
        var from = Number(this.dragged.dataset.id);
        var to = Number(this.over.dataset.id);
        if(from < to) to--;
        if(this.nodePlacement == "after") to++;

        if (from !== to) {
            this.props.onChangeOrder(from, to)
        }
    }

    dragOver(e) {
        e.preventDefault();
        this.dragged.style.display = "none";
        if(e.target.className == _ListBox_placeholder_cls) return;
        this.over = e.target;
        // Inside the dragOver method
        var relY = e.clientY - this.over.offsetTop;
        var height = this.over.offsetHeight / 2;
        var parent = e.target.parentNode;
        var container = this.refs.container;

        var realTarget = e.target;
        var found = false;
        while (realTarget !== null) {
            if (typeof realTarget.dataset.id !== 'undefined') {
                found = true;
                break
            }
            if (realTarget == container) {
                realTarget = container.lastChild;
                if (realTarget.className.indexOf(_ListBox_placeholder_cls) > -1) {
                    realTarget = realTarget.previousSibling;
                }
                found = true;
                break;
            }
            realTarget = realTarget.parentNode
        }

        if (!found) {
            return
        }

        this.over = realTarget;

        // Inside the dragOver method
        var parent = realTarget.parentNode;
        var overRect = this.over.getBoundingClientRect();
        var height2 = (overRect.bottom - overRect.top) / 2;

        if (e.clientY < overRect.top + height2) {
            this.nodePlacement = "before"
            parent.insertBefore(_ListBox_placeholder, realTarget);
        } else if (e.clientY >= overRect.top + height2) {
            this.nodePlacement = "after";
            parent.insertBefore(_ListBox_placeholder, realTarget.nextElementSibling);
        } else {

        }
    }
    getRelativeSelectableItemIndex = (index, step) => {
        const {items, canSelectItem} = this.props;
        var isDecrementing = step < 0;
        if(index || index === 0){
            while (step) {
                var i = index + step;
                while (i >= 0 && i < items.length) {
                    console.log(i);
                    if (canSelectItem(items[i], i)) {
                        return i;
                    }
                    isDecrementing ? i-- : i++;
                }
                isDecrementing ? step++ : step--;
            }
            console.log(index);
            return index;
        } else {
            return 0;
        }
    }

    getActiveIndexForUse(props, state) {
        var index

        if (typeof props.activeIndex !== 'undefined') {
            index = props.activeIndex
        } else if (typeof state.activeIndex !== 'undefined') {
            index = state.activeIndex
        } else {
            index = null
        }

        if (index < 0 || index >= props.items.length) {
            index = null
        }
        return index
    }

    ensureItemVisible(index) {

        var itemNode = ReactDOM.findDOMNode(this.refs['item-' + index])
        if (itemNode !== null) {
            var containerNode = ReactDOM.findDOMNode(this.refs.container)
            console.log("ensureItemVisible",itemNode,containerNode);
            scrollIntoView(itemNode, containerNode, { onlyScrollIfNeeded: true, alignWithTop:false })
        }
    }

    handleDoubleClick(e) {
        const {onDoubleClick} = this.props

        if (onDoubleClick) {
            this.unFocus();
            onDoubleClick(e)
        }
    }

    unFocus() {
        if (document.selection) {
            document.selection.empty();
        } else {
            window.getSelection().removeAllRanges()
        }
    }

    focus() {
        this.setState({}, () => {ReactDOM.findDOMNode(this.refs.wrapper).focus()})
    }

    render() {
        const {className, items, renderItemContent, multiselect} = this.props;
        const {activeIndex, activeIndexes} = this.state;

        var cls = "listbox-container";
        var wrapperClass = "listbox-wrapper"
        if (className){
             wrapperClass += " " + className
        }
        var rows = items.map((item, index) => {
            const active = multiselect ? (activeIndexes[index]) : (index === activeIndex)
            var draggableProps = {}
            if (this.props.sortable) {
                draggableProps = {
                    draggable: true,
                    onDragEnd: this.dragEnd,
                    onDragStart: this.dragStart.bind(this, index),
                }
            }
            return (
                <div
                    className={'listbox-item' + (active ? ' active' : '')}
                    ref={'item-' + index}
                    key={index}
                    data-id={index}
                    onMouseDown={this.handleClick.bind(this, index)}
                    onDoubleClick={this.handleDoubleClick}
                    {...draggableProps}
                >
                    {renderItemContent(item, active, index)}
                </div>
            )
        })

        return (
            <Shortcuts ref="wrapper" name="ListBox" handler={this.handleShortcuts} tabIndex={"0"} className={wrapperClass}>
                <div className={cls} ref='container' onDragOver={this.dragOver}>
                    {rows}
                </div>
            </Shortcuts>
        );
    }
}
ListBox.propsTypes = {
    items: React.PropTypes.array.isRequired,
    onSelect: React.PropTypes.func,
    onCheck: React.PropTypes.func,
    onDelete: React.PropTypes.func,
    canSelectItem: React.PropTypes.bool,
    multiselect: React.PropTypes.bool,
    onFocus: React.PropTypes.func,
    onChangeSelection: React.PropTypes.func,
    activeIndexes: React.PropTypes.array,
    onChangeOrder: React.PropTypes.func,
    className: React.PropTypes.string,
    renderItemContent: React.PropTypes.func.isRequired,
    sortable: React.PropTypes.bool
};

ListBox.defaultProps = {
    renderItemContent: (item, isActive, index) => {
        return (
            <div>{item.name}</div>
        )
    },
    canSelectItem: (item, index) => {
        return true
    }
};

module.exports = ListBox;
