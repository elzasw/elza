/**
 * Nastavení podmínky na filtr - filtrování pol podmíky.
 */

import './FundFilterSettings.less';

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes.js';
import {AbstractReactComponent, i18n, FormInput} from 'components/shared';
import {Modal, Button} from 'react-bootstrap';
import {indexById, getMapFromList, getSetFromIdsList} from 'stores/app/utils.jsx'

var FundFilterCondition = class FundFilterCondition extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderValues', 'handleCodeChange', 'handleChangeValue')

        this.state = {
            values: props.values,
            errors: [],
        }
    }

    componentWillReceiveProps(nextProps) {
    }

    componentDidMount() {
    }

    handleCodeChange(e) {
        const {values, onChange} = this.props

        onChange(e.target.value, values)
    }

    handleChangeValue(index, value) {
        const {values, items, selectedCode, onChange, validateField, normalizeField} = this.props
        const {errors} = this.state
        const itemsCodeMap = getMapFromList(items, 'code')
        const selectedItem = itemsCodeMap[selectedCode]

        var newValues = [...values]

        // Normalizace
        var updatedValue = normalizeField ? normalizeField(selectedCode, selectedItem.values, value, index) : value
        newValues[index] = updatedValue

        // Validace
        var hasErrors = false
        var newErrors = [...errors]
        const error = validateField(selectedCode, selectedItem.values, updatedValue, index)
        if (error instanceof Promise) { // promise pro validaci - asi serverová validace
            hasErrors = true;   // nevíme, zda projde validace, raději nastavíme, že je chyba - aby nešel formulář uložit

            // Zavolání asynchronnní validace
            error
                .then(error => {
                    var {values, selectedCode} = this.props
                    var {errors} = this.state
                    var newErrors = [...errors]
                    newErrors[index] = error;

                    this.setState({errors: newErrors})

                    // Existují nějaké chyby?
                    var hasErrors = false
                    newErrors.forEach(err => {
                        if (err) {
                            hasErrors = true
                        }
                    })

                    // On change
                    onChange(selectedCode, values, hasErrors)
                })
                .catch(() => {})
        } else {    // vlastní validační hláška
            newErrors[index] = error;
        }

        this.setState({errors: newErrors})

        // Existují nějaké chyby?
        newErrors.forEach(err => {
            if (err) {
                hasErrors = true
            }
        })

        // On change
        onChange(selectedCode, newValues, hasErrors)
    }

    renderValues() {
        const {values, children, items, selectedCode} = this.props
        const {errors} = this.state
        const itemsCodeMap = getMapFromList(items, 'code')
        const selectedItem = itemsCodeMap[selectedCode]

        var valuesChildren = []
        var fields = []
        for (var a=0; a<selectedItem.values; a++) {
            fields.push({
                value: values[a],
                error: errors[a],
                onChange: this.handleChangeValue.bind(this, a)
            })
        }

        return this.props.renderFields(fields)
    }

    render() {
        const {items, selectedCode, className, label} = this.props
        const lbl = label ? <h4>{label}</h4> : null

        var cls = className ? className + ' filter-condition-container' : 'filter-condition-container'

        return (
            <div className={cls}>
                {lbl}
                <div className='inputs-container'>
                    <FormInput componentClass='select' onChange={this.handleCodeChange} value={selectedCode}>
                        {items.map(i => {
                            return (
                                <option key={i.code} value={i.code}>{i.name}</option>
                            )
                        })}
                    </FormInput>
                </div>
                <div className='vlaues-container'>
                    {this.renderValues()}
                </div>
            </div>
        )
    }
}

export default FundFilterCondition

