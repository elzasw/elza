/**
 *  ListBox komponenta - jako ListBox, bez podpory multiselect, ale s podporou samostatného focus a s klient Lazy.
 *
 **/

import React from 'react';
import {AbstractReactComponent, LazyListBox} from 'components/index.jsx';
import ReactDOM from 'react-dom';
const scrollIntoView = require('dom-scroll-into-view')

require ('./ListBox2.less');

var ListBox2 = class ListBox2 extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleGetItems')
    }

    handleGetItems(fromIndex, toIndex) {
        const {items} = this.props
        return new Promise((resolve, reject) => {
            resolve({items: items.slice(fromIndex, toIndex), count: items.length})
        })
    }

    render() {
        const {items, itemHeight} = this.props;

        return (
            <LazyListBox
                {...this.props}
                getItems={this.handleGetItems}
                itemHeight={itemHeight || 24}
                fetchNow={true}
            />
        );
    }
}

module.exports = ListBox2
