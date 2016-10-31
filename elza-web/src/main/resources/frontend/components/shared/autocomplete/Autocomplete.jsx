const React = require('react')
import ReactDOM from 'react-dom';
import {Button, Input, HelpBlock} from 'react-bootstrap';
import {Icon, AbstractReactComponent} from 'components/index.jsx';
import {getBootstrapInputComponentInfo} from 'components/form/FormUtils.jsx';
const scrollIntoView = require('dom-scroll-into-view')
require('./Autocomplete.less')
let _debugStates = false
import {propsEquals} from 'components/Utils.jsx'

/**
 * Komponenta pro text input - defoinována pro překrytí a kontrolu shouldComponentUpdate. Pokud se v autocomplete
 * dovyplní a označí zbytek textu a input se překreslil, zmizel daný text. Tato komponenta tomuz zabrání - testuje změnu value.
 */
const TextInput = class extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }
    shouldComponentUpdate(nextProps, nextState) {
        return !propsEquals(this.props, nextProps);
    }
    render() {
        return (
            <input
                {...this.props}
            />
        )
    }
}

var keyDownHandlers = {
    ArrowRight: function (event) {
        const {tree} = this.props;
        if (tree) {
            var {highlightedIndex} = this.state
            var {expandedIds} = this.state
            if (highlightedIndex !== null) {
                const items = this.getFilteredItems();
                const node = items[highlightedIndex];
                const id = this.props.getItemId(node);
                if (node.children && node.children.length > 0) {
                    if (expandedIds[id]) {  // je rozbalený, přejdeme na potomka
                        if (highlightedIndex + 1 < items.length) {
                            this._performAutoCompleteOnKeyUp = true
                            this.changeState({
                                highlightedIndex: highlightedIndex + 1,
                            })
                        }
                    } else {    // není rozbalený, rozbalíme
                        this.expandNode(node, true)
                    }
                }
            }
        }
    },
    ArrowLeft: function (event) {
        const {tree} = this.props;
        if (tree) {
            var {highlightedIndex} = this.state;
            var {expandedIds} = this.state;
            if (highlightedIndex !== null) {
                const items = this.getFilteredItems();
                const node = items[highlightedIndex];
                const id = this.props.getItemId(node);
                if (node.children && node.children.length > 0 && expandedIds[id]) { // je rozbalený, zablíme
                    this.expandNode(node, false)
                } else {    // není rozbalený, přejmede na parenta
                    const currDepth = this.state.itemsDepth[highlightedIndex];
                    var index = highlightedIndex - 1;
                    while (index >= 0 && this.state.itemsDepth[index] >= currDepth) {
                        index--;
                    }
                    if (index >= 0) {
                        this._performAutoCompleteOnKeyUp = true
                        this.changeState({
                            highlightedIndex: index,
                        })
                    }
                }
            }
        }
    },
    Home: ()=> {
    },
    End: ()=> {
    },
    Alt: ()=> {
    },
    Tab: ()=> {
    },
    ArrowDown: function (event) {
        event.preventDefault()

        if (event.altKey) {
            this.changeState({
                isOpen: true,
            }, () => {
                this.props.onSearchChange(this.state.inputStrValue)
            })
        } else {
            var { highlightedIndex } = this.state
            // var index = (highlightedIndex === null || highlightedIndex === this.getFilteredItems().length - 1) ? 0 : highlightedIndex + 1
            var index = this.getNextFocusableItem(highlightedIndex);

            this._performAutoCompleteOnKeyUp = true

            this.changeState({
                highlightedIndex: index,
            })
        }
    },

    ArrowUp: function (event) {
        event.preventDefault()

        if (event.altKey) {
            this.closeMenu();
        } else {
            var { highlightedIndex } = this.state
            // var index = (highlightedIndex === 0 || highlightedIndex === null) ? this.getFilteredItems().length - 1 : highlightedIndex - 1
            var index = this.getPrevFocusableItem(highlightedIndex);
            this._performAutoCompleteOnKeyUp = true

            this.changeState({
                highlightedIndex: index,
            })
        }
    },

    Enter: function (event) {
        if (this.state.isOpen === false) {
            // already selected this, do nothing
            return
        }
        if (this.props.tags) {
            event.stopPropagation();
            event.preventDefault();
            var id, item;
            if (this.state.highlightedIndex == null) {
                id = null;
                item = {
                    name: this.state.inputStrValue
                }
            } else {
                item = this.getFilteredItems()[this.state.highlightedIndex];
                id = this.props.getItemId(item);
            }
            if (this.props.allowSelectItem(id, item)) {
                this.changeState({
                    inputStrValue: '',
                    value: '',
                    isOpen: false,
                    highlightedIndex: null
                }, () => {
                    this.props.onChange(id, item)
                })
            }
        } else {
            if (this.state.highlightedIndex == null) {
                event.stopPropagation();
                event.preventDefault();
                // hit enter after focus but before typing anything so no autocomplete attempt yet
                this.changeState({
                    isOpen: false,
                    inputStrValue: '',
                    value: null,
                }, () => {
                    ReactDOM.findDOMNode(this.refs.input).select()
                    this.props.onChange(null, {id: null, name: ''})
                })
            } else {
                event.stopPropagation();
                event.preventDefault();

                var item = this.getFilteredItems()[this.state.highlightedIndex]

                const id = this.props.getItemId(item);
                if (this.props.allowSelectItem(id, item)) {
                    this.changeState({
                        inputStrValue: this.props.getItemName(item),
                        value: item,
                        isOpen: false,
                        highlightedIndex: null
                    }, () => {
                        //ReactDOM.findDOMNode(this.refs.input).focus() // TODO: file issue
                        ReactDOM.findDOMNode(this.refs.input).setSelectionRange(
                            this.state.inputStrValue.length,
                            this.state.inputStrValue.length
                        )
                        this.props.onChange(id, item)
                    })
                }
            }
        }
    },

    Escape: function (event) {
        this.closeMenu();
    }
}

