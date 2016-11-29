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

            if (highlightedIndex != null) {
                event.preventDefault(); // u stromu nechceme posouvat po inputu, pokud je highlightedIndex = má focus list s položkami a klávesy fungují na rozbalení a zabalení stromu
                event.stopPropagation();
            }

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

            if (highlightedIndex != null) {
                event.preventDefault(); // u stromu nechceme posouvat po inputu, pokud je highlightedIndex = má focus list s položkami a klávesy fungují na rozbalení a zabalení stromu
                event.stopPropagation();
            }

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
        event.preventDefault();
        event.stopPropagation();

        if (event.altKey) {
            this.changeState({
                isOpen: true,
            }, () => {
                this.props.onSearchChange(this.state.inputStrValue)
            })
        } else {
            var {highlightedIndex} = this.state;
            const {tree} = this.props;

            var index = this.getNextFocusableItem(highlightedIndex, !tree);

            if (index !== null) {   // je pokud je nějaká vybraná položka, zkusíme doplnit autocomplete text
                this._performAutoCompleteOnKeyUp = true;
            }

            this.changeState({
                highlightedIndex: index,
            })
        }
    },

    ArrowUp: function (event) {
        event.preventDefault();
        event.stopPropagation();

        if (event.altKey) {
            this.closeMenu();
        } else {
            var {highlightedIndex} = this.state;
            const {tree} = this.props;

            var index = this.getPrevFocusableItem(highlightedIndex, !tree);

            if (index !== null) {   // je pokud je nějaká vybraná položka, zkusíme doplnit autocomplete text
                this._performAutoCompleteOnKeyUp = true;
            }

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
                    this.props.onChange(item)
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
                    this.props.onChange(null)
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
                        highlightedIndex: null,
                        changed:false
                    }, () => {
                        //ReactDOM.findDOMNode(this.refs.input).focus() // TODO: file issue
                        ReactDOM.findDOMNode(this.refs.input).setSelectionRange(
                            this.state.inputStrValue.length,
                            this.state.inputStrValue.length
                        )
                        this.props.onChange(item)
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
            hasFocus: false,
            changed:false
        }
    }

    /**
     * Získání indexu další možné položky pro focus.
     * @param index aktuální index
     * @param loop pokud je true, focus na položkách cykluje
     * @return další možná položka nebo index, pokud jiná není
     */
    getNextFocusableItem = (index, loop) => {
        const {allowFocusItem, getItemId} = this.props;
        const items = this.getFilteredItems();
        const start = index !== null ? index : 0;
        var ii = index != null ? start + 1 : start;
        if (ii >= items.length) {   // na konci přejdeme na začátek
            if (!loop) return null;

            ii = 0;
        }
        while (true) {
            var item = items[ii];
            if (allowFocusItem(getItemId(item), item)) {
                return ii;
            }
            ii++;

            if (ii >= items.length) {   // na konci přejdeme na začátek
                if (!loop) return null;

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
     * @param loop pokud je true, focus na položkách cykluje
     * @return další možná položka nebo index, pokud jiná není
     */
    getPrevFocusableItem = (index, loop) => {
        const {allowFocusItem, getItemId} = this.props;
        const items = this.getFilteredItems();
        const start = index !== null ? index : items.length - 1;
        var ii = index !== null ? index - 1 : start;
        if (ii < 0) {   // na konci přejdeme na začátek
            if (!loop) return null;

            ii = items.length - 1;
        }
        while (true) {
            var item = items[ii];
            if (allowFocusItem(getItemId(item), item)) {
                return ii;
            }
            ii--;

            if (ii < 0) {   // na konci přejdeme na začátek
                if (!loop) return null;

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
        const {alwaysExpanded, items, customFilter, tree, getItemId, allowSelectItem, allowFocusItem} = this.props;
        const id = this.props.getItemId(node);
        let newExpandedIds;

        if (expand) {
            newExpandedIds = {...expandedIds, [id]: true};
        } else {
            newExpandedIds = {...expandedIds};
            delete newExpandedIds[id];
        }

        const newItemsInfo = this.getNewFilteredItems(items, customFilter, shouldItemRender, inputStrValue, tree, newExpandedIds, getItemId, allowSelectItem, allowFocusItem, false, alwaysExpanded);

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
            const newItemsInfo = this.getNewFilteredItems(this.props.items, this.props.customFilter, this.state.shouldItemRender, nextState.inputStrValue, this.props.tree, this.state.expandedIds, this.props.getItemId, this.props.allowSelectItem, this.props.allowFocusItem, true, this.props.alwaysExpanded);
            nextState.items = newItemsInfo.items;
            nextState.itemsDepth = newItemsInfo.itemsDepth;
            if (!this.props.customFilter) {
                nextState.expandedIds = newItemsInfo.newExpandedIds;
            }
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
        var changed = state.changed;
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
            // Změna stavu, při které byla vybrána jiná položka.
            changed = false;
            inputStrValue = nextProps.getItemName(nextProps.value);
            if (typeof inputStrValue === 'undefined') {
                inputStrValue = ''
            }
        } else if(prevId === newId && !state.changed) {
            /* pokud došlo ke změně stavu, při které zůstala vybrána stejná položka
               a nebyl změněn vyhledávaný text ručně.*/
            inputStrValue = nextProps.getItemName(nextProps.value);
        } else {
            inputStrValue = state.inputStrValue;
        }

        var result = {
            shouldItemRender: shouldItemRender,
            value: nextProps.value,
            inputStrValue: inputStrValue,
            changed: changed
        }
        // ---
        // Sesbírání vstupních expanded, provedení filtru položek, případně u stromu na flat a získání depth položek ve stromu
        if ((props.items !== nextProps.items) || (prevId !== newId) || (prevId === newId && result.inputStrValue !== state.inputStrValue)) {
            // console.log(999999999, this.state, result, props, nextProps)
            // console.log(999999999, result.inputStrValue, props.items !== nextProps.items, prevId !== newId, result.inputStrValue !== state.inputStrValue)
            const newItemsInfo = this.getNewFilteredItems(nextProps.items, nextProps.customFilter, result.shouldItemRender, result.inputStrValue, nextProps.tree, null, nextProps.getItemId, nextProps.allowSelectItem, nextProps.allowFocusItem, true, nextProps.alwaysExpanded);
            result.items = newItemsInfo.items;
            result.itemsDepth = newItemsInfo.itemsDepth;
            if (nextProps.customFilter) {
                const expandedIds = {}; // mapa id na true, pokud je položky rozbalená
                newItemsInfo.items.forEach(item => {  // vždy se inicializuje při změně vstupu, ten říká, co je rozbalené a co ne
                    if (item.expanded) {
                        expandedIds[nextProps.getItemId(item)] = true;
                    }
                })
                result.expandedIds = expandedIds;
            } else {
                result.expandedIds = newItemsInfo.newExpandedIds;
            }
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

    renderMenuContainer(items, value) {
        const {highlightedIndex} = this.state;

        var cls = 'autocomplete-menu-container';
        if (highlightedIndex != null) {
            cls += " active";
        }

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
            <div ref='menuParent' className={cls}>
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
        this.setMenuPositions();

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
            inputStrValue: event.target.value, changed:true
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
     * Sestavuje strom pro daný node a jeho podstrom.
     * @param node node
     * @param lowerFilterText filtr
     * @param newExpandedIds mapa id na true, do kterého se přidají aktuálně rozbalené položky - ty co mají pod sebou ty, co odpovídají filtru
     * @param getItemId funkce pro načtení id z node
     * @param shouldItemRender funkce pro filtr
     * @param allowSelectItem funkce, která vrací true, pokud lze položku vybrat
     * @param allowFocusItem funkce, která vrací true, pokud lze na dát focus - ostatní jsou např. informační atp.
     * @param alwaysExpanded pokud je true, budou node označené jako expanded
     * @return {*}
     */
    getFilteredTreeNode = (node, lowerFilterText, newExpandedIds, getItemId, shouldItemRender, allowSelectItem, allowFocusItem, alwaysExpanded) => {
        // Podřízené nody
        const nodeChildren = [];
        node.children && node.children.forEach(subNode => {
            const newSubNode = this.getFilteredTreeNode(subNode, lowerFilterText, newExpandedIds, getItemId, shouldItemRender, allowSelectItem, allowFocusItem, alwaysExpanded);
            if (newSubNode) {
                nodeChildren.push(newSubNode);
            }
        });

        if (nodeChildren.length === 0 && nodeChildren.length === 0) {   // nemá potomky, chceme ho jen v případě, že je sám slectable a vyhovuje hledanému výrazu nebo že je allowFocusItem na false - ty dáváme vždy
            const id = getItemId(node);
            const selectable = allowSelectItem(id, node);
            const focusable = allowFocusItem(id, node);
            const show = shouldItemRender(node, lowerFilterText || "");
            if (!focusable || ((selectable || focusable) && show)) {
                // Pokud potomci neodpovídají hledanému výrazu, můžeme je všechny přidat - hledám podle nadřazeného node a můžu se podívat na potomky - pokud to nebude někde třeba, dáme na konfiguraci
                return {
                    ...node,
                }

                // Jen node, bez přidání potomků
                // return {
                //     ...node,
                //     children: null,
                // }
            } else {
                return null;
            }
        }

        // Má potomky, bude vždy rozbalený (nebo je nastaveno vždy mít rozbaleno), pokud je nastaven nějaký filtr - má položky, které filtru odpovídají
        if (lowerFilterText || alwaysExpanded) {
            newExpandedIds[getItemId(node)] = true;
        }

        return {
            ...node,
            expanded: lowerFilterText || alwaysExpanded ? true : false,
            children: [...nodeChildren]
        };
    }

    // @param alwaysExpanded pokud je true, budou node označené jako expanded
    getFilteredTree = (items, expandedIds, getItemId, filterText, newExpandedIds, shouldItemRender, allowSelectItem, allowFocusItem, alwaysExpanded) => {
        const result = [];

        const lowerFilterText = filterText ? filterText.toLocaleLowerCase() : filterText;

        items.forEach(node => {
            var newNode = this.getFilteredTreeNode(node, lowerFilterText, newExpandedIds, getItemId, shouldItemRender, allowSelectItem, allowFocusItem, alwaysExpanded);
            if (newNode) {
                result.push(newNode);
            }
        });

        return result;
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
     * @param allowSelectItem funkce, která vrací true, pokud lze položku vybrat
     * @param allowFocusItem funkce, která vrací true, pokud lze položku dát focus
     * @param modifyExpanded pokud je true, bude se brát expanded na základě hledání ve stromu, jinak se použije ten předaný jako expandedIds
     * @param alwaysExpanded pokud je true, budou node označené jako expanded (pouze pokud je modifyExpanded === true)
     * @return v případě stromu vrací: { items: [], itemsDepth: [], newExpandedIds: {}}, jinak vrací { items: [] }
     */
    getNewFilteredItems = (items, customFilter, shouldItemRender, inputStrValue, tree, expandedIds, getItemId, allowSelectItem, allowFocusItem, modifyExpanded, alwaysExpanded) => {
        // Sploštění stromu, pokud je potřeba a jeho filtrování, pokud není customFilter
        var result;
        if (tree) {
            // Filtrování stromu, pokud není nastaven customFilter
            let filteredItems;
            let newExpandedIds;
            if (!customFilter && shouldItemRender) {
                newExpandedIds = {};
                filteredItems = this.getFilteredTree(items, expandedIds, getItemId, inputStrValue, newExpandedIds, shouldItemRender, allowSelectItem, allowFocusItem, !customFilter && modifyExpanded && alwaysExpanded);
                // filteredItems = items;
            } else {
                filteredItems = items;
            }

            let flatTree;
            if (!customFilter && modifyExpanded) {
                flatTree = this.getFlatTree(filteredItems, newExpandedIds, getItemId);
            } else {
                flatTree = this.getFlatTree(filteredItems, expandedIds, getItemId);
            }
            result = {
                items: flatTree.list,
                itemsDepth: flatTree.depthList,
                newExpandedIds: newExpandedIds
            }
        } else {
            result = {
                items
            };

            // Jendoduchý filtr - pouze v případě plochého seznamu - ne stromu, strom se filtruje jinak
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
        if (!this.state.isOpen) {   // jen pokud je menu zobrazeno
            return;
        }

        var node = ReactDOM.findDOMNode(this.refs.input)
        var rect = node.getBoundingClientRect()
        var computedStyle = getComputedStyle(node)
        var marginBottom = parseInt(computedStyle.marginBottom, 10)
        var marginLeft = parseInt(computedStyle.marginLeft, 10)

        const position = {x: rect.left + marginLeft, y: rect.bottom + marginBottom};
        const inputHeight = rect.height;

        const elementNode = ReactDOM.findDOMNode(this.refs.menuParent);
        const element = $(elementNode);
        const screen = $(document);
        let elementSize = {w: element.width(), h: element.height()};
        const windowSize = {w: screen.width(), h: screen.height()};

        let x = position.x;
        let y = position.y;

        if (y + elementSize.h > windowSize.h) { // nevejde se dolu, dáme ho nahoru
            y = y - elementSize.h - inputHeight - 2;    // číslo 2 kvůli border 1px

            if (y < 0) {    // nevejde se nahoru ani dolu, neřešíme
                y = position.y;
            }
        }

        element.css({
            top: y + 'px',
            left: x + 'px',
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
                    highlightedIndex: null,
                    changed:false
                }, () => {
                    this.props.onChange(item);
                })
            } else {
                this.changeState({
                    inputStrValue: this.props.getItemName(item),
                    value: item,
                    isOpen: false,
                    highlightedIndex: null,
                    changed:false
                }, () => {
                    this.props.onChange(item)
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
            node.children && node.children.forEach(ch => this._getFlatTree(ch, prop, expandedIds, getItemId, depth + 1))
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
                treeInfo,
                this.props.getItemRenderClass
            );
            return React.cloneElement(element, {
                onMouseDown: () => this.setIgnoreBlur(true),
                onMouseEnter: () => this.highlightItemFromMouse(index),
                onClick: () => this.selectItemFromMouse(item),
                ref: `item-${index}`,
                key: `item-${index}`,
            })
        })
        var menu = this.renderMenuContainer(items, this.state.value)
        return React.cloneElement(menu)
    }

    handleInputBlur() {
        _debugStates && console.log('...handleInputBlur', 'state.hasFocus', this.state.hasFocus, '_ignoreBlur', this._ignoreBlur);

        if (!this._ignoreBlur) {
            this.changeState({hasFocus: false})
            this.closeMenu(true);
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
        ReactDOM.findDOMNode(this.refs.input).select()
        this.changeState({isOpen: true}, () => {
            this.props.onSearchChange(this.state.inputStrValue);
        })
    }

    closeMenu(callBlurAfterSetState = false) {
        if (!this.state.isOpen && this.state.highlightedIndex === null && this.state.inputStrValue === this.props.getItemName(this.state.value)) {
            // Není třeba nastavovat state
            if (callBlurAfterSetState) {
                const {onBlur} = this.props;
                const {value} = this.state;
                onBlur && onBlur(value);
            }
            return;
        }

        var addState = {
            isOpen: false,
            highlightedIndex: null,
            inputStrValue: this.props.getItemName(this.state.value),
            changed: false
        }
        _debugStates && console.log("#### closeMenu", "prev state", this.state, "props", this.props, "state change", addState);
        this.changeState(addState, () => {
            //ReactDOM.findDOMNode(this.refs.input).select()
            if (callBlurAfterSetState) {
                const {onBlur} = this.props;
                const {value} = this.state;
                onBlur && onBlur(value);
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
                                 className={(this.state.isOpen ? 'btn btn-default opened' : 'btn btn-default closed') + (this.props.disabled ? " disabled" : "")}
                                 onClick={()=>{if (!this.props.disabled) {this.state.isOpen ? this.closeMenu() : this.openMenu()}}}><Icon
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
    getItemRenderClass: item => null,
    alwaysExpanded: false,
    renderItem: (item, isHighlighted, isSelected, allowSelect = true, allowFocus = true, treeInfo = null /*{expanded, depth, onExpandCollapse}*/, getItemRenderClass) => {
        var cls = 'item';
        const itemCls = getItemRenderClass(item);
        if (itemCls) {
            cls += ' ' + itemCls;
        }
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
    getItemRenderClass: React.PropTypes.func, // načtení doplňující class pro položku - aby nebylo nutné měnit kvůli class celý renderItem
    alwaysExpanded: React.PropTypes.bool,
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
