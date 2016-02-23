/**
 *  ListBox komponenta.
 *
 **/

import React from 'react';
import {AbstractReactComponent} from 'components';
import ReactDOM from 'react-dom';
const scrollIntoView = require('dom-scroll-into-view')

require ('./ListBox.less');

var keyDownHandlers = {
    Enter: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {items} = this.props
        const {activeIndex} = this.state

        if (activeIndex !== null) {
            this.props.onSelect(items[activeIndex], activeIndex)
        }
    },
    Home: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {activeIndex} = this.state
        const {items} = this.props

        if (items.length > 0) {
            const newActiveIndex = 0
            this.setState({activeIndex: newActiveIndex}, this.ensureItemVisible.bind(this, newActiveIndex))
        }
    },
    End: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {activeIndex} = this.state
        const {items} = this.props

        if (items.length > 0) {
            const newActiveIndex = items.length - 1
            this.setState({activeIndex: newActiveIndex}, this.ensureItemVisible.bind(this, newActiveIndex))
        }
    },
    ArrowUp: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {activeIndex} = this.state
        const {items} = this.props

        if (items.length > 0) {
            var newActiveIndex = null

            if (activeIndex === null) {
                newActiveIndex = 0
            } else {
                if (activeIndex > 0) {
                    newActiveIndex = activeIndex - 1
                }
            }
            if (newActiveIndex !== null) {
                this.setState({activeIndex: newActiveIndex}, this.ensureItemVisible.bind(this, newActiveIndex))
            }
        }
    },
    ArrowDown: function(e) {
        e.preventDefault()
        e.stopPropagation()

        const {activeIndex} = this.state
        const {items} = this.props

        if (items.length > 0) {
            var newActiveIndex = null

            if (activeIndex === null) {
                newActiveIndex = 0
            } else {
                if (activeIndex + 1 < items.length) {
                    newActiveIndex = activeIndex + 1
                }
            }
            if (newActiveIndex !== null) {
                this.setState({activeIndex: newActiveIndex}, this.ensureItemVisible.bind(this, newActiveIndex))
            }
        }
    }
}

var ListBox = class ListBox extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleKeyDown', 'ensureItemVisible')

        this.state = {
            activeIndex: null,
        }
    }

    ensureItemVisible(index) {
        var itemNode = ReactDOM.findDOMNode(this.refs['item-' + index])
        if (itemNode !== null) {
            var containerNode = ReactDOM.findDOMNode(this.refs.container)
            scrollIntoView(itemNode, containerNode, { onlyScrollIfNeeded: true, alignWithTop:false })
        }
    }

    handleKeyDown(event) {
        if (keyDownHandlers[event.key]) {
            keyDownHandlers[event.key].call(this, event)
        }
    }

    render() {
        var cls = "listbox-container";
        if (this.props.className) {
            cls += " " + this.props.className;
        }

        var items = this.props.items.map((item, index) => {
            const active = (index === this.state.activeIndex)
            return (
                <div className={'listbox-item' + (active ? ' active' : '')} ref={'item-' + index}>
                    {this.props.renderItemContent(item)}
                </div>
            )
        })

        return (
            <div className={cls} onKeyDown={this.handleKeyDown} tabIndex={0} ref='container'>
                {items}  
            </div>
        );
    }
}

ListBox.defaultProps = {
    renderItemContent: (item, isActive) => {
        return (
            <div>{item.name}</div>
        )
    }
}

module.exports = ListBox
