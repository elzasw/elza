/**
 *  ListBox komponenta s možností filtrování a hledání a vybrání položek.
 *
 **/

import React from 'react';
import {FilterableListBox, AbstractReactComponent, i18n} from 'components/shared';
import {indexById, getSetFromIdsList} from 'stores/app/utils.jsx'

class SimpleCheckListBox extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.state = {
            specItems: [],
            specSearchText: '',
        }
    }

    componentWillReceiveProps(nextProps) {
        const props = this.props;
        const {specSearchText} = this.state;
        if (props.items !== nextProps.items) {
            this.setState({
                specItems: props.items
            }, () => this.callSpecSearch(specSearchText))
        }
    }

    getValue = () => {
        let {value} = this.props;
        if (typeof value === 'undefined') {
            value = {type: 'unselected', ids: []}
        }

        return {
            type: value.type || 'unselected',
            ids: value.ids || [],
        }
    };

    componentDidMount() {
        this.callSpecSearch('');
    }

    handleSpecItemsChange = (type, ids) => {
        this.props.onChange({type, ids})
    };

    handleSpecSearch = (text) => {
        this.setState({
            specSearchText: text
        }, this.callSpecSearch);
    };

    getSpecsIdsExt = (items, selectionType, selectedIds) => {
        let specIds = [];
        if (selectionType === 'selected') {
            specIds = selectedIds
        } else {
            let set = getSetFromIdsList(selectedIds);
            items.forEach(i => {
                if (!set[i.id]) {
                    specIds.push(i.id)
                }
            })
        }
        return specIds
    };

    getSpecsIds = () => {
        const {items} = this.props;
        const value = this.getValue();
        return this.getSpecsIdsExt(items, value.type, value.ids)
    };

    callSpecSearch = () => {
        const {items} = this.props;
        const {specSearchText} = this.state;

        let fspecSearchText = specSearchText.toLowerCase();
        let specItems = [];
        items.forEach(i => {
            if (!specSearchText || i.name.toLowerCase().indexOf(fspecSearchText) !== -1) {
                specItems.push(i)
            }
        });
        this.setState({
            specItems: specItems,
        })
    };

    render() {
        const {label} = this.props;
        const {specItems} = this.state;
        const value = this.getValue();

        return (
            <FilterableListBox
                className='filter-content-container'
                searchable
                items={specItems}
                label={label}
                selectionType={value.type}
                selectedIds={value.ids}
                onChange={this.handleSpecItemsChange}
                onSearch={this.handleSpecSearch}
            />
        )
    }
}

export default SimpleCheckListBox

