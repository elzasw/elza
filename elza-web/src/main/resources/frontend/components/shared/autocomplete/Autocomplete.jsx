const React = require('react')
import ReactDOM from 'react-dom';
import {Button, Input} from 'react-bootstrap';
const scrollIntoView = require('dom-scroll-into-view')
import {Icon, AbstractReactComponent} from 'components';

require ('./Autocomplete.less')
let _debugStates = []

var keyDownHandlers = {
    ArrowRight: ()=>{},
    ArrowLeft: ()=>{},
    Home: ()=>{},
    End: ()=>{},
    Alt: ()=>{},
    Tab: ()=>{},

    ArrowDown: function(event) {
        event.preventDefault()

        if (event.altKey) {
            this.setState({
                isOpen: true,
            })
        } else {
            var { highlightedIndex } = this.state
            var index = (highlightedIndex === null || highlightedIndex === this.getFilteredItems().length - 1) ?  0 : highlightedIndex + 1
            this._performAutoCompleteOnKeyUp = true

            this.setState({
                highlightedIndex: index,
            })
        }
    },

    ArrowUp: function(event) {
        event.preventDefault()

        if (event.altKey) {
            this.closeMenu();
        } else {
            var { highlightedIndex } = this.state
            var index = (highlightedIndex === 0 || highlightedIndex === null) ? this.getFilteredItems().length - 1 : highlightedIndex - 1
            this._performAutoCompleteOnKeyUp = true

            this.setState({
                highlightedIndex: index,
            })
        }
    },

    Enter: function(event) {
        if (this.state.isOpen === false) {
            // already selected this, do nothing
            return
        } else if (this.state.highlightedIndex == null) {
            // hit enter after focus but before typing anything so no autocomplete attempt yet
            this.setState({
                isOpen: false
            }, () => {
                ReactDOM.findDOMNode(this.refs.input.getInputDOMNode()).select()
            })
        } else {
            var item = this.getFilteredItems()[this.state.highlightedIndex]
            this.setState({
                inputStrValue: this.props.getItemName(item),
                value: item,
                isOpen: false,
                highlightedIndex: null
            }, () => {
                //ReactDOM.findDOMNode(this.refs.input.getInputDOMNode()).focus() // TODO: file issue
                ReactDOM.findDOMNode(this.refs.input.getInputDOMNode()).setSelectionRange(
                    this.state.inputStrValue.length,
                    this.state.inputStrValue.length
                )
                this.props.onChange(this.props.getItemId(item), item)
            })
        }
    },

    Escape: function(event) {
        this.closeMenu();
    }
}

