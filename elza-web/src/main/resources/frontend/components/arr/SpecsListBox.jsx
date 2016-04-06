/**
 *  ListBox komponenta s možností filtrování a hledání a vybrání specifikací.
 *
 **/

import React from 'react';
import {FilterableListBox, AbstractReactComponent, i18n} from 'components';

var SpecsListBox = class SpecsListBox extends AbstractReactComponent {
    constructor(props) {
        super(props)

        this.bindMethods('handleSpecItemsChange', 'handleSpecSearch')

        var state = {
            specItems: [],
            specSearchText: '',
            selectedSpecItems: [],
            selectedSpecItemsType: 'unselected',
        }
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.callSpecSearch('')
    }

    handleSpecItemsChange(type, ids) {
        this.setState({
            selectedSpecItems: ids,
            selectedSpecItemsType: type,
        }, this.props.onSpecItemsChange(type, ids))
    }

    handleSpecSearch(text) {
        this.setState({
            specSearchText: text
        }, this.callSpecSearch)
    }

    callSpecSearch() {
        const {refType} = this.props
        const {specSearchText} = this.state

        if (refType.useSpecification) {
            var fspecSearchText = specSearchText.toLowerCase()
            var specItems = []
            refType.descItemSpecs.forEach(i => {
                if (!specSearchText || i.name.toLowerCase().indexOf(fspecSearchText) !== -1) {
                    specItems.push({id: i.id, name: i.name})
                }
            })
            this.setState({
                specItems: specItems,
            })
        }
    }

    render() {
        const {refType, label} = this.props
        const {selectedSpecItemsType, selectedSpecItems, specItems} = this.state
        
        return (
            <FilterableListBox
                className='filter-content-container'
                searchable
                items={specItems}
                label={label}
                selectionType={selectedSpecItemsType}
                selectedIds={selectedSpecItems}
                onChange={this.handleSpecItemsChange}
                onSearch={this.handleSpecSearch}
            />
        )        
    }
}

module.exports = SpecsListBox

