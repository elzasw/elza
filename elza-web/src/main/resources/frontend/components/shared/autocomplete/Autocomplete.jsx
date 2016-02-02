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
            event.stopPropagation();
            event.preventDefault();

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
            shouldItemRender: shouldItemRender,
            value: props.value,
            inputStrValue: props.getItemName(props.value),
            isOpen: false,
            highlightedIndex: null,
            hasFocus: false
        }
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

    handleDocumentClick(e) {
_debugStates && console.log("STATE has focus", this.state.hasFocus, "ignore blur", this._ignoreBlur);
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
_debugStates && console.log("@CLICK:", inside);
        if (!inside) {
            var el = ReactDOM.findDOMNode(this.refs.input.getInputDOMNode());
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
            if (this.state.shouldItemRender) {
                items = items.filter((item) => (
                    this.state.shouldItemRender(item, this.state.inputStrValue)
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

    handleInputBlur() {
_debugStates && console.log('...handleInputBlur', 'state.hasFocus', this.state.hasFocus, '_ignoreBlur', this._ignoreBlur);

        if (!this._ignoreBlur) {
            this.setState({hasFocus: false})
            if (this.state.isOpen) {
                this.closeMenu();
            }
            this.props.onBlur && this.props.onBlur();
        } else {
            this._ignoreBlur = false;
        }


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
_debugStates && console.log('...handleInputFocus', 'state.hasFocus', this.state.hasFocus, '_ignoreBlur', this._ignoreBlur);

        if (this.state.hasFocus) {
            return;
        }

        this.setState({hasFocus: true})

        if (!this._ignoreBlur) {
            this.props.onFocus && this.props.onFocus();
        } else {
            this._ignoreBlur = false;
        }
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
_debugStates && console.log(this.props);

        var cls = 'form-group';
        var feedbackIcon = '';
        if (this.props.hasFeedback) {
            cls += ' has-feedback';
        }
        if (this.props.bsStyle) {
            cls += ' has-' + this.props.bsStyle;
            switch (this.props.bsStyle) {
                case 'success':
                    feedbackIcon = 'ok'
                    break;
                case 'warning':
                    feedbackIcon = 'warning-sign'
                    break;
                case 'error':
                    feedbackIcon = 'remove'
                    break;
            }
        }

/*
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
                    <div disabled={this.props.disabled} ref='openClose' className={this.state.isOpen ? 'btn btn-default opened' : 'btn btn-default closed'} onClick={()=>{this.state.isOpen ? this.closeMenu() : this.openMenu()}}><Icon glyph={glyph}/></div>
                    {this.props.actions}
*/

        return (
            <div className={this.props.className}>
                <div className='autocomplete-control-box'>
                    <div className={cls}>
                        {this.props.label && <label className='control-label'>{this.props.label}</label>}
                        <div className='autocomplete-input-container'>
                            <input
                                className='form-control'
                                type='text'
                                {...this.props.inputProps}
                                label={this.props.label}
                                disabled={this.props.disabled}
                                role='combobox'
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
                            <div disabled={this.props.disabled} ref='openClose' className={this.state.isOpen ? 'btn btn-default opened' : 'btn btn-default closed'} onClick={()=>{this.state.isOpen ? this.closeMenu() : this.openMenu()}}><Icon glyph={glyph}/></div>
                            {this.props.actions}                            
                        </div>
                        {this.props.hasFeedback && <span className={'glyphicon form-control-feedback glyphicon-' + feedbackIcon}></span>}
                        {this.props.help && <span className='help-block'>{this.props.help}</span>}
                    </div>
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
    getItemId: (item) => item ? item.id : null,
    getItemName: (item) => item ? item.name : '',
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
    inputProps: React.PropTypes.object,
    actions: React.PropTypes.array
}

module.exports = Autocomplete
