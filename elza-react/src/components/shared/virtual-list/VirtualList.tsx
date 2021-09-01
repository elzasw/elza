import PropTypes from 'prop-types';
import * as React from 'react';
import {ComponentClass, FunctionComponent} from 'react';
import * as ReactDOM from 'react-dom';
import Loading from '../loading/Loading';
import utils from './utils';

const DEFAULT_ITEM_HEIGHT = 16;

interface IndexInterface {
    index: number;
}

type Props = {
    /** v případě, že máme položky na klientovi, je zde seznam všech položek */
    items?: any[];
    /** v případě, že máme položky jen na serveru, zde je počet položek */
    lazyItemsCount?: number;
    itemHeight?: number;
    /** pokud je změněn, provede se scroll na daný index - lépe použít objekt, protože při stejném indexu lze kvůli odscrolování změnit referenci na objekt */
    scrollToIndex: number | IndexInterface;
    renderItem: () => React.ReactNode;
    onViewChange?: (firstIndex: number, lastIndex: number) => void;
    container: any;
    tagName: FunctionComponent<any> | ComponentClass<any> | string;
    scrollDelay?: number;
    itemBuffer?: number;
    scrollTopPadding: number;
    fetching: boolean;
};

type State = {
    items?: any[];
    bufferStart?: number;
    height?: number;
    itemHeight?: number;
    isMounted: boolean;
    prevFirstItemIndex?: number;
};

class VirtualList extends React.Component<Props, State> {
    private onScrollDebounced: Function;
    private container: Element;

    constructor(props) {
        super(props);
        this.state = {
            ...this.getVirtualState(this.props, {prevFirstItemIndex: -1, isMounted: false}),
            isMounted: false,
            itemHeight: this.props.itemHeight || DEFAULT_ITEM_HEIGHT,
        };
    }

    static propTypes = {
        items: PropTypes.array,
        lazyItemsCount: PropTypes.number,
        itemHeight: PropTypes.number,
        scrollToIndex: PropTypes.oneOfType([PropTypes.number, PropTypes.shape({index: PropTypes.number})]),
        renderItem: PropTypes.func.isRequired,
        onViewChange: PropTypes.func,
        container: PropTypes.object.isRequired,
        tagName: PropTypes.string.isRequired,
        scrollDelay: PropTypes.number,
        itemBuffer: PropTypes.number,
        scrollTopPadding: PropTypes.number,
    };

    static defaultProps = {
        container: typeof window !== 'undefined' ? window : undefined,
        tagName: 'div',
        scrollDelay: 0,
        itemBuffer: 0,
        scrollTopPadding: 0,
        fetching: false,
    };

    static getItems = function(viewTop, viewHeight, listTop, itemHeight, itemCount, itemBuffer) {
        if (itemCount === 0 || itemHeight === 0) {
            return {
                itemsInView: 0,
            };
        }

        const listHeight = itemHeight * itemCount;

        const listBox = {
            top: listTop,
            height: listHeight,
            bottom: listTop + listHeight,
        };

        const bufferHeight = itemBuffer * itemHeight;
        viewTop -= bufferHeight;
        viewHeight += bufferHeight * 2;

        const viewBox = {
            top: viewTop,
            bottom: viewTop + viewHeight,
        };

        // list is below viewport
        if (viewBox.bottom < listBox.top) {
            return {
                //firstItemIndex: 0,
                itemsInView: 0,
                //lastItemIndex: 0
            };
        }

        // list is above viewport
        if (viewBox.top > listBox.bottom) {
            return {
                firstItemIndex: 0,
                itemsInView: 0,
                lastItemIndex: 0,
            };
        }

        const listViewBox = VirtualList.getBox(viewBox, listBox);

        const firstItemIndex = Math.max(0, Math.floor(listViewBox.top / itemHeight));
        const lastItemIndex = Math.ceil(listViewBox.bottom / itemHeight) - 1;

        const itemsInView = lastItemIndex - firstItemIndex + 1;

        const result = {
            firstItemIndex: firstItemIndex,
            lastItemIndex: lastItemIndex,
            itemsInView: itemsInView,
        };

        return result;
    };

    static getBox = function(view, list) {
        list.height = list.height || list.bottom - list.top;

        return {
            top: Math.max(0, Math.min(view.top - list.top)),
            bottom: Math.max(0, Math.min(list.height, view.bottom - list.top)),
        };
    };

