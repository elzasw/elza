import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {decorateValue} from './DescItemUtils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import {indexById} from 'stores/app/utils.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';

import './DescItemUnitdate.scss';
import {CLS_CALCULABLE} from "../../../constants";

/**
 * Input prvek pro desc item - typ UNITDATE.
 */
class DescItemUnitdate extends AbstractReactComponent {
    focusEl = null;
    focus = () => {
        this.focusEl.focus();
    };

    handleValueChange = e => {
        const newValue = e.target.value;

        if (newValue != this.props.descItem.value) {
            this.props.onChange({
                calendarTypeId: this.props.descItem.calendarTypeId,
                value: newValue,
            });
        }
    };

    handleCalendarTypeChange = e => {
        const newValue = e.target.value;

        if (newValue != this.props.descItem.calendarTypeId) {
            this.props.onChange({
                calendarTypeId: newValue,
                value: this.props.descItem.value,
            });
        }
    };

    render() {
        const {descItem, locked, readMode, calendarTypes, cal} = this.props;

        let value = cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : descItem.value;

        if (readMode) {
            let index = indexById(calendarTypes.items, descItem.calendarTypeId);
            if (index !== null) {
                let calendar = calendarTypes.items[index].name;
                return <DescItemLabel value={calendar + ': ' + value} cal={false} notIdentified={descItem.undefined} />;
            } else {
                return <DescItemLabel value="" cal={cal} notIdentified={descItem.undefined} />;
            }
        }

        let cls = ['unitdate-input'];
        if (cal) {
            cls.push(CLS_CALCULABLE);
        }

        return (
            <div className="desc-item-value desc-item-value-parts">
                <select
                    {...decorateValue(
                        this,
                        descItem.hasFocus,
                        descItem.error.calendarType,
                        locked || descItem.undefined,
                        ['calendar-select'],
                    )}
                    value={descItem.calendarTypeId}
                    onChange={this.handleCalendarTypeChange}
                >
                    <option />
                    {calendarTypes.items.map(type => (
                        <option key={type.id} value={type.id}>
                            {type.name.charAt(0)}
                        </option>
                    ))}
                </select>
                <ItemTooltipWrapper tooltipTitle="dataType.unitdate.format" style={{width: '100%'}}>
                    <input
                        {...decorateValue(
                            this,
                            descItem.hasFocus,
                            descItem.error.value,
                            locked || descItem.undefined,
                            cls,
                        )}
                        ref={ref => (this.focusEl = ref)}
                        type="text"
                        value={descItem.undefined ? i18n('subNodeForm.descItemType.notIdentified') : value || ''}
                        onChange={this.handleValueChange}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemUnitdate;
