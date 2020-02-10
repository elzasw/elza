var React = require('react');
var utils = require('./utils');

var AccordionComponent = React.createClass({
    propTypes: {
        items: React.PropTypes.array.isRequired,
        itemCloseHeight: React.PropTypes.number.isRequired,
        itemOpenHeight: React.PropTypes.number.isRequired,
        renderItem: React.PropTypes.func.isRequired,
        container: React.PropTypes.object.isRequired,
        tagName: React.PropTypes.string.isRequired,
        scrollDelay: React.PropTypes.number,
        itemBuffer: React.PropTypes.number,
        selectedId: React.PropTypes.number
    },
    getDefaultProps: function() {
        return {
            container: typeof window !== 'undefined' ? window : undefined,
            tagName: 'div',
            scrollDelay: 0,
            scrollTo: -1,
            itemBuffer: 0
        };
    },
    getVirtualState: function(props) {
        // default values
        var state = {
            items: [],
            bufferStart: 0,
            height: 0
        };

        // early return if nothing to render
        if (typeof props.container === 'undefined' || props.items.length === 0 || props.itemCloseHeight <= 0 || props.itemOpenHeight <= 0 || !this.isMounted()) return state;

        var items = props.items;

        state.height = props.items.length * props.itemCloseHeight;

        if (props.selectedId != null) {
            state.height = state.height - props.itemCloseHeight;
        }

        var container = props.container;

        var viewHeight = typeof container.innerHeight !== 'undefined' ? container.innerHeight : container.clientHeight;

        // no space to render
        if (viewHeight <= 0) return state;

        var list = this.getDOMNode();

        var offsetTop = utils.topDifference(list, container);

        var viewTop = typeof container.scrollY !== 'undefined' ? container.scrollY : container.scrollTop;

        var renderStats = AccordionComponent.getItems(viewTop, viewHeight, offsetTop, props.itemCloseHeight, props.itemOpenHeight, props.selectedId, items.length, props.itemBuffer);

        // no items to render
        if (renderStats.itemsInView.length === 0) return state;

        state.items = items.slice(renderStats.firstItemIndex, renderStats.lastItemIndex + 1);
        state.bufferStart = renderStats.firstItemIndex * props.itemCloseHeight;

        return state;
    },
    getInitialState: function() {
        return this.getVirtualState(this.props);
    },
    shouldComponentUpdate: function(nextProps, nextState) {
        return true;
    },

    componentDidUpdate: function() {
        console.log("xxx", this.state.scrollTo);

        if (this.state.scrollTo > 0) {
            React.findDOMNode(this.props.container).scrollTop = this.state.scrollTo;
            var state = this.state;
            state.scrollTo = -1;
            this.setState(state);
        }
    },

    componentWillReceiveProps: function(nextProps) {
        var state = this.getVirtualState(nextProps);
        state.scrollTo = this.props.nextProps;

        this.props.container.removeEventListener('scroll', this.onScrollDebounced);

        this.onScrollDebounced = utils.debounce(this.onScroll, nextProps.scrollDelay, false);

        nextProps.container.addEventListener('scroll', this.onScrollDebounced);

        this.setState(state);
    },
    componentWillMount: function() {
        this.onScrollDebounced = utils.debounce(this.onScroll, this.props.scrollDelay, false);
    },
    componentDidMount: function() {
        var state = this.getVirtualState(this.props);
        state.scrollTo = this.props.scrollTo;

        this.setState(state);

        this.props.container.addEventListener('scroll', this.onScrollDebounced);
    },
    componentWillUnmount: function() {
        this.props.container.removeEventListener('scroll', this.onScrollDebounced);
    },
    onScroll: function() {
        var state = this.getVirtualState(this.props);

        this.setState(state);
    },
    // in case you need to get the currently visible items
    visibleItems: function() {
        return this.state.items;
    },
    render: function() {
        return (
                <this.props.tagName {...this.props} style={{boxSizing: 'border-box', height: this.state.height, paddingTop: this.state.bufferStart }} >
                    {this.state.items.map(this.props.renderItem)}
                </this.props.tagName>
        );
    }
});

AccordionComponent.getBox = function(view, list) {
    list.height = list.height || list.bottom - list.top;

    return {
        top: Math.max(0, Math.min(view.top - list.top)),
        bottom: Math.max(0, Math.min(list.height, view.bottom - list.top))
    };
};

AccordionComponent.getItems = function(viewTop, viewHeight, listTop, itemCloseHeight, itemOpenHeight, selectedId, itemCount, itemBuffer) {
    if (itemCount === 0 || itemCloseHeight === 0 || itemOpenHeight === 0) return {
        itemsInView: 0
    };

    var listHeight = itemCloseHeight * itemCount;

    var listBox = {
        top: listTop,
        height: listHeight,
        bottom: listTop + listHeight
    };

    var bufferHeight = itemBuffer * itemCloseHeight;

    if (selectedId != null) {
        bufferHeight = bufferHeight - itemCloseHeight + itemOpenHeight;
    }

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

    var listViewBox = AccordionComponent.getBox(viewBox, listBox);

    var firstItemIndex = Math.max(0,  Math.floor(listViewBox.top / itemCloseHeight));
    var lastItemIndex = Math.ceil(listViewBox.bottom / itemCloseHeight) - 1;

    var itemsInView = lastItemIndex - firstItemIndex + 1;

    var result = {
        firstItemIndex: firstItemIndex,
        lastItemIndex: lastItemIndex,
        itemsInView: itemsInView,
    };

    return result;
};

export default AccordionComponent;
