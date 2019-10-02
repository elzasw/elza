import React from "react";
import ReactDOM from "react-dom";
import utils from "./utils";
import Loading from "../loading/Loading";

const DEFAULT_ITEM_HEIGHT = 16;

var VirtualList = React.createClass({
    propTypes: {
        items: React.PropTypes.array,   // v případě, že máme položky na klientovi, je zde seznam všech položek
        lazyItemsCount: React.PropTypes.number,  //  v případě, že máme položky jen na serveru, zde je počet položek
        itemHeight: React.PropTypes.number,
        scrollToIndex: React.PropTypes.oneOfType([ React.PropTypes.number, React.PropTypes.shape({ index: React.PropTypes.number }) ]), // pokud je změněn, provede se scroll na daný index - lépe použít objekt, protože při stejném indexu lze kvůli odscrolování změnit referenci na objekt
        renderItem: React.PropTypes.func.isRequired,
        onViewChange: React.PropTypes.func,
        container: React.PropTypes.object.isRequired,
        tagName: React.PropTypes.string.isRequired,
        scrollDelay: React.PropTypes.number,
        itemBuffer: React.PropTypes.number
    },
    getDefaultProps: function() {
        return {
            container: typeof window !== 'undefined' ? window : undefined,
            tagName: 'div',
            scrollDelay: 0,
            itemBuffer: 0,
            scrollTopPadding: 0,
        };
    },
    getVirtualState: function(props, isMounted, currState) {    // currState - aktuální stav komponenty
        // default values
        var state = {
            items: [],
            bufferStart: 0,
            height: 0,
        };

        const lazyItems = props.items ? false : true
        const itemsCount = lazyItems ? props.lazyItemsCount : props.items.length
        let itemHeight = currState.itemHeight;
        // early return if nothing to render
        if (typeof props.container === 'undefined' || itemsCount === 0 || itemHeight <= 0 || !isMounted) return state;

        state.height = itemsCount * itemHeight;

        var container = props.container;

        var viewHeight = typeof container.innerHeight !== 'undefined' ? container.innerHeight : container.clientHeight;

        // Při změně položek ve virtuallistu je problém, že se nepřekreslí, pokud si virtual list "myslí", že je oblast pro kreslení velká, začne vše fungovat
        // console.log(container)   // doresime pozdeji
        // viewHeight = 100000;

        // no space to render
        if (viewHeight <= 0) return state;

        //var list = this.getDOMNode();
        var list = ReactDOM.findDOMNode(this)

        var offsetTop = utils.topDifference(list, container);

        var viewTop = typeof container.scrollY !== 'undefined' ? container.scrollY : container.scrollTop;

        var renderStats = VirtualList.getItems(viewTop, viewHeight, offsetTop, itemHeight, itemsCount, props.itemBuffer);

        // no items to render
        if (renderStats.itemsInView.length === 0) return state;

        if (lazyItems) {
            state.items = []
            for (var a=renderStats.firstItemIndex; a < renderStats.lastItemIndex + 1 ; a++) {
                state.items.push(a)
            }
        } else {
            state.items = props.items.slice(renderStats.firstItemIndex, renderStats.lastItemIndex + 1);
        }
        state.bufferStart = renderStats.firstItemIndex * itemHeight;

        state.prevFirstItemIndex = renderStats.firstItemIndex

        if (renderStats.firstItemIndex !== currState.prevFirstItemIndex) {
            const {onViewChange} = this.props
            onViewChange && onViewChange(renderStats.firstItemIndex, renderStats.lastItemIndex)
        }

        return state;
    },
    getInitialState: function() {
        return {
            ...this.getVirtualState(this.props, false, {prevFirstItemIndex: -1}),
            isMounted: false,
            itemHeight: this.props.itemHeight || DEFAULT_ITEM_HEIGHT
        }
    },
    shouldComponentUpdate: function(nextProps, nextState) {
        return true;
    },
    componentWillReceiveProps: function(nextProps) {
        var state = this.getVirtualState(nextProps, this.state.isMounted, this.state);
        let itemHeight = this.state.itemHeight;
        this.props.container.removeEventListener('scroll', this.onScrollDebounced);

        this.onScrollDebounced = utils.debounce(this.onScroll, nextProps.scrollDelay, false);

        nextProps.container.addEventListener('scroll', this.onScrollDebounced);

        if (typeof nextProps.scrollToIndex !== 'undefined' && this.props.scrollToIndex !== nextProps.scrollToIndex) {
            var scrollTopPadding = this.props.scrollTopPadding || 0

            const scrollToIndexNum = typeof nextProps.scrollToIndex === "number" ? nextProps.scrollToIndex : (nextProps.scrollToIndex !== null ? nextProps.scrollToIndex.index : null);

            if (scrollToIndexNum !== null) {
                this.setState(state, () => {
                    var box = this.container;
                    var itemTop = scrollToIndexNum * itemHeight + this.props.scrollTopPadding

                    var from = this.state.bufferStart + this.props.scrollTopPadding
                    var to = from + box.parentNode.clientHeight - 2*this.props.scrollTopPadding

                    //console.log('itemTop', itemTop, 'from', from, 'to', to, 'itemHeight', this.props.itemHeight)
                    if (itemTop <= from) {
                        if (itemTop - itemHeight < this.props.scrollTopPadding) {
                            box.parentNode.scrollTop = 0
                        } else {
                            box.parentNode.scrollTop = itemTop - itemHeight  // chceme alespon o jednu vice, aby nebyla vybrana moc nahore
                        }
                    } else if (itemTop + itemHeight > to) {
                        // box.parentNode.scrollTop = itemTop + this.props.itemHeight  // chceme alespon o jednu vice, aby nebyla vybrana moc dole
                        box.parentNode.scrollTop = itemTop - itemHeight
                    }
                });
            }
        } else {
            this.setState(state);
        }
    },
    componentDidUpdate: function(prevProps) {
        if(!this.props.itemHeight){
            this.updateItemHeightIfChanged();
        }
    },
    updateItemHeightIfChanged: function() {
        let itemNode = this.container && this.container.children[0];
        if(itemNode){
            let itemHeight = this.getItemHeight(itemNode);
            if(itemHeight !== this.state.itemHeight){
                let state = {
                    ...this.state,
                    itemHeight: itemHeight,
                    itemsRendered: true
                };
                let virtState = this.getVirtualState(this.props, this.state.isMounted, state);
                state = {
                    ...state,
                    ...virtState
                };
                this.setState(state);
            }
        }
    },
    componentWillMount: function() {
        this.onScrollDebounced = utils.debounce(this.onScroll, this.props.scrollDelay, false);
    },
    componentDidMount: function() {
        var state = this.getVirtualState(this.props, true, this.state);
        this.setState({
            ...state,
            isMounted: true
        })

        this.props.container.addEventListener('scroll', this.onScrollDebounced);
    },
    getItemHeight: function(element) {
        return this.props.itemHeight || element.clientHeight;
    },
    componentWillUnmount: function() {
        this.props.container.removeEventListener('scroll', this.onScrollDebounced);
    },
    onScroll: function() {
        var state = this.getVirtualState(this.props, this.state.isMounted, this.state);

        this.setState(state);
    },
    // in case you need to get the currently visible items
    visibleItems: function() {
        return this.state.items;
    },
    render: function() {
        let content;
        if (this.props.fetching && this.props.fetching === true) {
            content = <Loading />;
        } else {
            content = this.state.items.map(this.props.renderItem);
        }
        return (
            <this.props.tagName className="virtual-list" ref={(container)=>{this.container = container;}} style={{boxSizing: 'border-box', height: this.state.height, paddingTop: this.state.bufferStart}} >
                {content}
            </this.props.tagName>
        );
    }
});

