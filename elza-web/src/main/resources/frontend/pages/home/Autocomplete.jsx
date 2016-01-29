const React = require('react')
import ReactDOM from 'react-dom';
const scrollIntoView = require('dom-scroll-into-view')
import {AbstractReactComponent} from 'components';

require ('./Autocomplete.less')
let _debugStates = []

var keyDownHandlers = {
    ArrowDown: function() {
        event.preventDefault()
        var { highlightedIndex } = this.state
        var index = (highlightedIndex === null || highlightedIndex === this.getFilteredItems().length - 1) ?  0 : highlightedIndex + 1
        this._performAutoCompleteOnKeyUp = true
        this.setState({
            highlightedIndex: index,
            isOpen: true,
        })
    },

    ArrowUp: function(event) {
        event.preventDefault()
        var { highlightedIndex } = this.state
        var index = (highlightedIndex === 0 || highlightedIndex === null) ? this.getFilteredItems().length - 1 : highlightedIndex - 1
        this._performAutoCompleteOnKeyUp = true
        this.setState({
            highlightedIndex: index,
            isOpen: true,
        })
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
                ReactDOM.findDOMNode(this.refs.input).select()
            })
        } else {
            var item = this.getFilteredItems()[this.state.highlightedIndex]
            this.setState({
                value: this.props.getItemValue(item),
                isOpen: false,
                highlightedIndex: null
            }, () => {
                //ReactDOM.findDOMNode(this.refs.input).focus() // TODO: file issue
                ReactDOM.findDOMNode(this.refs.input).setSelectionRange(
                    this.state.value.length,
                    this.state.value.length
                )
                this.props.onSelect(this.state.value, item)
            })
        }
    },

    Escape: function(event) {
        this.setState({
            highlightedIndex: null,
            isOpen: false
        })
    }
}

var Autocomplete = class Autocomplete extends AbstractReactComponent {
    constructor(props) {
        super();

        this.bindMethods('handleKeyDown', 'handleChange', 'handleKeyUp', 'getFilteredItems', 'maybeAutoCompleteText',
            'maybeScrollItemIntoView', 'handleInputFocus', 'handleInputClick', 'handleInputBlur', 'handleInputFocus',
            'handleKeyDown')

        this.state = {
            value: props.initialValue || '',
            isOpen: false,
            highlightedIndex: null,
        }
    }

    renderMenuItems(items, value, style) {
        return (
            <div className='autocomplete-menu-container'>
                <div className='autocomplete-menu'>
                    {items}
                </div>
                <div className='autocomplete-menu-actions'>
                    actions...
                </div>
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
            value: event.target.value,
        }, () => {
            this.props.onChange(event, this.state.value)
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

        if (this.props.shouldItemRender) {
            items = items.filter((item) => (
                this.props.shouldItemRender(item, this.state.value)
            ))
        }

        if (this.props.sortItems) {
            items.sort((a, b) => (
              this.props.sortItems(a, b, this.state.value)
            ))
        }

        return items
    }

    maybeAutoCompleteText() {
        if (this.state.value === '') {
            return
        }

        var { highlightedIndex } = this.state
        var items = this.getFilteredItems()

        if (items.length === 0) {
            return
        }

        var matchedItem = highlightedIndex !== null ? items[highlightedIndex] : items[0]
        var itemValue = this.props.getItemValue(matchedItem)
        var itemValueDoesMatch = (itemValue.toLowerCase().indexOf(this.state.value.toLowerCase()) === 0)
      
        if (itemValueDoesMatch) {
            var node = ReactDOM.findDOMNode(this.refs.input)
            var setSelection = () => {
                node.value = itemValue
                node.setSelectionRange(this.state.value.length, itemValue.length)
            }
            if (highlightedIndex === null) {
                this.setState({ highlightedIndex: 0 }, setSelection)
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
            value: this.props.getItemValue(item),
            isOpen: false,
            highlightedIndex: null
        }, () => {
            this.props.onSelect(this.state.value, item)
            ReactDOM.findDOMNode(this.refs.input).focus()
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
                {cursor: 'default'}
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
        var menu = this.renderMenuItems(items, this.state.value, style)
        return React.cloneElement(menu, { ref: 'menu' })
    }

    handleInputBlur () {
        if (this._ignoreBlur) {
            return
        }
        this.setState({
            isOpen: false,
            highlightedIndex: null
        })
    }

    handleInputFocus () {
        if (this._ignoreBlur) {
            return
        }
        this.setState({ isOpen: true })
    }

    handleInputClick () {
        if (this.state.isOpen === false) {
            this.setState({ isOpen: true })
        }
    }

    render() {
        if (this.props.debug) { // you don't like it, you love it
            _debugStates.push({
                id: _debugStates.length,
                state: this.state
            })
        }

        return (
            <div style={{display: 'inline-block'}}
                onFocus={this.props.onFocus}
                onBlur={this.props.onBlur}
            >
                <input
                    {...this.props.inputProps}
                    role="combobox"
                    aria-autocomplete="both"
                    ref="input"
                    onFocus={this.handleInputFocus}
                    onBlur={this.handleInputBlur}
                    onChange={(event) => this.handleChange(event)}
                    onKeyDown={(event) => this.handleKeyDown(event)}
                    onKeyUp={(event) => this.handleKeyUp(event)}
                    onClick={this.handleInputClick}
                    value={this.state.value}
                />
                {this.state.isOpen && this.renderMenu()}
                {this.props.debug && (
                  <pre style={{marginLeft: 300}}>
                    {JSON.stringify(_debugStates.slice(_debugStates.length - 5, _debugStates.length), null, 2)}
                  </pre>
                )}
            </div>
        )
    }
}

Autocomplete.defaultProps = {
    inputProps: {},
    onChange () {},
    onSelect (value, item) {},
    shouldItemRender () { return true },
}
Autocomplete.propTypes = {
    initialValue: React.PropTypes.any,
    onFocus: React.PropTypes.func,
    onBlur: React.PropTypes.func,
    onChange: React.PropTypes.func,
    onSelect: React.PropTypes.func,
    shouldItemRender: React.PropTypes.func,
    renderItem: React.PropTypes.func.isRequired,
    inputProps: React.PropTypes.object
}

module.exports = Autocomplete