export default class Autocomplete extends AbstractReactComponent {
    constructor(props) {
        super();

        this.bindMethods('handleKeyDown', 'handleChange', 'handleKeyUp', 'getFilteredItems', 'maybeAutoCompleteText',
            'maybeScrollItemIntoView', 'handleInputFocus', 'handleInputClick', 'handleInputBlur',
            'handleKeyDown', 'openMenu', 'closeMenu', 'handleDocumentClick', 'getStateFromProps',
            'focus')

        this._ignoreBlur = false;

        var shouldItemRender;
        if (props.shouldItemRender) {
            shouldItemRender = props.shouldItemRender;
        } else if (props.customFilter) {
            shouldItemRender = () => true;
        } else {
            shouldItemRender = (state, value) => state.name.toLowerCase().indexOf(value.toLowerCase()) !== -1
        }

        this.state = {
            ...this.getStateFromProps({}, props, {inputStrValue: ''}),
            isOpen: false,
            highlightedIndex: null,
            hasFocus: false
        }
    }

    /**
     * Získání indexu další možné položky pro focus.
     * @param index aktuální index
     * @return další možná položka nebo index, pokud jiná není
     */
    getNextFocusableItem = (index) => {
        const {allowFocusItem, getItemId} = this.props;
        const items = this.getFilteredItems();
        const start = index !== null ? index : 0;
        var ii = index != null ? start + 1 : start;
        if (ii >= items.length) {   // na konci přejdeme na začátek
            ii = 0;
        }
        while (true) {
            var item = items[ii];
            if (allowFocusItem(getItemId(item), item)) {
                return ii;
            }
            ii++;

            if (ii >= items.length) {   // na konci přejdeme na začátek
                ii = 0;
            }
            if (ii === start) { // udělali jsme celé kolečko a nenalezli položku, na kterou se může dát další focus
                return index;
            }
        }
        return null;
    }

