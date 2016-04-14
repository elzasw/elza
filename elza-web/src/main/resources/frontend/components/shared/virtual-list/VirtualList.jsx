var React = require('react');
var ReactDOM = require('react-dom');
var utils = require('./utils');

var VirtualList = React.createClass({
    propTypes: {
        items: React.PropTypes.array,   // v případě, že máme položky na klientovi, je zde seznam všech položek
        lazyItemsCount: React.PropTypes.number,  //  v případě, že máme položky jen na serveru, zde je počet položek
        itemHeight: React.PropTypes.number.isRequired,
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

        // early return if nothing to render
        if (typeof props.container === 'undefined' || itemsCount === 0 || props.itemHeight <= 0 || !isMounted) return state;
        
        state.height = itemsCount * props.itemHeight;

        var container = props.container;

        var viewHeight = typeof container.innerHeight !== 'undefined' ? container.innerHeight : container.clientHeight;
        
        // no space to render
        if (viewHeight <= 0) return state;
        
        //var list = this.getDOMNode();
        var list = ReactDOM.findDOMNode(this)

        var offsetTop = utils.topDifference(list, container);

        var viewTop = typeof container.scrollY !== 'undefined' ? container.scrollY : container.scrollTop;

        var renderStats = VirtualList.getItems(viewTop, viewHeight, offsetTop, props.itemHeight, itemsCount, props.itemBuffer);

        // no items to render
        if (renderStats.itemsInView.length === 0) return state;

        if (lazyItems) {
            state.items = []
            for (var a=renderStats.firstItemIndex; a<renderStats.lastItemIndex + 1; a++) {
                state.items.push(a)
            }
        } else {
            state.items = props.items.slice(renderStats.firstItemIndex, renderStats.lastItemIndex + 1);
        }
        state.bufferStart = renderStats.firstItemIndex * props.itemHeight;

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
        }
    },
    shouldComponentUpdate: function(nextProps, nextState) {
return true;
    },
    componentWillReceiveProps: function(nextProps) {
        var state = this.getVirtualState(nextProps, this.state.isMounted, this.state);

        this.props.container.removeEventListener('scroll', this.onScrollDebounced);

        this.onScrollDebounced = utils.debounce(this.onScroll, nextProps.scrollDelay, false);

        nextProps.container.addEventListener('scroll', this.onScrollDebounced);

        if (typeof nextProps.scrollToIndex !== 'undefined' && this.props.scrollToIndex !== nextProps.scrollToIndex) {
            var scrollTopPadding = this.props.scrollTopPadding || 0

            this.setState(state, () => {
                var box = ReactDOM.findDOMNode(this.refs.box)
                var itemTop = nextProps.scrollToIndex * this.props.itemHeight + this.props.scrollTopPadding

                var from = this.state.bufferStart + this.props.scrollTopPadding
                var to = from + box.parentNode.clientHeight - 2*this.props.scrollTopPadding

                //console.log('itemTop', itemTop, 'from', from, 'to', to, 'itemHeight', this.props.itemHeight)
                if (itemTop <= from) {
                    if (itemTop - this.props.itemHeight < this.props.scrollTopPadding) {
                        box.parentNode.scrollTop = 0
                    } else {
                        box.parentNode.scrollTop = itemTop - this.props.itemHeight  // chceme alespon o jednu vice, aby nebyla vybrana moc nahore
                    }
                } else if (itemTop + this.props.itemHeight > to) {
                    // box.parentNode.scrollTop = itemTop + this.props.itemHeight  // chceme alespon o jednu vice, aby nebyla vybrana moc dole
                    box.parentNode.scrollTop = itemTop - this.props.itemHeight
                }
            });
        } else {
            this.setState(state);
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
        return (
        <this.props.tagName ref='box' {...this.props} style={{boxSizing: 'border-box', height: this.state.height, paddingTop: this.state.bufferStart }} >
            {this.state.items.map(this.props.renderItem)}
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
        itemsInView: 0
    };
    
    // list is above viewport
    if (viewBox.top > listBox.bottom) return {
        itemsInView: 0
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

module.exports = VirtualList;