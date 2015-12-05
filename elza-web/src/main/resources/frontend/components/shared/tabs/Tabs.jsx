/**
 *
 */
import React from 'react';

import {Button, Glyphicon, Nav, NavItem} from 'react-bootstrap';

require ('./Tabs.less');

var EntityTabs = class EntityTabs extends React.Component {
    constructor(props) {
        super(props);

        this.handleTabSelect = this.handleTabSelect.bind(this);
        this.handleTabClose = this.handleTabClose.bind(this);
    }

    handleTabClose(item, e) {
        var newActiveItem = null;

        var isSelected = this.props.activeItem && this.props.activeItem.id === item.id;
        if (isSelected) {
            var index = 0;
            var fi = this.props.items.one(i => {
                if (i.id == item.id) {
                    return index;
                }
                index++;
            });
            if (fi + 1 < this.props.items.length) {
                newActiveItem = this.props.items[fi + 1];
            } else if (fi - 1 >= 0) {
                newActiveItem = this.props.items[fi - 1];
            }
        }

        this.props.onClose(item, newActiveItem);

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
            <Nav className='tabs-container' bsStyle="tabs" onSelect={this.handleTabSelect} activeKey={this.props.activeItem ? this.props.activeItem.id : null}>
                {tabs}
            </Nav>
        );
    }
}

module.exports = EntityTabs;