    /**
     * Získání indexu předchozí možné položky pro focus.
     * @param index aktuální index
     * @return další možná položka nebo index, pokud jiná není
     */
    getPrevFocusableItem = (index) => {
        const {allowFocusItem, getItemId} = this.props;
        const items = this.getFilteredItems();
        const start = index !== null ? index : items.length - 1;
        var ii = index !== null ? index - 1 : start;
        if (ii < 0) {   // na konci přejdeme na začátek
            ii = items.length - 1;
        }
        while (true) {
            var item = items[ii];
            if (allowFocusItem(getItemId(item), item)) {
                return ii;
            }
            ii--;

            if (ii < 0) {   // na konci přejdeme na začátek
                ii = items.length - 1;
            }
            if (ii === start) { // udělali jsme celé kolečko a nenalezli položku, na kterou se může dát další focus
                return index;
            }
        }
        return null;
    }

    /**
     * Rozbalení nebo zabalení položky ve stromu.
     * @param node položka
     * @param expand true, pokud se má rozbalit
     */
    expandNode = (node, expand) => {
        const {expandedIds, inputStrValue, shouldItemRender} = this.state;
        const {items, customFilter, tree, getItemId} = this.props;
        const id = this.props.getItemId(node);
        let newExpandedIds;

        if (expand) {
            newExpandedIds = {...expandedIds, [id]: true};
        } else {
            newExpandedIds = {...expandedIds};
            delete newExpandedIds[id];
        }

        const newItemsInfo = this.getNewFilteredItems(items, customFilter, shouldItemRender, inputStrValue, tree, newExpandedIds, getItemId);

        this.changeState({
            expandedIds: newExpandedIds,
            items: newItemsInfo.items,
            itemsDepth: newItemsInfo.itemsDepth,
        })

    }

    /**
     * Metoda pro změnu stavu - měla by se volat místo this.setState, protože kontroluje, zda se mají položky přefiltrovat.
     * @param nextState
     * @param callback
     */
    changeState = (nextState, callback = null) => {
        if (typeof nextState.inputStrValue !== 'undefined' && nextState.inputStrValue !== this.state.inputStrValue) {   // chce změnit vstupní řetězec, musíme přefiltrovat
            const newItemsInfo = this.getNewFilteredItems(this.props.items, this.props.customFilter, this.state.shouldItemRender, nextState.inputStrValue, this.props.tree, this.state.expandedIds, this.props.getItemId);
            nextState.items = newItemsInfo.items;
            nextState.itemsDepth = newItemsInfo.itemsDepth;
        }

        this.setState(nextState, callback);
    }

    focus() {
        ReactDOM.findDOMNode(this.refs.input).focus()
    }

    isUnderEl(parentEl, el) {
        while (el !== null) {
            if (el === parentEl) {
                return true;
            }
            el = el.parentNode;
        }
        return false;
    }

    componentWillReceiveProps(nextProps) {
        this._performAutoCompleteOnUpdate = true;
        const newState = this.getStateFromProps(this.props, nextProps, this.state);
        // console.log("SSSS", this.state, newState)
        this.setState(newState);
    }

