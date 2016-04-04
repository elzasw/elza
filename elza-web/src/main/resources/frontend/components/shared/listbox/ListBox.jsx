/**
 *  ListBox komponenta.
 *
 **/

import React from 'react';
import {AbstractReactComponent} from 'components';
import ReactDOM from 'react-dom';
const scrollIntoView = require('dom-scroll-into-view')

require ('./ListBox.less');

var _ListBox_placeholder = document.createElement("div");
var _ListBox_placeholder_cls = "placeholder"
_ListBox_placeholder.className = _ListBox_placeholder_cls;

var keyDownHandlers = {
    Enter: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {items} = this.props
        const {activeIndex} = this.state

        if (activeIndex !== null) {
            this.props.onSelect && this.props.onSelect(items[activeIndex], activeIndex)
        }
    },
    ' ': function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {items} = this.props
        const {activeIndex} = this.state

        if (activeIndex !== null) {
            this.props.onCheck && this.props.onCheck(items[activeIndex], activeIndex)
        }
    },
    Home: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {items, canSelectItem, multiselect} = this.props

        if (items.length > 0) {
            var newActiveIndex = 0
            if (!canSelectItem(items[newActiveIndex], newActiveIndex)) {
                newActiveIndex = this.getNextSelectableItemIndex(newActiveIndex)
            }

            var state = multiselect ? {lastFocus: newActiveIndex, activeIndexes: {[newActiveIndex]: true}} : {lastFocus: newActiveIndex, activeIndex: newActiveIndex}
            this.setState(state, this.ensureItemVisible.bind(this, newActiveIndex))
            this.props.onFocus && this.props.onFocus(items[newActiveIndex], newActiveIndex)
            this.props.onChangeSelection && this.props.onChangeSelection([newActiveIndex])
        }
    },
    End: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {items, canSelectItem, multiselect} = this.props

        if (items.length > 0) {
            var newActiveIndex = items.length - 1
            if (!canSelectItem(items[newActiveIndex], newActiveIndex)) {
                newActiveIndex = this.getPrevSelectableItemIndex(newActiveIndex)
            }

            var state = multiselect ? {lastFocus: newActiveIndex, activeIndexes: {[newActiveIndex]: true}} : {lastFocus: newActiveIndex, activeIndex: newActiveIndex}
            this.setState(state, this.ensureItemVisible.bind(this, newActiveIndex))
            this.props.onFocus && this.props.onFocus(items[newActiveIndex], newActiveIndex)
            this.props.onChangeSelection && this.props.onChangeSelection([newActiveIndex])
        }
    },
    ArrowUp: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {lastFocus} = this.state
        const {items, multiselect} = this.props

        if (items.length > 0) {
            var newActiveIndex = null

            if (lastFocus === null) {
                newActiveIndex = 0
            } else {
                newActiveIndex = this.getPrevSelectableItemIndex(lastFocus)
            }
            if (newActiveIndex !== null) {
                var state = multiselect ? {lastFocus: newActiveIndex, activeIndexes: {[newActiveIndex]: true}} : {lastFocus: newActiveIndex, activeIndex: newActiveIndex}
                this.setState(state, this.ensureItemVisible.bind(this, newActiveIndex))
                this.props.onFocus && this.props.onFocus(items[newActiveIndex], newActiveIndex)
                this.props.onChangeSelection && this.props.onChangeSelection([newActiveIndex])
            }
        }
    },
    ArrowDown: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {lastFocus} = this.state
        const {items, multiselect} = this.props

        if (items.length > 0) {
            var newActiveIndex = null

            if (lastFocus === null) {
                newActiveIndex = 0
            } else {
                newActiveIndex = this.getNextSelectableItemIndex(lastFocus)
            }
            if (newActiveIndex !== null) {
                var state = multiselect ? {lastFocus: newActiveIndex, activeIndexes: {[newActiveIndex]: true}} : {lastFocus: newActiveIndex, activeIndex: newActiveIndex}
                this.setState(state, this.ensureItemVisible.bind(this, newActiveIndex))
                this.props.onFocus && this.props.onFocus(items[newActiveIndex], newActiveIndex)
                this.props.onChangeSelection && this.props.onChangeSelection([newActiveIndex])
            }
        }
    }
}

var ListBox = class ListBox extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleKeyDown', 'ensureItemVisible', 'getNextSelectableItemIndex', 'getPrevSelectableItemIndex',
            'dragStart', 'dragEnd', 'dragOver', 'handleClick')

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

    dragStart(index, e) {
        const {items, multiselect, canSelectItem} = this.props

        this.dragged = e.currentTarget;
        e.dataTransfer.effectAllowed = 'move';
        // Firefox requires dataTransfer data to be set
        e.dataTransfer.setData("text/html", e.currentTarget);

        var canSelect = canSelectItem(items[index], index)
        /*if (multiselect) {
            this.setState(canSelect ? {lastFocus: index, activeIndexes: {[index]: true}} : {lastFocus: index, activeIndexes: {}})
        } else {
            this.setState(canSelect ? {lastFocus: index, activeIndex: index} : {lastFocus: index, activeIndex: null})
        }*/
        if (multiselect) {
            this.setState({lastFocus: index, activeIndexes: {}})
        } else {
            this.setState({lastFocus: index, activeIndex: null})
        }
        this.props.onChangeSelection && this.props.onChangeSelection([])
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

        var realTarget = e.target;
        var found = false;
        while (realTarget !== null) {
            if (typeof realTarget.dataset.id !== 'undefined') {
                found = true;
                break
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

    getNextSelectableItemIndex(index) {
        const {items, canSelectItem} = this.props

        var i = index + 1
        while (i < items.length) {
            if (canSelectItem(items[i], i)) {
                return i
            }
            i++
        }
        return null
    }

    getPrevSelectableItemIndex(index) {
        const {items, canSelectItem} = this.props

        var i = index - 1
        while (i >= 0) {
            if (canSelectItem(items[i], i)) {
                return i
            }
            i--
        }
        return null
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

        if (index < 0) {
            index = props.items.length > 0 ? 0 : null
        } else if (index >= props.items.length) {
            index = props.items.length - 1
        }
        return index
    }

    ensureItemVisible(index) {
        var itemNode = ReactDOM.findDOMNode(this.refs['item-' + index])
        if (itemNode !== null) {
            var containerNode = ReactDOM.findDOMNode(this.refs.container)
            scrollIntoView(itemNode, containerNode, { onlyScrollIfNeeded: true, alignWithTop:false })
        }
    }

    handleKeyDown(event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
        }
    }

    focus() {
        this.refs.container.focus()
    }

    render() {
        const {className, items, renderItemContent, multiselect} = this.props;
        const {activeIndex, activeIndexes} = this.state;

        var cls = "listbox-container";
        if (className) {
            cls += " " + className;
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
                    onClick={this.handleClick.bind(this, index)}
                    onDoubleClick={this.props.onDoubleClick}
                    {...draggableProps}
                >
                    {renderItemContent(item)}
                </div>
            )
        })

        return (
            <div
                className={cls}
                onKeyDown={this.handleKeyDown}
                tabIndex={0}
                ref='container'
                onDragOver={this.dragOver}
            >
                {rows}  
            </div>
        );
    }
}

ListBox.defaultProps = {
    renderItemContent: (item, isActive) => {
        return (
            <div>{item.name}</div>
        )
    },
    canSelectItem: (item, index) => {
        return true
    }
}

module.exports = ListBox
