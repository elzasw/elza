/**
 * Nastavení podmínky na filtr - filtrování pol podmíky.
 */

require ('./FundFilterSettings.less')

import React from 'react';
import ReactDOM from 'react-dom';
import * as types from 'actions/constants/ActionTypes';
import {AbstractReactComponent, i18n} from 'components';
import {Modal, Button, Input} from 'react-bootstrap';
import {indexById, getMapFromList, getSetFromIdsList} from 'stores/app/utils.jsx'

var FundFilterCondition = class FundFilterCondition extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('renderValues', 'handleCodeChange', 'handleChangeValue')

        this.state = {
            values: props.values
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

    handleChangeValue(index, e) {
        const {values, selectedCode, onChange} = this.props

        var newValues = [...values]
        newValues[index] = e.target.value

        onChange(selectedCode, newValues)
    }

    renderValues() {
        const {values, children, items, selectedCode} = this.props
        const itemsCodeMap = getMapFromList(items, 'code')
        const selectedItem = itemsCodeMap[selectedCode]

        var valuesChildren = []
        var index = 0
        for (var a=0; a<selectedItem.values; a++) {
            const els = React.Children.map(children, el => {
                var result = React.cloneElement(el, {
                    value: values[index],
                    onChange: this.handleChangeValue.bind(this, index)
                })

                index++

                return result
            })
            valuesChildren.push(
                <div className='value-container'>
                    {els}
                </div>
            )
        }
        return valuesChildren
    }

    render() {
        const {items, selectedCode, className, label} = this.props
        const lbl = label ? <h4>{label}</h4> : null

        var cls = className ? className + ' filter-condition-container' : 'filter-condition-container'

        return (
            <div className={cls}>
                {lbl}
                <div className='inputs-container'>
                    <Input type='select' onChange={this.handleCodeChange} value={selectedCode}>
                        {items.map(i => {
                            return (
                                <option value={i.code}>{i.name}</option>
                            )
                        })}
                    </Input>
                </div>
                <div className='vlaues-container'>
                    {this.renderValues()}
                </div>
            </div>
        )
    }
}

module.exports = FundFilterCondition