    getStateFromProps(props, nextProps, state) {
        var shouldItemRender;
        if (nextProps.shouldItemRender) {
            shouldItemRender = nextProps.shouldItemRender;
        } else if (nextProps.customFilter) {
            shouldItemRender = () => true;
        } else {
            shouldItemRender = (state, value) => {
                return state.name.toLowerCase().indexOf(value.toLowerCase()) !== -1
            }
        }

        var inputStrValue;
        var prevId = props.getItemId ? props.getItemId(props.value) : nextProps.getItemId(props.value)
        var newId = nextProps.getItemId(nextProps.value)
        _debugStates && console.log("getStateFromProps", "prevId", prevId, "newId", newId, "state", state);
        if (prevId != newId) {
            inputStrValue = nextProps.getItemName(nextProps.value)
            if (typeof inputStrValue === 'undefined') {
                inputStrValue = ''
            }
        } else {
            inputStrValue = state.inputStrValue;
        }

        var result = {
            shouldItemRender: shouldItemRender,
            value: nextProps.value,
            inputStrValue: inputStrValue,
        }

        // ---
        // Sesbírání vstupních expanded, provedení filtru položek, případně u stromu na flat a získání depth položek ve stromu
        if (props.items !== nextProps.items || result.inputStrValue !== state.inputStrValue) {
            const newItemsInfo = this.getNewFilteredItems(nextProps.items, nextProps.customFilter, result.shouldItemRender, result.inputStrValue, nextProps.tree, null, nextProps.getItemId);
            result.items = newItemsInfo.items;
            result.itemsDepth = newItemsInfo.itemsDepth;
            const expandedIds = {}; // mapa id na true, pokud je položky rozbalená
            newItemsInfo.items.forEach(item => {  // vždy se inicializuje při změně vstupu, ten říká, co je rozbalené a co ne
                if (item.expanded) {
                    expandedIds[nextProps.getItemId(item)] = true;
                }
            })
            result.expandedIds = expandedIds;
        }
        // ---

        _debugStates && console.log("getStateFromProps", result);

        return result;
    }

    handleDocumentClick(e) {
        _debugStates && console.log("STATE has focus", this.state.hasFocus, "ignore blur", this._ignoreBlur);
        var el1 = ReactDOM.findDOMNode(this.refs.input);
        var el2 = ReactDOM.findDOMNode(this.refs.menuParent);
        var el3 = ReactDOM.findDOMNode(this.refs.openClose);
        var el = e.target;
        var inside = false;
        while (el !== null) {
            if (el === el1 || el === el2 || el === el3) {
                inside = true;
                break;
            }
            el = el.parentNode;
        }
        _debugStates && console.log("@CLICK:", inside);
        if (!inside) {
            var el = ReactDOM.findDOMNode(this.refs.input);
            if (this.state.hasFocus && document.activeElement !== el) {   // víme, že má focus, ale nemá focus vlastní input, budeme simulovat blur
                this._ignoreBlur = true;
                el.focus();
                this._ignoreBlur = false;
                el.blur();
            }
            this._ignoreBlur = false;
            this.closeMenu();
        } else if (this.state.hasFocus && (this.isUnderEl(el2, e.target) || this.isUnderEl(el3, e.target))) {
            this._ignoreBlur = true;
        } else {
            this._ignoreBlur = false;
        }
    }

    componentDidMount() {
        //document.addEventListener("click", this.handleDocumentClick, false)
        document.addEventListener("mousedown", this.handleDocumentClick, false)
    }

    componentWillUnmount() {
        //document.removeEventListener("click", this.handleDocumentClick, false)
        document.removeEventListener("mousedown", this.handleDocumentClick, false)
    }

    renderMenuContainer(items, value, style) {
        var cls = 'autocomplete-menu-container';

        var header;
        if (this.props.header) {
            cls += ' has-header';

            header = (
                <div className='autocomplete-menu-header'>
                    {this.props.header}
                </div>
            )
        }

        var footer;
        if (this.props.footer) {
            cls += ' has-footer';

            footer = (
                <div className='autocomplete-menu-footer'>
                    {this.props.footer}
                </div>
            )
        }

        return (
            <div ref='menuParent' className={cls} style={style}>
                {header}
                <div className='autocomplete-menu-wrapper'>
                    <div ref='menu' className='autocomplete-menu'>
                        {items}
                    </div>
                </div>
                {footer}
            </div>
        )
    }

    componentWillMount() {
        this._ignoreBlur = false
        this._performAutoCompleteOnUpdate = false
        this._performAutoCompleteOnKeyUp = false
    }

