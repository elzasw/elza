/**
 *
 */
import React from 'react';

import {Button, Glyphicon, Nav, NavItem} from 'react-bootstrap';
import {ResizeStore} from 'stores';
import ReactDOM from 'react-dom';

require ('./Tabs.less');

var TabsContainer = class TabsContainer extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        var cls = "tabs-container";
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        return (
            <div className={cls}>
                {this.props.children}
            </div>
        );
    }
}

var TabContent = class TabContent extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        var cls = "tab-content";
        if (this.props.className) {
            cls += " " + this.props.className;
        }
        return (
            <div className={cls}>
                {this.props.children}
            </div>
        );
    }
}

var Tabs = class Tabs extends React.Component {
    constructor(props) {
        super(props);

        this.handleTabSelect = this.handleTabSelect.bind(this);
        this.handleTabClose = this.handleTabClose.bind(this);
        this.handleResize = this.handleResize.bind(this);

        ResizeStore.listen(status => {
            this.handleResize();
        });
    }

    handleResize() {
        var el = ReactDOM.findDOMNode(this.refs.tabs);
        var width = el.offsetWidth;
    }

    handleTabClose(item, e) {
        this.props.onClose(item);

        e.preventDefault();
        e.stopPropagation();
    }

    handleTabSelect(itemId) {
        var item = this.props.items.one(i => {
            if (i.id === itemId) {
                return i;
            } else {
                return null;
            }
        });
        this.props.onSelect(item);
    }

    render() {
        var tabs = this.props.items.map((item) => {
            var title = item.title || "Tab " + item.id;
            return <NavItem eventKey={item.id}>{title}<Button onClick={this.handleTabClose.bind(this, item)}><Glyphicon glyph="remove" /></Button></NavItem>
        });

        return (
            <Nav ref="tabs" className='tabs-tabs-container' bsStyle="tabs" onSelect={this.handleTabSelect} activeKey={this.props.activeItem ? this.props.activeItem.id : null}>
                {tabs}
            </Nav>
        );
    }
}

module.exports = {
    Container: TabsContainer,
    Tabs: Tabs,
    Content: TabContent,
}