VirtualList.getBox = function(view, list) {
    list.height = list.height || list.bottom - list.top;

    return {
        top: Math.max(0, Math.min(view.top - list.top)),
        bottom: Math.max(0, Math.min(list.height, view.bottom - list.top))
    };
};

VirtualList.getItems = function(viewTop, viewHeight, listTop, itemHeight, itemCount, itemBuffer) {
    if (itemCount === 0 || itemHeight === 0) return {
        itemsInView: 0
    };

    var listHeight = itemHeight * itemCount;

    var listBox = {
        top: listTop,
        height: listHeight,
        bottom: listTop + listHeight
    };

    var bufferHeight = itemBuffer * itemHeight;
    viewTop -= bufferHeight;
    viewHeight += bufferHeight * 2;

    var viewBox = {
        top: viewTop,
        bottom: viewTop + viewHeight
    };

    // list is below viewport
    if (viewBox.bottom < listBox.top) return {
        //firstItemIndex: 0,
        itemsInView: 0,
        //lastItemIndex: 0
    };

    // list is above viewport
    if (viewBox.top > listBox.bottom) return {
        firstItemIndex: 0,
        itemsInView: 0,
        lastItemIndex: 0
    };

    var listViewBox = VirtualList.getBox(viewBox, listBox);

    var firstItemIndex = Math.max(0,  Math.floor(listViewBox.top / itemHeight));
    var lastItemIndex = Math.ceil(listViewBox.bottom / itemHeight) - 1;

    var itemsInView = lastItemIndex - firstItemIndex + 1;

    var result = {
        firstItemIndex: firstItemIndex,
        lastItemIndex: lastItemIndex,
        itemsInView: itemsInView,
    };

    return result;
};

export default VirtualList;