    componentDidUpdate(prevProps, prevState) {
        if (this.state.isOpen === true && prevState.isOpen === false) {
            this.setMenuPositions()
        }

        if (this.state.isOpen && this._performAutoCompleteOnUpdate) {
            this._performAutoCompleteOnUpdate = false
            //this.maybeAutoCompleteText()
        }

        this.maybeScrollItemIntoView()
    }

    maybeScrollItemIntoView() {
        if (this.state.isOpen === true && this.state.highlightedIndex !== null) {
            var itemNode = ReactDOM.findDOMNode(this.refs[`item-${this.state.highlightedIndex}`])
            var menuNode = ReactDOM.findDOMNode(this.refs.menu).parentNode
            scrollIntoView(itemNode, menuNode, {onlyScrollIfNeeded: true})
        }
    }

    handleKeyDown(event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
        } else {
            this.changeState({
                highlightedIndex: null,
                isOpen: true
            })
        }
    }

    handleChange(event) {
        this._performAutoCompleteOnKeyUp = true
        this.changeState({
            inputStrValue: event.target.value,
        }, () => {
            this.props.onSearchChange(this.state.inputStrValue)
        })
    }

    handleKeyUp(e) {
        if (this._performAutoCompleteOnKeyUp) {
            this._performAutoCompleteOnKeyUp = false
            this.maybeAutoCompleteText()
        }
        if (this.props.onKeyUp) {
            this.props.onKeyUp(e);
        }
    }

    getFilteredItems() {
        return this.state.items;
    }

    /**
     * Provede novou filtraci položek, převede případný stromu na plochý seznam a načte hloubku jednotlivých položek ve stromu.
     * Pokud je nastaven customFilter, neprovádí se filtrování, jinak se provádí a k tomu se využívá shouldItemRender a inputStrValue.
     * @param items seznam položek
     * @param customFilter jsou položky filtrovány externě?
     * @param shouldItemRender metoda, která vrací informaci, zda se má položka renderovat v případě ne customFilter
     * @param inputStrValue zadaný vyhledávací výraz
     * @param tree jedná se stromovou komponentu?
     * @param expandedIds mapa aktuálně rozbalených id
     * @param getItemId metoda pro načtení id z položky
     * @return v případě stromu vrací: { items: [], itemsDepth: []}, jinak vrací { items: [] }
     */
    getNewFilteredItems = (items, customFilter, shouldItemRender, inputStrValue, tree, expandedIds, getItemId) => {
        // Spploštění stromu, pokud je potřeba
        var result;
        if (tree) {
            const flatTree = this.getFlatTree(items, expandedIds, getItemId);
            result = {
                items: flatTree.list,
                itemsDepth: flatTree.depthList
            }
        } else {
            result = {
                items
            };
        }

        // Jendoduchý filtr
        if (!customFilter && shouldItemRender) {
            var filteredItems = [];
            var filteredItemsDepth = [];
            result.items.forEach((item, index) => {
                if (shouldItemRender(item, inputStrValue || "")) {
                    filteredItems.push(item);
                    tree && filteredItemsDepth.push(result.itemsDepth[index]);
                }
            });
            result.items = filteredItems;
            result.itemsDepth = filteredItemsDepth;
        }

        return result
    }

    maybeAutoCompleteText() {
        if (this.props.customFilter) {
            return
        }

        if (this.state.inputStrValue === '') {
            return
        }

        var { highlightedIndex } = this.state
        var items = this.getFilteredItems()

        if (items.length === 0) {
            return
        }

        var matchedItem = highlightedIndex !== null ? items[highlightedIndex] : items[0]
        var itemValue = this.props.getItemName(matchedItem)
        var itemValueDoesMatch = this.state.inputStrValue && (itemValue.toLowerCase().indexOf(this.state.inputStrValue.toLowerCase()) === 0)

        if (itemValueDoesMatch) {
            var node = ReactDOM.findDOMNode(this.refs.input)
            var setSelection = () => {
                if (node.createTextRange) {
                    // TODO: IE a Edge špatně vybírá text
                } else {
                    node.value = itemValue
                    node.setSelectionRange(this.state.inputStrValue.length, itemValue.length)
                }
            }
            if (highlightedIndex === null) {
                this.changeState({highlightedIndex: 0}, setSelection)
            } else {
                setSelection()
            }
        }
    }

    setMenuPositions() {
        var node = ReactDOM.findDOMNode(this.refs.input)
        var rect = node.getBoundingClientRect()
        var computedStyle = getComputedStyle(node)
        var marginBottom = parseInt(computedStyle.marginBottom, 10)
        var marginLeft = parseInt(computedStyle.marginLeft, 10)
        var marginRight = parseInt(computedStyle.marginRight, 10)
        this.changeState({
            menuTop: rect.bottom + marginBottom,
            menuLeft: rect.left + marginLeft,
            menuWidth: rect.width + marginLeft + marginRight
        })
    }

    highlightItemFromMouse(index) {
        this.changeState({highlightedIndex: index})
    }

    selectItemFromMouse(item) {
        const {getItemId, allowSelectItem, allowFocusItem} = this.props;
        const id = getItemId(item);
        const allowSelect = allowSelectItem(id, item);
        const allowFocus = allowFocusItem(id, item);
        if (allowSelect && allowFocus) {
            if (this.props.tags) {
                this.changeState({
                    inputStrValue: '',
                    value: '',
                    isOpen: false,
                    highlightedIndex: null
                }, () => {
                    this.props.onChange(this.props.getItemId(item), item);
                })
            } else {
                this.changeState({
                    inputStrValue: this.props.getItemName(item),
                    value: item,
                    isOpen: false,
                    highlightedIndex: null
                }, () => {
                    this.props.onChange(this.props.getItemId(item), item)
                    ReactDOM.findDOMNode(this.refs.input).focus()
                    this.setIgnoreBlur(false)
                })
            }
        }
    }

    setIgnoreBlur(ignore) {
        this._ignoreBlur = ignore
    }

    getFlatTree = (rows, expandedIds, getItemId) => {
        var prop = {
            index: 0,
            list: [],
            depthList: [],
        };
        rows.forEach(node => this._getFlatTree(node, prop, expandedIds, getItemId, 0));
        return prop;
    }

    _getFlatTree = (node, prop, expandedIds, getItemId, depth) => {
        prop.list.push(node);
        prop.depthList.push(depth);
        if (expandedIds ? expandedIds[getItemId(node)] : node.expanded) {
            node.children.forEach(ch => this._getFlatTree(ch, prop, expandedIds, getItemId, depth + 1))
        }
    }

    handleExpandCollapse = (node, index, e) => {
        e.stopPropagation();
        e.preventDefault();

        const {expandedIds} = this.state;
        const {getItemId} = this.props;

        const id = getItemId(node);
        const expanded = expandedIds[id];

        this.expandNode(node, expanded ? false : true);
    }

    renderMenu() {
        const {tree, allowSelectItem, allowFocusItem} = this.props;

        var items = this.getFilteredItems().map((item, index) => {
            const id = this.props.getItemId(item);
            const allowSelect = allowSelectItem(id, item);
            const allowFocus = allowFocusItem(id, item);
            const treeInfo = tree ? {
                expanded: this.state.expandedIds[id],
                depth: this.state.itemsDepth[index],
                onExpandCollapse: (e) => this.handleExpandCollapse(item, index, e)
            } : null;
            var element = this.props.renderItem(
                item,
                this.state.highlightedIndex === index,
                this.state.value && this.props.getItemId(this.state.value) === id,
                allowSelect,
                allowFocus,
                treeInfo
            );
            return React.cloneElement(element, {
                onMouseDown: () => this.setIgnoreBlur(true),
                onMouseEnter: () => this.highlightItemFromMouse(index),
                onClick: () => this.selectItemFromMouse(item),
                ref: `item-${index}`,
            })
        })
        var style = {
            left: this.state.menuLeft,
            top: this.state.menuTop,
            _minWidth: this.state.menuWidth,
        }
        var menu = this.renderMenuContainer(items, this.state.value, style)
        return React.cloneElement(menu)
    }

    handleInputBlur() {
        _debugStates && console.log('...handleInputBlur', 'state.hasFocus', this.state.hasFocus, '_ignoreBlur', this._ignoreBlur);

        if (!this._ignoreBlur) {
            this.changeState({hasFocus: false})
            this.closeMenu(true);
            //this.props.onBlur && this.props.onBlur();
        } else {
            this._ignoreBlur = false;
        }


        return true;
    }

    handleInputFocus() {
        _debugStates && console.log('...handleInputFocus', 'state.hasFocus', this.state.hasFocus, '_ignoreBlur', this._ignoreBlur);

        if (this.state.hasFocus) {
            return;
        }

        this.changeState({hasFocus: true})

        if (!this._ignoreBlur) {
            this.props.onFocus && this.props.onFocus();
        } else {
            this._ignoreBlur = false;
        }
        return true;
        if (this._ignoreBlur) {
            return
        }
        this.changeState({isOpen: true})
    }

    openMenu() {
        if (this.state.isOpen) {
            return
        }

        this.changeState({isOpen: true}, () => {
            this.props.onSearchChange(this.state.inputStrValue)
            ReactDOM.findDOMNode(this.refs.input).select()
        })
    }

    closeMenu(callBlurAfterSetState = false) {
        if (!this.state.isOpen && this.state.highlightedIndex === null && this.state.inputStrValue === this.props.getItemName(this.state.value)) {
            // Není třeba nastavovat state
            if (callBlurAfterSetState) {
                this.props.onBlur && this.props.onBlur(this.state.value);
            }
            return;
        }

        var addState = {
            isOpen: false,
            highlightedIndex: null,
            inputStrValue: this.props.getItemName(this.state.value)
        }
        _debugStates && console.log("#### closeMenu", "prev state", this.state, "props", this.props, "state change", addState);
        this.changeState(addState, () => {
            //ReactDOM.findDOMNode(this.refs.input).select()
            if (callBlurAfterSetState) {
                this.props.onBlur && this.props.onBlur(this.state.value);
            }
        })
    }

    handleInputClick() {
        /*if (this.state.isOpen === false) {
         this.changeState({ isOpen: true })
         }*/
    }

    render() {
        const {error, title, touched, inline} = this.props;

        const hasError = touched && error;
        let inlineProps = {};
        if (inline) {
            error && (inlineProps.title = title ? title + "/" + error : error);
        }

        var clsMain = 'autocomplete-control-container'
        if (this.props.className) {
            clsMain += ' ' + this.props.className;
        }

        var glyph = this.state.isOpen ? 'fa-angle-up' : 'fa-angle-down';
        _debugStates && console.log("RENDER", "props", this.props, "state", this.state);

        var bootInfo = getBootstrapInputComponentInfo(this.props);
        var cls = bootInfo.cls;

        return (
            <div className={clsMain}>
                <div className='autocomplete-control-box'>
                    <div className={cls}>
                        {this.props.label && <label className='control-label'>{this.props.label}</label>}
                        <div key="inputWrapper" className={'autocomplete-input-container form-group' + (hasError ? " has-error" : "")}>
                            <TextInput
                                key="input"
                                className='form-control'
                                type='text'
                                {...inlineProps}
                                {...this.props.inputProps}
                                label={this.props.label}
                                disabled={this.props.disabled}
                                role='combobox'
                                aria-autocomplete="both"
                                ref="input"
                                onFocus={this.handleInputFocus}
                                onBlur={this.handleInputBlur}
                                onChange={this.handleChange}
                                onKeyDown={this.handleKeyDown}
                                onKeyUp={this.handleKeyUp}
                                onClick={this.handleInputClick}
                                value={this.state.inputStrValue}
                            />
                            <div disabled={this.props.disabled} ref='openClose'
                                 className={this.state.isOpen ? 'btn btn-default opened' : 'btn btn-default closed'}
                                 onClick={()=>{this.state.isOpen ? this.closeMenu() : this.openMenu()}}><Icon
                                glyph={glyph}/></div>
                            {this.props.actions}
                            {!inline && hasError && <HelpBlock>{error}</HelpBlock>}
                        </div>
                        {this.state.isOpen && this.renderMenu()}
                        {this.props.hasFeedback &&
                        <span className={'glyphicon form-control-feedback glyphicon-' + bootInfo.feedbackIcon}></span>}
                        {this.props.help && <span className='help-block'>{this.props.help}</span>}
                    </div>
                </div>
            </div>
        )
    }
}

