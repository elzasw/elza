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
import {WebApi} from 'actions'
import {hasDescItemTypeValue} from 'components/arr/ArrUtils'
const FundFilterCondition = require('./FundFilterCondition')
const SpecsListBox = require('./SpecsListBox')

var FundFilterSettings = class FundFilterSettings extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callValueSearch', 'handleValueSearch',
            'handleValueItemsChange', 'renderConditionFilter', 'handleSpecItemsChange', 'handleConditionChange',
            'handleSubmit', 'renderValueFilter')

        var state = {
            valueItems: [],
            valueSearchText: '',
            selectedValueItems: [],
            selectedValueItemsType: 'unselected',
            selectedSpecItems: [],
            selectedSpecItemsType: 'unselected',
            conditionSelectedCode: 'none',
            conditionValues: []
        }

        const {filter} = props
        if (typeof filter !== 'undefined' && filter) {
            state.selectedValueItems = filter.values
            state.selectedValueItemsType = filter.valuesType
            state.selectedSpecItems = filter.specs
            state.selectedSpecItemsType = filter.specsType
            state.conditionSelectedCode = filter.conditionType
            state.conditionValues = filter.condition
        }

        this.state = state
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
        this.callValueSearch('')
    }

    handleValueSearch(text) {
        this.setState({
            valueSearchText: text
        }, this.callValueSearch)
    }

    callValueSearch() {
        const {versionId, refType, dataType} = this.props
        const {valueSearchText} = this.state

        if (!hasDescItemTypeValue(dataType)) {  // pokud nemá hodnotu, nemůžeme volat
            return
        }

        var specIds = []
        if (refType.useSpecification) {
            specIds = this.refs.specsListBox.getSpecsIds()

            if (specIds.length === 0) { // pokud nemá nic vybráno, nevrátily by se žádné položky a není třeba volat server
                this.setState({
                    valueItems: [],
                })
                return 
            }
        }

        WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, specIds, 200)
            .then(json => {
                this.setState({
                    valueItems: json.map(i => ({id: i, name: i})),
                })
            })
    }

    handleSpecItemsChange(data) {
        const {type, ids} = data
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

    handleConditionChange(selectedCode, values) {
        this.setState({
            conditionSelectedCode: selectedCode,
            conditionValues: values,
        })
    }

    renderValueFilter() {
        const {refType, dataType} = this.props
        const {valueItems, selectedValueItems, selectedValueItemsType} = this.state

        if (!hasDescItemTypeValue(dataType)) {
            return null
        }

        if (dataType.code === 'UNITDATE' || dataType.code === 'TEXT') { // zde je výjimka a nechceme dle hodnoty
            return null
        }

        return (
            <FilterableListBox
                className='filter-content-container'
                searchable
                items={valueItems}
                label={i18n('arr.fund.filterSettings.filterByValue.title')}
                selectionType={selectedValueItemsType}
                selectedIds={selectedValueItems}
                onChange={this.handleValueItemsChange}
                onSearch={this.handleValueSearch}
            />
        )
    }

    renderConditionFilter() {
        const {refType, dataType} = this.props
        const {conditionSelectedCode, conditionValues} = this.state

        if (!hasDescItemTypeValue(dataType)) {
            return null
        }

        var items = []
        switch (dataType.code) {
            case 'TEXT':
            case 'STRING':
            case 'FORMATTED_TEXT':
            case 'UNITID':
                items = [
                    {values: 0, code: 'none', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'empty', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'notEmpty', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'contain', name: i18n('arr.fund.filterSettings.condition.string.contain')},
                    {values: 1, code: 'notContain', name: i18n('arr.fund.filterSettings.condition.string.notContain')},
                    {values: 1, code: 'begin', name: i18n('arr.fund.filterSettings.condition.begin')},
                    {values: 1, code: 'end', name: i18n('arr.fund.filterSettings.condition.end')},
                    {values: 1, code: 'eq', name: i18n('arr.fund.filterSettings.condition.eq')},
                ]
                break
            case 'INT':
            case 'DECIMAL':
                items = [
                    {values: 0, code: 'none', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'empty', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'notEmpty', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'gt', name: i18n('arr.fund.filterSettings.condition.gt')},
                    {values: 1, code: 'ge', name: i18n('arr.fund.filterSettings.condition.ge')},
                    {values: 1, code: 'lt', name: i18n('arr.fund.filterSettings.condition.lt')},
                    {values: 1, code: 'le', name: i18n('arr.fund.filterSettings.condition.le')},
                    {values: 1, code: 'eq', name: i18n('arr.fund.filterSettings.condition.eq')},
                    {values: 1, code: 'ne', name: i18n('arr.fund.filterSettings.condition.ne')},
                    {values: 2, code: 'interval', name: i18n('arr.fund.filterSettings.condition.interval')},
                    {values: 2, code: 'notInterval', name: i18n('arr.fund.filterSettings.condition.notInterval')},
                ]
                break
            case 'PARTY_REF':
            case 'RECORD_REF':
                items = [
                    {values: 0, code: 'none', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'empty', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'notEmpty', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'contain', name: i18n('arr.fund.filterSettings.condition.string.contain')},
                ]
                break
            case 'UNITDATE':
                items = [
                    {values: 0, code: 'none', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'empty', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'notEmpty', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'eq', name: i18n('arr.fund.filterSettings.condition.eq')},
                    {values: 1, code: 'lt', name: i18n('arr.fund.filterSettings.condition.unitdate.lt')},
                    {values: 1, code: 'gt', name: i18n('arr.fund.filterSettings.condition.unitdate.gt')},
                    {values: 1, code: 'subset', name: i18n('arr.fund.filterSettings.condition.unitdate.subset')},
                    {values: 1, code: 'intersect', name: i18n('arr.fund.filterSettings.condition.unitdate.intersect')},
                ]
                break
            case 'COORDINATES':
                break
            case 'PACKET_REF':
                break
            case 'ENUM':
                break 
        }

        if (items.length === 0) {
            return null
        }

        return (
            <FundFilterCondition
                className='filter-content-container'
                label={i18n('arr.fund.filterSettings.filterByCondition.title')}
                selectedCode={conditionSelectedCode}
                values={conditionValues}
                onChange={this.handleConditionChange}
                items={items}
            >
                <Input type='text' />
            </FundFilterCondition>
        )
    }

    handleSubmit() {
        const {selectedValueItems, selectedValueItemsType, selectedSpecItems, selectedSpecItemsType, conditionSelectedCode, conditionValues} = this.state
        const {onSubmitForm, refType} = this.props

        var data = {
            values: selectedValueItems,
            valuesType: selectedValueItemsType,
            condition: conditionValues,
            conditionType: conditionSelectedCode,
        }

        if (refType.useSpecification) {
            data.specs = selectedSpecItems
            data.specsType = selectedSpecItemsType
        }

console.log(data)

        // ##
        // # Test, zda není filtr prázdný
        // ##
        var outData = null

        if (data.valuesType === 'selected' || data.values.length > 0) {      // je zadáno filtrování podle hodnoty
            outData = data
        } else if (refType.useSpecification && (data.specsType === 'selected' || data.specs.length > 0)) {     // je zadáno filtrování podle specifikace
            outData = data
        } else if (data.conditionType !== 'none') { // je zadáno filtrování podle podmínky
            outData = data
        }

        onSubmitForm(outData)
    }

    render() {
        const {refType, onClose} = this.props
        const {conditionSelectedCode, conditionValues, valueItems, selectedValueItems, selectedValueItemsType, selectedSpecItems, selectedSpecItemsType} = this.state

        var specContent = null
        if (refType.useSpecification) {
            specContent = (
                <SpecsListBox
                    ref='specsListBox'
                    refType={refType}
                    label={i18n('arr.fund.filterSettings.filterBySpecification.title')}
                    value={{type: selectedSpecItemsType, ids: selectedSpecItems}}
                    onChange={this.handleSpecItemsChange}
                />
            )
        }
        
        var valueContent = this.renderValueFilter()

        var conditionContent = this.renderConditionFilter()

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
                    <Button onClick={this.handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = FundFilterSettings
