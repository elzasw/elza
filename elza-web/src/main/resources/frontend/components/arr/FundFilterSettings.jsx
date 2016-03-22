/**
 * Formulář nastavení filtru na sloupečku.
 */

require ('./FundFilterSettings.less')

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {reduxForm} from 'redux-form';
import {FilterableListBox, AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById, getSetFromIdsList} from 'stores/app/utils.jsx'
import {WebApi} from 'actions'

var FundFilterSettings = class FundFilterSettings extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callValueSearch', 'callSpecSearch', 'handleValueSearch', 'handleSpecSearch',
            'handleValueItemsChange', 'handleSpecItemsChange')

        this.state = {
            valueItems: [],
            specItems: [],
            valueSearchText: '',
            specSearchText: '',
            selectedValueItems: [],
            selectedValueItemsType: 'unselected',
            selectedSpecItems: [],
            selectedSpecItemsType: 'unselected',
        }
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.callValueSearch('')
        this.callSpecSearch('')
    }

    handleValueSearch(text) {
        this.setState({
            valueSearchText: text
        }, this.callValueSearch)
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

    callValueSearch() {
        const {versionId, refType} = this.props
        const {valueSearchText, selectedSpecItems, selectedSpecItemsType} = this.state

        var specIds = []
        if (refType.useSpecification) {
            if (selectedSpecItemsType === 'selected') {
                specIds = selectedSpecItems
            } else {
                var set = getSetFromIdsList(selectedSpecItems)
                refType.descItemSpecs.forEach(i => {
                    if (!set[i.id]) {
                        specIds.push(i.id)
                    }
                })
            }
        }

        WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, specIds, 200)
            .then(json => {
                this.setState({
                    valueItems: json.map(i => ({id: i.value, name: i.value})),
                })
            })
    }

    handleSpecItemsChange(type, ids) {
        this.setState({
            selectedSpecItems: ids,
            selectedSpecItemsType: type,
        }, this.callValueSearch)
    }

    handleValueItemsChange(type, ids) {
        this.setState({
            selectedValueItems: ids,
            selectedValueItemsType: type,
        })
    }

    render() {
        const {refType, onClose} = this.props
        const {valueItems, specItems} = this.state

        var specContent
        if (refType.useSpecification) {
            specContent = (
                <FilterableListBox
                    className='filter-content-container'
                    searchable
                    items={specItems}
                    label={i18n('arr.fund.filterSettings.filterBySpecification.title')}
                    onChange={this.handleSpecItemsChange}
                    onSearch={this.handleSpecSearch}
                />
            )
        }

        var valueContent
        valueContent = (
            <FilterableListBox
                className='filter-content-container'
                searchable
                items={valueItems}
                label={i18n('arr.fund.filterSettings.filterByValue.title')}
                onChange={this.handleValueItemsChange}
                onSearch={this.handleValueSearch}
            />
        )

        var conditionContent

        return (
            <div className='fund-filter-settings-container'>
                <Modal.Body>
                    <div className='filters-container'>
                        {specContent}
                        {conditionContent}
                        {valueContent}
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button onClick={()=>{}}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = FundFilterSettings
