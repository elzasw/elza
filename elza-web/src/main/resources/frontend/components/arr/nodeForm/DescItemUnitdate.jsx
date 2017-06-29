import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n} from 'components/shared';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils.jsx'
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import DescItemLabel from './DescItemLabel.jsx'
import {indexById} from 'stores/app/utils.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

import './DescItemUnitdate.less'

/**
 * Input prvek pro desc item - typ UNITDATE.
 */
class DescItemUnitdate extends AbstractReactComponent {

    focus = () => {
        this.refs.focusEl.focus()
    };

    handleValueChange = (e) => {
        const newValue = e.target.value;

        if (newValue != this.props.descItem.value) {
            this.props.onChange({
                calendarTypeId: this.props.descItem.calendarTypeId,
                value: newValue
            });
        }
    };

    handleCalendarTypeChange = (e) => {
        const newValue = e.target.value;

        if (newValue != this.props.descItem.calendarTypeId) {
            this.props.onChange({
                calendarTypeId: newValue,
                value: this.props.descItem.value
            });
        }
    };

    render() {
        const {descItem, locked, readMode, calendarTypes, cal} = this.props;

        let value = cal && descItem.value == null ? i18n("subNodeForm.descItemType.calculable") : descItem.value;

        if (readMode) {
            let index = indexById(calendarTypes.items, descItem.calendarTypeId);
            if (index !== null) {
                let calendar = calendarTypes.items[index].name;
                return <DescItemLabel value={calendar + ": " + value} cal={false}/>
            } else {
                return <DescItemLabel value="" cal={cal} />
            }
        }

        let cls = ['unitdate-input'];
        if (cal) {
            cls.push("calculable");
        }

        return <div className='desc-item-value desc-item-value-parts'>
            <select
                {...decorateValue(this, descItem.hasFocus, descItem.error.calendarType, locked, ['calendar-select'])}
                value={descItem.calendarTypeId}
                onChange={this.handleCalendarTypeChange}
            >
                <option />
                {calendarTypes.items.map(type => <option key={type.id} value={type.id}>{type.name.charAt(0)}</option>)}
            </select>
            <ItemTooltipWrapper tooltipTitle="dataType.unitdate.format">
                <input
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                    ref='focusEl'
                    type="text"
                    value={value}
                    onChange={this.handleValueChange}
                />
            </ItemTooltipWrapper>
        </div>
    }
}

export default connect(null, null, null, { withRef: true })(DescItemUnitdate);