Autocomplete.defaultProps = {
    inputProps: {},
    onSearchChange (text) {
    },
    onChange (value, item) {
    },
    allowSelectItem (id, item) { // vrati true, když může být daná položka vybrána
        return true;
    },
    allowFocusItem (id, item) { // vrati true, když může na danou položku najet klávesnicí nebo myší a může být focusovatelná
        return true;
    },
    getItemId: (item) => item ? item.id : null,
    getItemName: (item) => item ? item.name : '',
    renderItem: (item, isHighlighted, isSelected, allowSelect = true, allowFocus = true, treeInfo = null /*{expanded, depth, onExpandCollapse}*/) => {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }
        if (!allowSelect) {
            cls += ' not-selectable';
        }
        if (!allowFocus) {
            cls += ' not-focusable';
        }
        if (treeInfo !== null) {
            cls += " depth-" + treeInfo.depth;
        }
        if (item.className) {
            cls += " " + item.className;
        }

        var itemStr;
        if (item.name && item.name.length > 0) {
            itemStr = <span className="item-text">{item.name}</span>;
        } else {
            itemStr = <span className="item-text">&nbsp;</span>;
        }

        let treeTogle;
        if (treeInfo) {
            if (item.children && item.children.length > 0) {
                treeTogle = (
                    <div
                        className={`node-expand-collapse ${treeInfo.expanded ? 'expanded' : 'collapsed'}`}
                        onClick={treeInfo.onExpandCollapse}
                    >
                        <Icon glyph={treeInfo.expanded ? "fa-minus-square-o" : "fa-plus-square-o"}/>
                    </div>
                )
            } else {
                treeTogle = <div className={`node-expand-collapse`}>
                </div>
            }
        }

        return (
            <div
                className={cls}
                key={item.id}
            >
                {treeTogle}
                {itemStr}
            </div>
        )
    },
    inline: false,
    error: null,
    touched: false,
    tree: false,
}
Autocomplete.propTypes = {
    initialValue: React.PropTypes.any,
    onFocus: React.PropTypes.func,
    onBlur: React.PropTypes.func,
    onSearchChange: React.PropTypes.func,
    onChange: React.PropTypes.func,
    onKeyUp: React.PropTypes.func,
    shouldItemRender: React.PropTypes.func,
    renderItem: React.PropTypes.func,
    inputProps: React.PropTypes.object,
    actions: React.PropTypes.array,
    tags: React.PropTypes.bool,
    inline: React.PropTypes.bool,
    touched: React.PropTypes.bool,
    error: React.PropTypes.string,
    allowSelectItem: React.PropTypes.func,  // vrací true, pokud je možné řádek vybrat jako hodnotu
    allowFocusItem: React.PropTypes.func,   // vrací true, pokud se na řádek d8 najet focusem, např. přes klávesnici
    tree: React.PropTypes.bool, // jedná se o stromovou reprezentaci dat?
}