    getVirtualState = (props: Props, currState: State): State => {
        // default values
        const state: State = {
            ...currState,
            items: [],
            bufferStart: 0,
            height: 0,
        };

        const lazyItems = !props.items;
        const itemsCount = lazyItems ? props.lazyItemsCount! : props.items!.length;
        let itemHeight = currState.itemHeight || -1;

        // early return if nothing to render
        if (typeof props.container === 'undefined' || itemsCount === 0 || itemHeight <= 0) {
            return state;
        }

        state.height = itemsCount * itemHeight;

        const container = props.container;

        const viewHeight =
            typeof container.innerHeight !== 'undefined' ? container.innerHeight : container.clientHeight;

        // Při změně položek ve virtuallistu je problém, že se nepřekreslí, pokud si virtual list "myslí", že je oblast pro kreslení velká, začne vše fungovat
        // console.log(container)   // doresime pozdeji
        // viewHeight = 100000;

        // no space to render
        if (viewHeight <= 0) {
            return state;
        }

        //var list = this.getDOMNode();
        const list = ReactDOM.findDOMNode(this);

        const offsetTop = utils.topDifference(list, container);

        const viewTop = typeof container.scrollY !== 'undefined' ? container.scrollY : container.scrollTop;

        const renderStats = VirtualList.getItems(
            viewTop,
            viewHeight,
            offsetTop,
            itemHeight,
            itemsCount,
            props.itemBuffer,
        );

        // no items to render
        if (renderStats.itemsInView === 0 && (!props.items || props.items.length === 0)) {
            return state;
        }

        // TODO @stanekpa tahle část vlastně nějak nemohla dávat smysl. Možná že už čekáme že tam budou data.
        if (lazyItems) {
            state.items = [];
            const firstIndex = renderStats.firstItemIndex!;
            const lastIndex = renderStats.lastItemIndex!;
            for (let a = firstIndex; a < lastIndex + 1; a++) {
                state.items.push(a);
            }
        } else {
            if (props.items!.length < 3) {
                state.items = props.items;
            }else {
                state.items = props.items!.slice(renderStats.firstItemIndex, renderStats.lastItemIndex! + 1);
            }
        }
        state.bufferStart = renderStats.firstItemIndex! * itemHeight;

        state.prevFirstItemIndex = renderStats.firstItemIndex;

        if (renderStats.firstItemIndex !== currState.prevFirstItemIndex) {
            const {onViewChange} = this.props;
            onViewChange && onViewChange(renderStats.firstItemIndex!, renderStats.lastItemIndex!);
        }

        return state;
    };

    UNSAFE_componentWillReceiveProps(nextProps) {
        const state = this.getVirtualState(nextProps, this.state);
        let itemHeight = this.state.itemHeight!;
        if (this.onScrollDebounced) {
            this.props.container.removeEventListener('scroll', this.onScrollDebounced);
        }

        this.onScrollDebounced = utils.debounce(this.onScroll, nextProps.scrollDelay, false);

        nextProps.container.addEventListener('scroll', this.onScrollDebounced);

        if (typeof nextProps.scrollToIndex !== 'undefined' && this.props.scrollToIndex !== nextProps.scrollToIndex) {
            const scrollToIndexNum =
                typeof nextProps.scrollToIndex === 'number'
                    ? nextProps.scrollToIndex
                    : nextProps.scrollToIndex !== null
                    ? nextProps.scrollToIndex.index
                    : null;

            if (scrollToIndexNum !== null) {
                this.setState(state, () => {
                    const box = this.container;
                    const itemTop = scrollToIndexNum * itemHeight + this.props.scrollTopPadding;
                    const from = this.state.bufferStart! + this.props.scrollTopPadding;
                    const boxParent = box.parentNode! as Element;
                    const to = from + boxParent.clientHeight - 2 * this.props.scrollTopPadding;

                    //console.log('itemTop', itemTop, 'from', from, 'to', to, 'itemHeight', this.props.itemHeight)
                    if (itemTop <= from) {
                        if (itemTop - itemHeight < this.props.scrollTopPadding) {
                            boxParent.scrollTop = 0;
                        } else {
                            boxParent.scrollTop = itemTop - itemHeight; // chceme alespon o jednu vice, aby nebyla vybrana moc nahore
                        }
                    } else if (itemTop + itemHeight > to) {
                        // box.parentNode.scrollTop = itemTop + this.props.itemHeight  // chceme alespon o jednu vice, aby nebyla vybrana moc dole
                        boxParent.scrollTop = itemTop - itemHeight;
                    }
                });
            }
        } else {
            this.setState(state);
        }
    }

    componentDidUpdate(prevProps) {
        if (!this.props.itemHeight) {
            this.updateItemHeightIfChanged();
        }
    }

    updateItemHeightIfChanged = () => {
        let itemNode = this.container && this.container.children[0];
        if (itemNode) {
            let itemHeight = this.getItemHeight(itemNode);
            if (itemHeight !== this.state.itemHeight) {
                let state = {
                    ...this.state,
                    itemHeight: itemHeight,
                    itemsRendered: true,
                };
                let virtState = this.getVirtualState(this.props, state);
                state = {
                    ...state,
                    ...virtState,
                };
                this.setState(state);
            }
        }
    };
    UNSAFE_componentWillMount() {
        this.onScrollDebounced = utils.debounce(this.onScroll, this.props.scrollDelay, false);
    }
    componentDidMount() {
        var state = this.getVirtualState(this.props, this.state);
        this.setState({
            ...state,
            isMounted: true,
        });

        this.props.container.addEventListener('scroll', this.onScrollDebounced);
    }
    getItemHeight = element => {
        return this.props.itemHeight || element.clientHeight;
    };
    componentWillUnmount() {
        this.props.container.removeEventListener('scroll', this.onScrollDebounced);
    }
    onScroll = () => {
        var state = this.getVirtualState(this.props, this.state);

        this.setState(state);
    };
    // in case you need to get the currently visible items
    visibleItems = () => {
        return this.state.items;
    };
    render() {
        const {tagName, fetching} = this.props;
        const Tag = tagName;
        let content;
        if (fetching || !this.state.items) {
            content = <Loading />;
        } else {
            content = this.state.items.map(this.props.renderItem);
        }
        return (
            <Tag
                className="virtual-list"
                ref={ref => this.container = ref}
                style={{boxSizing: 'border-box', height: this.state.height, paddingTop: this.state.bufferStart}}
            >
                {content}
            </Tag>
        );
    }
}

export default VirtualList;
