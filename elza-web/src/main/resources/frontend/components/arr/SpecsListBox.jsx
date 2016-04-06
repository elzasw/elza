/**
 *  ListBox komponenta s možností filtrování a hledání a vybrání specifikací.
 *
 **/

import React from 'react';
import {FilterableListBox, AbstractReactComponent, i18n} from 'components';
import {indexById, getSetFromIdsList} from 'stores/app/utils.jsx'
import {getSpecsIds} from 'components/arr/ArrUtils'

var SpecsListBox = class SpecsListBox extends AbstractReactComponent {
    constructor(props) {
        super(props)

        this.bindMethods('handleSpecItemsChange', 'handleSpecSearch', 'getSpecsIds', 'getValue')

        this.state = {
            specItems: [],
            specSearchText: '',
        }
    }

    componentWillReceiveProps(nextProps) {
    }

    getValue() {
        var {value} = this.props
        if (typeof value === 'undefined') {
            value = {type: 'unselected', ids: []}
        }

        return {
            type: value.type || 'unselected',
            ids: value.ids || [],
        }
    }

    componentDidMount() {
        this.callSpecSearch('')
    }

    handleSpecItemsChange(type, ids) {
        this.props.onChange({type, ids})
    }

    handleSpecSearch(text) {
        this.setState({
            specSearchText: text
        }, this.callSpecSearch)
    }

    getSpecsIds() {
        const {refType} = this.props
        const value = this.getValue()
        return getSpecsIds(refType, value.type, value.ids)
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
        const {specItems} = this.state
        const value = this.getValue()
        
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

module.exports = SpecsListBox

