/**
 *  ListBox komponenta s daty načítanými ze serveru.
 *
 **/

require ('./LazyListBox.less');

import React from "react";
import ReactDOM from 'react-dom';
import {VirtualList, AbstractReactComponent, i18n} from "components";
import {indexById} from 'stores/app/utils.jsx'
var classNames = require('classnames');
const scrollIntoView = require('dom-scroll-into-view')

const _LLB_FETCH_DELAY = 32
const _LLB_FETCH_BOUNDARY = 200

function changeFocus(newActiveIndex) {
    if (this.state.lastFocus !== newActiveIndex) {
        var state = {lastFocus: newActiveIndex, activeIndex: newActiveIndex}
        this.setState(state, this.ensureItemVisible.bind(this, newActiveIndex))
        this.callCallbackAction(newActiveIndex, 'onFocus')
        if (!this.props.separateFocus) {
            this.callCallbackAction(newActiveIndex, 'onChangeSelection')
            this.callCallbackAction(newActiveIndex, 'onSelect')
        }
    }
}

var keyDownHandlers = {
    Enter: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {activeIndex} = this.state

        if (activeIndex !== null && this.state.selectedIndex !== activeIndex) {
            this.setState({selectedIndex: activeIndex})
            this.callCallbackAction(activeIndex, 'onChangeSelection')
            this.callCallbackAction(activeIndex, 'onSelect')
        }
    },
    ' ': function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {activeIndex} = this.state

        if (activeIndex !== null) {
            this.callCallbackAction(activeIndex, 'onCheck')
        }
    },
    Home: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {itemsCount} = this.state
        itemsCount > 0 && changeFocus.bind(this, 0)()
    },
    End: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {itemsCount} = this.state
        itemsCount > 0 && changeFocus.bind(this, itemsCount - 1)()
    },
    ArrowUp: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {itemsCount, lastFocus} = this.state
        itemsCount > 0 && changeFocus.bind(this, lastFocus === null ? 0 : Math.max(lastFocus - 1, 0))()
    },
    ArrowDown: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {itemsCount, lastFocus} = this.state
        itemsCount > 0 && changeFocus.bind(this, lastFocus === null ? 0 : Math.min(lastFocus + 1, itemsCount - 1))()
    },
    PageDown: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {itemHeight} = this.props
        const elHeight = ReactDOM.findDOMNode(this.refs.mainContainer).getBoundingClientRect().height
        const pageSize = Math.floor(elHeight / itemHeight)
        // console.log(elHeight, itemHeight, pageSize)

        const {itemsCount, lastFocus} = this.state
        itemsCount > 0 && changeFocus.bind(this, lastFocus === null ? 0 : Math.min(lastFocus + pageSize, itemsCount - 1))()
    },
    PageUp: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {itemHeight} = this.props
        const elHeight = ReactDOM.findDOMNode(this.refs.mainContainer).getBoundingClientRect().height
        const pageSize = Math.floor(elHeight / itemHeight)
        // console.log(elHeight, itemHeight, pageSize)

        const {itemsCount, lastFocus} = this.state
        itemsCount > 0 && changeFocus.bind(this, lastFocus === null ? 0 : Math.max(lastFocus - pageSize, 0))()
    }
}