var Autocomplete = class Autocomplete extends AbstractReactComponent {
    constructor(props) {
        super();

        this.bindMethods('handleKeyDown', 'handleChange', 'handleKeyUp', 'getFilteredItems', 'maybeAutoCompleteText',
            'maybeScrollItemIntoView', 'handleInputFocus', 'handleInputClick', 'handleInputBlur',
            'handleKeyDown', 'openMenu', 'closeMenu', 'handleDocumentClick')

        this.state = {
            value: props.value,
            inputStrValue: props.getItemName(props.value),
            isOpen: false,
            highlightedIndex: null,
        }
    }

    handleDocumentClick(e) {
        var el1 = ReactDOM.findDOMNode(this.refs.input.getInputDOMNode());
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
        if (!inside) {
            this.closeMenu();
        }
    }

    componentDidMount() {
        document.addEventListener("click", this.handleDocumentClick, false)
    }

    componentWillUnmount() {
        document.removeEventListener("click", this.handleDocumentClick, false)
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
            <div ref='menuParent' className={cls}>
                {header}
                <div ref='menu' className='autocomplete-menu'>
                    {items}
                </div>
                {footer}
            </div>
        )
    }

    componentWillMount () {
        this._ignoreBlur = false
        this._performAutoCompleteOnUpdate = false
        this._performAutoCompleteOnKeyUp = false
    }

    componentWillReceiveProps () {
        this._performAutoCompleteOnUpdate = true
    }

    componentDidUpdate(prevProps, prevState) {
        if (this.state.isOpen === true && prevState.isOpen === false) {
            this.setMenuPositions()
        }

        if (this.state.isOpen && this._performAutoCompleteOnUpdate) {
            this._performAutoCompleteOnUpdate = false
            this.maybeAutoCompleteText()
        }

        this.maybeScrollItemIntoView()
    }

    maybeScrollItemIntoView () {
        if (this.state.isOpen === true && this.state.highlightedIndex !== null) {
          var itemNode = ReactDOM.findDOMNode(this.refs[`item-${this.state.highlightedIndex}`])
          var menuNode = ReactDOM.findDOMNode(this.refs.menu)
          scrollIntoView(itemNode, menuNode, { onlyScrollIfNeeded: true })
        }
    }

    handleKeyDown (event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
        } else {
            this.setState({
                highlightedIndex: null,
                isOpen: true
            })
        }
    }

    handleChange (event) {
        this._performAutoCompleteOnKeyUp = true
        this.setState({
            inputStrValue: event.target.value,
        }, () => {
            this.props.onSearchChange(this.state.inputStrValue)
        })
    }

    handleKeyUp () {
        if (this._performAutoCompleteOnKeyUp) {
            this._performAutoCompleteOnKeyUp = false
            this.maybeAutoCompleteText()
        }
    }

    getFilteredItems () {
        let items = this.props.items

        if (!this.props.customFilter) {
            if (this.props.shouldItemRender) {
                items = items.filter((item) => (
                    this.props.shouldItemRender(item, this.state.inputStrValue)
                ))
            }
        }

        return items
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
        var itemValueDoesMatch = (itemValue.toLowerCase().indexOf(this.state.inputStrValue.toLowerCase()) === 0)
      
        if (itemValueDoesMatch) {
            var node = ReactDOM.findDOMNode(this.refs.input.getInputDOMNode())
            var setSelection = () => {
                node.value = itemValue
                node.setSelectionRange(this.state.inputStrValue.length, itemValue.length)
            }
            if (highlightedIndex === null) {
                this.setState({ highlightedIndex: 0 }, setSelection)
            } else {
                setSelection()
            }
        }
    }

    setMenuPositions() {
        var node = ReactDOM.findDOMNode(this.refs.input.getInputDOMNode())
        var rect = node.getBoundingClientRect()
        var computedStyle = getComputedStyle(node)
        var marginBottom = parseInt(computedStyle.marginBottom, 10)
        var marginLeft = parseInt(computedStyle.marginLeft, 10)
        var marginRight = parseInt(computedStyle.marginRight, 10)
        this.setState({
            menuTop: rect.bottom + marginBottom,
            menuLeft: rect.left + marginLeft,
            menuWidth: rect.width + marginLeft + marginRight
        })
    }

    highlightItemFromMouse(index) {
        this.setState({ highlightedIndex: index })
    }

    selectItemFromMouse(item) {
        this.setState({
            inputStrValue: this.props.getItemName(item),
            value: item,
            isOpen: false,
            highlightedIndex: null
        }, () => {
            this.props.onChange(this.props.getItemId(item), item)
            ReactDOM.findDOMNode(this.refs.input.getInputDOMNode()).focus()
            this.setIgnoreBlur(false)
        })
    }

    setIgnoreBlur (ignore) {
        this._ignoreBlur = ignore
    }

    renderMenu () {
        var items = this.getFilteredItems().map((item, index) => {
            var element = this.props.renderItem(
                item,
                this.state.highlightedIndex === index,
                this.state.value && this.props.getItemId(this.state.value) === this.props.getItemId(item)
            )
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
            minWidth: this.state.menuWidth,
        }
        var menu = this.renderMenuContainer(items, this.state.value, style)
        return React.cloneElement(menu)
    }

    handleInputBlur () {
        this.props.onBlur && this.props.onBlur();
return true;
        if (this._ignoreBlur) {
            return
        }
        this.setState({
            isOpen: false,
            highlightedIndex: null,
            inputStrValue: this.props.getItemName(this.state.value)
        })
    }

    handleInputFocus () {
            this.props.onFocus && this.props.onFocus();
return true;
        if (this._ignoreBlur) {
            return
        }
        this.setState({ isOpen: true })
    }

    openMenu() {
        if (this.state.isOpen) {
            return
        }

        this.setState({ isOpen: true }, () => {
                ReactDOM.findDOMNode(this.refs.input.getInputDOMNode()).select()
            })
    }

    closeMenu() {
        if (!this.state.isOpen) {
            return;
        }

        this.setState({
            isOpen: false,
            highlightedIndex: null,
            inputStrValue: this.props.getItemName(this.state.value)
        }, () => {
                //ReactDOM.findDOMNode(this.refs.input.getInputDOMNode()).select()
            })
    }

    handleInputClick () {
        /*if (this.state.isOpen === false) {
            this.setState({ isOpen: true })
        }*/
    }

    render() {
        var cls = 'autocomplete-control-container'
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }

        var glyph = this.state.isOpen ? 'fa-angle-up' : 'fa-angle-down';

        return (
            <div className={cls}>
                <div className='autocomplete-control-box'>
                    <Input
                        type='text'
                        {...this.props.inputProps}
                        label={this.props.label}
                        disabled={this.props.disabled}
                        bsStyle={this.props.bsStyle}
                        hasFeedback={this.props.hasFeedback}
                        help={this.props.help}
                        role="combobox"
                        aria-autocomplete="both"
                        ref="input"
                        onFocus={this.handleInputFocus}
                        onBlur={this.handleInputBlur}
                        onChange={(event) => this.handleChange(event)}
                        onKeyDown={(event) => this.handleKeyDown(event)}
                        onKeyUp={(event) => this.handleKeyUp(event)}
                        onClick={this.handleInputClick}
                        value={this.state.inputStrValue}
                    />
                    <Button disabled={this.props.disabled} ref='openClose' className={this.state.isOpen ? 'btn btn-default opened' : 'btn btn-default closed'} onClick={()=>{this.state.isOpen ? this.closeMenu() : this.openMenu()}}><Icon glyph={glyph}/></Button>
                </div>
                {this.state.isOpen && this.renderMenu()}
            </div>
        )
    }
}

Autocomplete.defaultProps = {
    inputProps: {},
    onSearchChange () {},
    onChange (value, item) {},
    shouldItemRender () { return true },
    renderItem: (item, isHighlighted, isSelected) => {
        var cls = 'item';
        if (isHighlighted) {
            cls += ' focus'
        }
        if (isSelected) {
            cls += ' active'
        }

        var itemStr;
        if (item.name && item.name.length > 0) {
            itemStr = item.name;
        } else {
            itemStr = <div>&nbsp;</div>;
        }

        return (
            <div
                className={cls}
                key={item.id}
            >{itemStr}</div>
        )
    }
}
Autocomplete.propTypes = {
    initialValue: React.PropTypes.any,
    onFocus: React.PropTypes.func,
    onBlur: React.PropTypes.func,
    onSearchChange: React.PropTypes.func,
    onChange: React.PropTypes.func,
    shouldItemRender: React.PropTypes.func,
    renderItem: React.PropTypes.func,
    inputProps: React.PropTypes.object
}

module.exports = Autocomplete
