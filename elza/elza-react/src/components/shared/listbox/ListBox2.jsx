/**
 *  ListBox komponenta - jako ListBox, bez podpory multiselect, ale s podporou samostatného focus a s klient Lazy.
 *
 **/

import React from 'react';
import AbstractReactComponent from '../../AbstractReactComponent';
import LazyListBox from './LazyListBox';

import './ListBox2.scss';

class ListBox2 extends AbstractReactComponent {

    handleGetItems = (fromIndex, toIndex) => {
        const {items} = this.props;
        return new Promise((resolve, reject) => {
            resolve({items: items.slice(fromIndex, toIndex), count: items.length});
        });
    };

    render() {
        const {itemHeight} = this.props;

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

export default ListBox2;
