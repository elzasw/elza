/**
 * Formulář nastavení filtru na sloupečku.
 */

require ('./FundFilterSettings.less')

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {reduxForm} from 'redux-form';
import {FilterableListBox, AbstractReactComponent, i18n} from 'components/index.jsx';
import DescItemUnitdate from './nodeForm/DescItemUnitdate.jsx'
import DescItemCoordinates from './nodeForm/DescItemCoordinates.jsx'
import {Modal, Button, Input} from 'react-bootstrap';
import {WebApi} from 'actions/index.jsx';
import {hasDescItemTypeValue} from 'components/arr/ArrUtils.jsx'
import {FILTER_NULL_VALUE} from 'actions/arr/fundDataGrid.jsx'
import {normalizeInt, normalizeDouble, validateInt, validateDouble, validateCoordinatePoint} from 'components/validate.jsx';
import {getMapFromList} from 'stores/app/utils.jsx'
import {objectFromWKT, wktFromTypeAndData} from 'components/Utils.jsx';

const FundFilterCondition = require('./FundFilterCondition')
const SimpleCheckListBox = require('./SimpleCheckListBox')

var _ffs_validateTimer
var _ffs_prevReject = null

const renderTextFields = (fields) => {
    return fields.map((field, index) => {
        var decorate
        if (field.error) {
            decorate = {
                bsStyle: 'error',
                hasFeedback: true,
                help: field.error
            }
        }

        return (
            <div key={index} className='value-container'>
                <Input {...decorate} type="text" value={field.value} onChange={(e) => field.onChange(e.target.value)} />
            </div>
        )
    })
}

const renderCoordinatesFields = (fields) => {
    switch (fields.length) {
        case 0:
            return null
        case 1:
            var descItem = {
                hasFocus: false,
                value: typeof fields[0].value !== 'undefined' ? fields[0].value : '',
                error: {value: fields[0].error},
            }
            return (
                <div key={0} className='value-container'>
                    <DescItemCoordinates
                        onChange={fields[0].onChange}
                        descItem={descItem}
                        onFocus={()=>{}}
                        onBlur={()=>{}}
                        />
                    {false && <Input type="text" value={fields[0].value} onChange={(e) => fields[0].onChange(e.target.value)} />}
                </div>
            )
        case 2:
            var vals = []
            var descItem = {
                hasFocus: false,
                value: typeof fields[0].value !== 'undefined' ? fields[0].value : '',
                error: {},
            }
            vals.push(
                <div key={0} className='value-container'>
                    <DescItemCoordinates
                        onChange={fields[0].onChange}
                        descItem={descItem}
                        onFocus={()=>{}}
                        onBlur={()=>{}}
                    />
                </div>
            )
            vals.push(
                <div key={1} className='value-container'>
                    <Input type="select" defaultValue={10000} value={fields[1].value} onChange={(e) => fields[1].onChange(e.target.value)}>
                        {[100, 500, 1000, 10000, 20000, 50000, 100000].map(l => {
                            return <option key={l} value={l}>{i18n('arr.fund.filterSettings.condition.coordinates.near.' + l)}</option>
                        })}
                    </Input>
                </div>
            )
            return vals
    }
}

const renderUnitdateFields = (calendarTypes, fields) => {
    switch (fields.length) {
        case 0:
            return null
        case 2:
            var vals = []
            vals.push(
                <div key={0} className='value-container'>
                    <Input type="select"
                        value={typeof fields[0].value !== 'undefined' ? fields[0].value : 1}
                        onChange={(e) => {fields[0].onChange(e.target.value);}}
                    >
                        {calendarTypes.items.map(calendarType => (
                            <option key={calendarType.id} value={calendarType.id}>{calendarType.name}</option>
                        ))}
                    </Input>
                </div>
            )
            vals.push(
                <div key={1} className='value-container'>
                    <Input type="text"
                        value={fields[1].value}
                        onChange={(e) => {fields[1].onChange(e.target.value);}}
                   />
                </div>
            )
        return vals
    }
}

