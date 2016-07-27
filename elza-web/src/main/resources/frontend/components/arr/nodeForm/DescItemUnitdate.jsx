/**
 * Input prvek pro desc item - typ UNITDATE.
 */

require ('./DescItemUnitdate.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import DescItemLabel from './DescItemLabel.jsx'
import {indexById} from 'stores/app/utils.jsx'

var DescItemUnitdate = class DescItemUnitdate extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleValueChange', 'handleCalendarTypeChange', 'focus');
    }

    focus() {
        this.refs.focusEl.focus()
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
        const {descItem, locked, readMode, calendarTypes} = this.props;

        if (readMode) {
            let index = indexById(calendarTypes.items, descItem.calendarTypeId);
            if (index !== null) {
                let calendar = calendarTypes.items[index].name;
                return (
                    <DescItemLabel value={calendar + ": " + descItem.value}/>
                )
            } else {
                return (
                    <DescItemLabel value="" />
                )
            }
        }

        let tooltip = <Tooltip id='tt'>
                        <b>Formát datace</b><br />
                        Století: 20.st.<br />
                        Rok: 1968<br />
                        Rok/měsíc: 1968/8<br />
                        Datum: 21.8.1698<br />
                        Datum a čas: 21.8.1968 8:00<br />
                        <b>Intervaly</b><br />
                        Jednotlivá hodnota: 1968<br />
                        Interval: 21.8.1968 0:00-27.6.1989<br />
                        Polointerval 21.8.1968-<br />
                        <b>Odhad</b><br />
                        Definuje se uzavřením hodnoty do kulatých závorek: (16.8.1977)<br />
                      </Tooltip>

        return (
            <div className='desc-item-value desc-item-value-parts'>
                <select
                    {...decorateValue(this, descItem.hasFocus, descItem.error.calendarType, locked, ['calendar-select'])}
                    value={descItem.calendarTypeId}
                    onChange={this.handleCalendarTypeChange}
                >
                    <option />
                    {this.props.calendarTypes.items.map(calendarType => (
                        <option key={calendarType.id} value={calendarType.id}>{calendarType.name.charAt(0)}</option>
                    ))}
                </select>
                <OverlayTrigger
                        overlay={tooltip} placement="bottom"
                        >
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, ['unitdate-input'])}
                        ref='focusEl'
                        type="text"
                        value={descItem.value}
                        onChange={this.handleValueChange}
                    />
                </OverlayTrigger>
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemUnitdate);