var LazyListBox = class LazyListBox extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleKeyDown', 'ensureItemVisible',
            'handleClick', 'unFocus', 'handleViewChange', 'handleRenderItem', 'isFetching', 'callFetch', 'callCallbackAction',
            'handleDoubleClick', 'tryCallCallback', 'tryCallSingleCallback', 'tryUpdateSelectedIndex', 'fetchNow')

        this.currentFetch = []  // pole aktuálně načítaných dat ze serveru, obsahuje objekty s atributy: {id, from, to}
        this.fetchId = 0;

        this.fetchTimer = null
        this.callbackInfo = {}  // mapa nazvu callback metody na index, pro ktery se to ma zavolat, pokud je index null, nebudeme volat

        this.state = {
            activeIndex: this.getActiveIndexForUse(props, {}),
            selectedIndex: this.getSelectedIndexForUse(props, {}),
            lastFocus: null,
            itemsCount: typeof props.itemsCount !== 'undefined' ? props.itemsCount : 0, // zatím počet položek neznáme
            itemsFromIndex: 0,  // od jakého indexu máme položky
            itemsToIndex: 0,    // do jakého indexu máme položky
            items: [],  // načtené položky
            view: {from: 0, to: 0},
            scrollToIndex: {index: 0},
            selectedItem: props.selectedItem,
        }
    }

    componentDidMount() {
        this.setState({mainContainer: ReactDOM.findDOMNode(this.refs.mainContainer)});
        this.callFetch(0, 1, true)
    }

    componentWillReceiveProps(nextProps) {
        var newState = {
            activeIndex: this.getActiveIndexForUse(nextProps, this.state),
            selectedIndex: this.getSelectedIndexForUse(nextProps, this.state),
            lastFocus: this.getActiveIndexForUse(nextProps, this.state),
            selectedItem: nextProps.selectedItem,
        }

        if (this.props.selectedItem !== nextProps.selectedItem) {
            newState.selectedIndex = null
        }

        if (this.props.itemsCount !== nextProps.itemsCount) {
            newState.itemsCount = nextProps.itemsCount
        }

        if (nextProps.fetchNow) {
            if (this.props.selectedItem) {
                newState.activeIndex = null
                newState.selectedIndex = null
            }
            this.callFetch(this.state.itemsFromIndex, this.state.itemsToIndex, true)
        }

        this.setState(newState)
        this.tryUpdateSelectedIndex(this.state.itemsFromIndex, this.state.itemsToIndex, this.state.items)
    }

    tryCallSingleCallback(onCallbackName, itemsFrom, itemsTo, items) {
        if (this.callbackInfo[onCallbackName] !== null && this.callbackInfo[onCallbackName] >= itemsFrom && this.callbackInfo[onCallbackName] < itemsTo) {
            // console.log("  *CALL", this.callbackInfo[onCallbackName], onCallbackName)
            const onCallback = this.props[onCallbackName]
            if (onCallback) {
                const item = items[this.callbackInfo[onCallbackName] - itemsFrom]
                onCallback(item, this.callbackInfo[onCallbackName])
            }
            this.callbackInfo[onCallbackName] = null
        }
    }

    tryCallCallback(itemsFrom, itemsTo, items) {
        this.tryCallSingleCallback('onFocus', itemsFrom, itemsTo, items)
        this.tryCallSingleCallback('onChangeSelection', itemsFrom, itemsTo, items)
        this.tryCallSingleCallback('onDoubleClick', itemsFrom, itemsTo, items)
        this.tryCallSingleCallback('onSelect', itemsFrom, itemsTo, items)
        this.tryCallSingleCallback('onCheck', itemsFrom, itemsTo, items)
    }

    callCallbackAction(index, onCallbackName) {
        // console.log("CALLBACK", index, onCallbackName)

        const {items, itemsFromIndex, itemsToIndex} = this.state
        if (index >= itemsFromIndex && index < itemsToIndex) {  // máme data daného objektu, můžeme akci provést hned
            // console.log("   CALL", index, onCallbackName)
            const onCallback = this.props[onCallbackName]
            if (onCallback) {
                const item = items[index - itemsFromIndex]
                onCallback(item, index)
                this.callbackInfo[onCallbackName] = null
            }
        } else {    // musíme počkat, až se data načtou a pak danou akci zavolat
            this.callbackInfo[onCallbackName] = index
        }
    }

    handleClick(index, e) {
        const {items} = this.props
        var {activeIndex, selectedIndex, lastFocus} = this.state

        if (activeIndex !== index || selectedIndex !== index || lastFocus !== index) {
            const wasChanged = activeIndex !== index || selectedIndex !== index
            this.setState({
                activeIndex: index,
                lastFocus: index,
                selectedIndex: index,
            })
            if (wasChanged) {
                this.callCallbackAction(index, 'onFocus')
                this.callCallbackAction(index, 'onChangeSelection')
                this.callCallbackAction(index, 'onSelect')
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

    getSelectedIndexForUse(props, state) {
        var index

        if (typeof props.selectedIndex !== 'undefined') {
            index = props.selectedIndex
        } else if (typeof state.selectedIndex !== 'undefined') {
            index = state.selectedIndex
        } else {
            index = null
        }

        return index
    }

    getActiveIndexForUse(props, state) {
        const {itemsCount} = state

        var index

        if (itemsCount === -1) {
            return null
        }

        if (typeof props.activeIndex !== 'undefined') {
            index = props.activeIndex
        } else if (typeof state.activeIndex !== 'undefined') {
            index = state.activeIndex
        } else {
            index = null
        }

        if (index < 0) {
            index = itemsCount > 0 ? 0 : null
        } else if (index >= itemsCount) {
            index = itemsCount - 1
        }
        return index
    }

    ensureItemVisible(index) {
        this.setState({
            scrollToIndex: {index},
        })
    }

    handleKeyDown(event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
        }
    }

    focus() {
        this.refs.container.focus()
    }

    needFetch(existingFrom, existingTo, requiredFrom, requiredTo) {
        if (false
            || requiredFrom < existingFrom
            || requiredFrom >= existingTo
            || requiredTo < existingFrom
            || requiredTo >= existingTo

        ) {
            return true
        } else {
            return false
        }
    }

    tryUpdateSelectedIndex(itemsFrom, itemsTo, items) {
        const {selectedItem, itemIdAttrName} = this.props
        const {selectedIndex} = this.state
        if (selectedItem) {
            const i = indexById(items, selectedItem, itemIdAttrName)
            if (i !== null) {
                const itemIndex = itemsFrom + i
                if (selectedIndex !== itemIndex) {
                    this.setState({
                        selectedIndex: itemIndex,
                        activeIndex: itemIndex,
                    })
                }
            }
        }
    }

    callFetch(fromIndex, toIndex, tryUpdateSelectedIndex) {
        if (!this.isFetching(fromIndex, toIndex)) {
            const fetchFrom = Math.max(fromIndex - _LLB_FETCH_BOUNDARY, 0)
            const fetchTo = Math.max(toIndex + _LLB_FETCH_BOUNDARY, 0)

            // console.log("FETCH: " + fetchFrom, fetchTo)
            const id = this.fetchId++
            this.currentFetch.push({id, from: fetchFrom, to: fetchTo})
            this.props.getItems(fetchFrom, fetchTo)
                .then(data => {
                    if (tryUpdateSelectedIndex) {   // pokud původní selectedIndex položka byla v seznamu items a nyní není, zrušíme selectedIndex
                        var {selectedItem, itemIdAttrName} = this.props
                        var {selectedIndex, items} = this.state
                        if (selectedItem) {
                            const iPrev = indexById(items, selectedItem, itemIdAttrName)
                            const iNew = indexById(data.items, selectedItem, itemIdAttrName)
                            if (iPrev !== null && iNew === null) {
                                this.setState({selectedIndex: null})
                            }
                        }
                    }

                    this.tryUpdateSelectedIndex(fetchFrom, fetchTo, data.items)
                    this.tryCallCallback(fetchFrom, fetchTo, data.items)
                    if (!this.needFetch(fetchFrom, fetchTo, this.state.view.from, this.state.view.to)) {    // jen pokud daná data jsou vhodná pro aktuální view
                        this.setState({
                            itemsCount: data.count,
                            itemsFromIndex: fetchFrom,
                            itemsToIndex: fetchTo,
                            items: data.items,
                        })
                    }
                    const i = indexById(this.currentFetch, id)
                    this.currentFetch = [
                        ...this.currentFetch.slice(0, i),
                        ...this.currentFetch.slice(i + 1)
                    ]
                })
        }
    }

    handleViewChange(fromIndex, toIndex) {
        const {items, itemsFromIndex, itemsToIndex, itemsCount} = this.state

        this.setState({
            view: {from: fromIndex, to: toIndex}
        })

        if (false
            || itemsCount === -1
            || this.needFetch(itemsFromIndex, itemsToIndex, fromIndex, toIndex)
        ) {

            if (this.fetchTimer) {
                clearTimeout(this.fetchTimer)
            }
            this.fetchTimer = setTimeout(this.callFetch.bind(this, fromIndex, toIndex, false), _LLB_FETCH_DELAY);
        }
    }

    isFetching(from, to) {
        for (var a=0; a<this.currentFetch.length; a++) {
            const fetch = this.currentFetch[a]

            if (!this.needFetch(fetch.from, fetch.to, from, to)) {
                return true
            }
        }
        return false
    }

    handleDoubleClick(index) {
        this.callCallbackAction(index, 'onDoubleClick')
    }

    /**
     * Provede kompletní reload dat, nezachovává pozice atp.
     */
    reload = () => {
        this.setState({
            activeIndex: this.getActiveIndexForUse(this.props, {}),
            selectedIndex: this.getSelectedIndexForUse(this.props, {}),
            lastFocus: null,
            itemsCount: typeof this.props.itemsCount !== 'undefined' ? this.props.itemsCount : 0, // zatím počet položek neznáme
            itemsFromIndex: 0,  // od jakého indexu máme položky
            itemsToIndex: 0,    // do jakého indexu máme položky
            items: [],  // načtené položky
            view: {from: 0, to: 0},
            scrollToIndex: {index: 0},
            selectedItem: this.props.selectedItem,
        }, this.fetchNow);
    }

    fetchNow() {
        this.callFetch(0, 1, true)
    }

    handleRenderItem(index) {
        const {className, items, itemsFromIndex, itemsToIndex, activeIndex, selectedIndex} = this.state
        const {renderItemContent} = this.props;

        var data = null
        if (index >= itemsFromIndex && index < itemsToIndex) {
            data = items[index - itemsFromIndex]
        }

        const active = (index === selectedIndex)
        const focus = (index === activeIndex)

        const clsObj = {
            'listbox-item': true,
            'active': active,
            'focus': focus,
        };

        if (className) {
            clsObj[className] = true;
        }

        var cls = classNames(clsObj);

        return (
            <div
                className={cls}
                ref={'item-' + index}
                key={index}
                onMouseDown={this.handleClick.bind(this, index)}
                onDoubleClick={this.handleDoubleClick.bind(this, index)}
            >
                {renderItemContent(data, active, index)}
            </div>
        )
    }

    focus() {
        ReactDOM.findDOMNode(this.refs.mainContainer).focus()
    }
    
    render() {
        const {className, items, renderItemContent} = this.props;
        const {scrollToIndex, activeIndex, activeIndexes} = this.state;

        var cls = "lazy-listbox-container listbox-container";
        if (className) {
            cls += " " + className;
        }

        return (
            <div className={cls} ref="mainContainer" onKeyDown={this.handleKeyDown} tabIndex={0}>
                <VirtualList
                    ref="virtualList"
                    tagName='div'
                    container={this.state.mainContainer}
                    lazyItemsCount={this.state.itemsCount}
                    renderItem={this.handleRenderItem}
                    itemHeight={this.props.itemHeight}
                    onViewChange={this.handleViewChange}
                    scrollToIndex={scrollToIndex}
                    />
            </div>
        )
    }
}

LazyListBox.defaultProps = {
    renderItemContent: (item, isActive, index) => {
        return (
            <div>{item !== null ? (item.name) : ('i' + index)}</div>
        )
    },
    itemHeight: 24,
    separateFocus: true,
    itemIdAttrName: "id",
}

module.exports = LazyListBox
