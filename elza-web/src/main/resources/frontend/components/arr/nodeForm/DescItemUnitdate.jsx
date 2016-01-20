/**
 * Input prvek pro desc item - typ UNITDATE.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'

var DescItemUnitdate = class DescItemUnitdate extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleValueChange', 'handleCalendarTypeChange');
    }

    handleValueChange(e) {
        var newValue = e.target.value;

        if (newValue != this.props.descItem.value) {
            this.props.onChange({
                calendarTypeId: this.props.descItem.calendarTypeId,
                value: newValue
            });
        }
    }

    handleCalendarTypeChange(e) {
        var newValue = e.target.value;

        if (newValue != this.props.descItem.calendarTypeId) {
            this.props.onChange({
                calendarTypeId: newValue,
                value: this.props.descItem.value
            });
        }
    }

    render() {
        const {descItem, locked} = this.props;

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <select
                    {...decorateValue(this, descItem.hasFocus, descItem.error.calendarType, locked, ['part1'])}
                    value={descItem.calendarTypeId}
                    onChange={this.handleCalendarTypeChange}
                >
                    <option />
                    {this.props.calendarTypes.items.map(calendarType => (
                        <option value={calendarType.id}>{calendarType.name}</option>
                    ))}
                </select>
                <input
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, ['part2'])}
                    type="text"
                    value={descItem.value}
                    onChange={this.handleValueChange}
                />
            </div>
        )
    }
}

module.exports = connect()(DescItemUnitdate);
