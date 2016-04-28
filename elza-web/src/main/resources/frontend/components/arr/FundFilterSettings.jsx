/**
 * Formulář nastavení filtru na sloupečku.
 */

require ('./FundFilterSettings.less')

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {FilterableListBox, AbstractReactComponent, i18n} from 'components/index.jsx';
import {Modal, Button, Input} from 'react-bootstrap';
import {WebApi} from 'actions/index.jsx';
import {hasDescItemTypeValue} from 'components/arr/ArrUtils.jsx'
const FundFilterCondition = require('./FundFilterCondition')
const SimpleCheckListBox = require('./SimpleCheckListBox')

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
            conditionSelectedCode: 'NONE',
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
        if (refType.useSpecification || dataType.code === 'PACKET_REF') {
            specIds = this.refs.specsListBox.getSpecsIds()

            if (specIds.length === 0) { // pokud nemá nic vybráno, nevrátily by se žádné položky a není třeba volat server
                this.setState({
                    valueItems: [],
                })
                return
            }
        }

        if (dataType.code !== 'UNITDATE' && dataType.code !== 'TEXT' && dataType.code !== 'COORDINATES') {
            WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, specIds, 200)
                .then(json => {
                    this.setState({
                        valueItems: json.map(i => ({id: i, name: i})),
                    })
                })
        }
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
                    {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'CONTAIN', name: i18n('arr.fund.filterSettings.condition.string.contain')},
                    {values: 1, code: 'NOT_CONTAIN', name: i18n('arr.fund.filterSettings.condition.string.notContain')},
                    {values: 1, code: 'BEGIN', name: i18n('arr.fund.filterSettings.condition.begin')},
                    {values: 1, code: 'END', name: i18n('arr.fund.filterSettings.condition.end')},
                    {values: 1, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                ]
                break
            case 'INT':
            case 'DECIMAL':
                items = [
                    {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'GT', name: i18n('arr.fund.filterSettings.condition.gt')},
                    {values: 1, code: 'GE', name: i18n('arr.fund.filterSettings.condition.ge')},
                    {values: 1, code: 'LT', name: i18n('arr.fund.filterSettings.condition.lt')},
                    {values: 1, code: 'LE', name: i18n('arr.fund.filterSettings.condition.le')},
                    {values: 1, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                    {values: 1, code: 'NE', name: i18n('arr.fund.filterSettings.condition.ne')},
                    {values: 2, code: 'INTERVAL', name: i18n('arr.fund.filterSettings.condition.interval')},
                    {values: 2, code: 'NOT_INTERVAL', name: i18n('arr.fund.filterSettings.condition.notInterval')},
                ]
                break
            case 'PARTY_REF':
            case 'RECORD_REF':
                items = [
                    {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'CONTAIN', name: i18n('arr.fund.filterSettings.condition.string.contain')},
                ]
                break
            case 'UNITDATE':
                items = [
                    {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                    {values: 1, code: 'LT', name: i18n('arr.fund.filterSettings.condition.unitdate.lt')},
                    {values: 1, code: 'GT', name: i18n('arr.fund.filterSettings.condition.unitdate.gt')},
                    {values: 1, code: 'SUBSET', name: i18n('arr.fund.filterSettings.condition.unitdate.subset')},
                    {values: 1, code: 'INTERSECT', name: i18n('arr.fund.filterSettings.condition.unitdate.intersect')},
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
        const {onSubmitForm, refType, dataType} = this.props

        var data = {
            values: selectedValueItems,
            valuesType: selectedValueItemsType,
            condition: conditionValues,
            conditionType: conditionSelectedCode,
        }

        if (refType.useSpecification || dataType.code === 'PACKET_REF') {
            data.specs = selectedSpecItems
            data.specsType = selectedSpecItemsType
        }

        // ##
        // # Test, zda není filtr prázdný
        // ##
        var outData = null

        if (data.valuesType === 'selected' || data.values.length > 0) {      // je zadáno filtrování podle hodnoty
            outData = data
        } else if ((refType.useSpecification || dataType.code === 'PACKET_REF') && (data.specsType === 'selected' || data.specs.length > 0)) {     // je zadáno filtrování podle specifikace
            outData = data
        } else if (data.conditionType !== 'NONE') { // je zadáno filtrování podle podmínky
            outData = data
        }

        onSubmitForm(outData)
    }

    render() {
        const {refType, onClose, dataType, packetTypes} = this.props
        const {conditionSelectedCode, conditionValues, valueItems, selectedValueItems, selectedValueItemsType, selectedSpecItems, selectedSpecItemsType} = this.state

        var specContent = null
        if (refType.useSpecification) {
            specContent = (
                <SimpleCheckListBox
                    ref='specsListBox'
                    items={refType.descItemSpecs}
                    label={i18n('arr.fund.filterSettings.filterBySpecification.title')}
                    value={{type: selectedSpecItemsType, ids: selectedSpecItems}}
                    onChange={this.handleSpecItemsChange}
                    />
            )
        } else if (dataType.code === 'PACKET_REF') { // u obalů budeme místo specifikací zobrazovat výběr typů obsalů
            specContent = (
                <SimpleCheckListBox
                    ref='specsListBox'
                    items={packetTypes.items}
                    label={i18n('arr.fund.filterSettings.filterByPacketType.title')}
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