var FundFilterSettings = class FundFilterSettings extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('callValueSearch', 'handleValueSearch',
            'handleValueItemsChange', 'renderConditionFilter', 'handleSpecItemsChange', 'handleConditionChange',
            'handleSubmit', 'renderValueFilter', 'getConditionInfo')

        var state = {
            valueItems: [],
            valueSearchText: '',
            selectedValueItems: [],
            selectedValueItemsType: 'unselected',
            selectedSpecItems: [],
            selectedSpecItemsType: 'unselected',
            conditionSelectedCode: 'NONE',
            conditionValues: [],
            conditionHasErrors: false,
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
            // Ladění objektu pro server
            var useSpecIds = specIds.map(id => {
                return id === FILTER_NULL_VALUE ? null : id
            })

            WebApi.getDescItemTypeValues(versionId, refType.id, valueSearchText, useSpecIds, 200)
                .then(json => {
                    var valueItems = json.map(i => ({id: i, name: i}))

                    // TODO [stanekpa] Toto zde nebude, když se na server přidělá podpora na vracení a hledání NULL hodnot - problé je ale v locales (řetězec arr.fund.filterSettings.value.empty), měly by se doplnit i na server
                    if (valueSearchText == '' || i18n('arr.fund.filterSettings.value.empty').toLowerCase().indexOf(valueSearchText) !== -1) {   // u prázdného hledání a případně u hledání prázdné hodnoty doplňujeme null položku
                        valueItems = [{id: FILTER_NULL_VALUE, name: i18n('arr.fund.filterSettings.value.empty')}, ...valueItems]
                    }

                    this.setState({
                        valueItems: valueItems,
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

    handleConditionChange(selectedCode, values, hasErrors) {
        const {dataType} = this.props
        var useValues = [...values]

        // Inicializace implicitních hodnot, musí být i u input prvků v render metodě
        switch (dataType.code) {
            case 'TEXT':
            case 'STRING':
            case 'FORMATTED_TEXT':
            case 'UNITID':
            case 'INT':
            case 'DECIMAL':
            case 'PARTY_REF':
            case 'PACKET_REF':
            case 'JSON_TABLE':
            case 'ENUM':
            case 'RECORD_REF':
                break
            case 'UNITDATE':
                if (useValues.length > 0) {
                    if (!useValues[0]) {
                        useValues[0] = 1
                    }
                    if (!useValues[1]) {
                        useValues[1] = ''
                    }
                }
                break
            case 'COORDINATES':
                if (selectedCode === 'NEAR' && !useValues[1]) {
                    useValues[1] = 10000
                }
                break
        }

        this.setState({
            conditionSelectedCode: selectedCode,
            conditionValues: useValues,
            conditionHasErrors: hasErrors,
        })
    }

    renderValueFilter() {
        const {refType, dataType} = this.props
        const {valueItems, selectedValueItems, selectedValueItemsType} = this.state

        if (!hasDescItemTypeValue(dataType)) {
            return null
        }

        if (dataType.code === 'UNITDATE' || dataType.code === 'TEXT' || dataType.code === 'COORDINATES') { // zde je výjimka a nechceme dle hodnoty
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

    getConditionInfo() {
        const {dataType, calendarTypes} = this.props

        let renderFields
        let validateField
        let normalizeField
        var items = []
        switch (dataType.code) {
            case 'TEXT':
            case 'STRING':
            case 'FORMATTED_TEXT':
            case 'UNITID':
                renderFields = renderTextFields
                validateField = (code, valuesCount, value, index) => {
                    return value ? null : i18n('global.validation.required')
                }
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
                renderFields = renderTextFields
                normalizeField = (code, valuesCount, value, index) => {
                    return dataType.code === 'INT' ? normalizeInt(value) : normalizeDouble(value)
                }
                validateField = (code, valuesCount, value, index) => {
                    if (!value) return i18n('global.validation.required')
                    return dataType.code === 'INT' ? validateInt(value) : validateDouble(value)
                }
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
                renderFields = renderTextFields
                validateField = (code, valuesCount, value, index) => {
                    return value ? null : i18n('global.validation.required')
                }
                items = [
                    {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'CONTAIN', name: i18n('arr.fund.filterSettings.condition.string.contain')},
                ]
                break
            case 'UNITDATE':
                renderFields = renderUnitdateFields.bind(this, calendarTypes)
                validateField = (code, valuesCount, value, index) => {
                    return new Promise(function (resolve, reject) {
                        if (_ffs_validateTimer) {
                            clearTimeout(_ffs_validateTimer)
                            if (_ffs_prevReject) {
                                _ffs_prevReject()
                                _ffs_prevReject = null
                            }
                        }
                        _ffs_prevReject = reject
                        var fc = () => {
                            WebApi.validateUnitdate(value)
                                .then(json => {
                                    resolve(json.message)
                                })
                        }
                        _ffs_validateTimer = setTimeout(fc, 250);
                    })
                }                
                items = [
                    {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 2, code: 'EQ', name: i18n('arr.fund.filterSettings.condition.eq')},
                    {values: 2, code: 'LT', name: i18n('arr.fund.filterSettings.condition.unitdate.lt')},
                    {values: 2, code: 'GT', name: i18n('arr.fund.filterSettings.condition.unitdate.gt')},
                    {values: 2, code: 'SUBSET', name: i18n('arr.fund.filterSettings.condition.unitdate.subset')},
                    {values: 2, code: 'INTERSECT', name: i18n('arr.fund.filterSettings.condition.unitdate.intersect')},
                ]
                break
            case 'COORDINATES':
                renderFields = renderCoordinatesFields
                validateField = (code, valuesCount, value, index) => {
                    return validateCoordinatePoint(value)
                }
                items = [
                    {values: 0, code: 'NONE', name: i18n('arr.fund.filterSettings.condition.none')},
                    {values: 0, code: 'EMPTY', name: i18n('arr.fund.filterSettings.condition.empty')},
                    {values: 0, code: 'NOT_EMPTY', name: i18n('arr.fund.filterSettings.condition.notEmpty')},
                    {values: 1, code: 'SUBSET', name: i18n('arr.fund.filterSettings.condition.coordinates.subset')},
                    {values: 2, code: 'NEAR', name: i18n('arr.fund.filterSettings.condition.coordinates.near')},
                ]
                break
            case 'PACKET_REF':
            case 'JSON_TABLE':
            case 'ENUM':
                break
        }

        return {
            renderFields,
            validateField,
            normalizeField,
            items,
        }
    }

    renderConditionFilter() {
        const {refType, dataType, calendarTypes} = this.props
        const {conditionSelectedCode, conditionValues} = this.state

        if (!hasDescItemTypeValue(dataType)) {
            return null
        }

        const info = this.getConditionInfo()

        if (info.items.length === 0) {
            return null
        }

        return (
            <FundFilterCondition
                className='filter-content-container'
                label={i18n('arr.fund.filterSettings.filterByCondition.title')}
                selectedCode={conditionSelectedCode}
                values={conditionValues}
                onChange={this.handleConditionChange}
                items={info.items}
                renderFields={info.renderFields}
                validateField={info.validateField}
                normalizeField={info.normalizeField}
            />
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
        const {conditionHasErrors, conditionSelectedCode, conditionValues, selectedSpecItems, selectedSpecItemsType} = this.state

        var specContent = null
        if (refType.useSpecification) {
            var items = [{id: FILTER_NULL_VALUE, name: i18n('arr.fund.filterSettings.value.empty')}, ...refType.descItemSpecs]

            specContent = (
                <SimpleCheckListBox
                    ref='specsListBox'
                    items={items}
                    label={i18n('arr.fund.filterSettings.filterBySpecification.title')}
                    value={{type: selectedSpecItemsType, ids: selectedSpecItems}}
                    onChange={this.handleSpecItemsChange}
                    />
            )
        } else if (dataType.code === 'PACKET_REF') { // u obalů budeme místo specifikací zobrazovat výběr typů obsalů
            var items = [{id: FILTER_NULL_VALUE, name: i18n('arr.fund.filterSettings.value.empty')}, ...packetTypes.items]

            specContent = (
                <SimpleCheckListBox
                    ref='specsListBox'
                    items={items}
                    label={i18n('arr.fund.filterSettings.filterByPacketType.title')}
                    value={{type: selectedSpecItemsType, ids: selectedSpecItems}}
                    onChange={this.handleSpecItemsChange}
                    />
            )
        }
        
        var valueContent = this.renderValueFilter()

        var conditionContent = this.renderConditionFilter()

        var hasAllValues = true
        if (hasDescItemTypeValue(dataType)) {
            const info = this.getConditionInfo()
            if (info.items.length > 0) {
                const itemsCodeMap = getMapFromList(info.items, 'code')
                const selectedItem = itemsCodeMap[conditionSelectedCode]

                for (var a=0; a<selectedItem.values; a++) {
                    if (!conditionValues[a]) {
                        hasAllValues = false
                    }
                }
            }
        }

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
                    <Button disabled={conditionHasErrors || !hasAllValues} onClick={this.handleSubmit}>{i18n('global.action.store')}</Button>
                    <Button bsStyle="link" onClick={onClose}>{i18n('global.action.cancel')}</Button>
                </Modal.Footer>
            </div>
        )
    }
}

module.exports = FundFilterSettings
