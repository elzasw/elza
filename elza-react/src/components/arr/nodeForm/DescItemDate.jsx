/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {decorateAutocompleteValue} from './DescItemUtils.jsx';
import {DescItemLabel} from './DescItemLabel';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';

import './DescItemDate.scss';

import Moment from 'moment';
import {DateTimePicker, Localization} from 'react-widgets';
import MomentLocalizer from 'react-widgets-moment';
import {formatDateIso} from '../../validate';

const momentLocalizer = new MomentLocalizer(Moment);
import {CLS_CALCULABLE} from "../../../constants";

const DATE_FORMAT = "DD.MM.RRRR";

class DescItemDate extends AbstractReactComponent {
    focusEl = null;
    focus() {
        this.focusEl && this.focusEl.focus();
    }

    handleChange = e => {
        const newValue = e == null ? null : formatDateIso(e);
        if (newValue !== null && newValue !== this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    };

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        const isCalculated = cal && descItem.value == null;
        const dateValue = descItem.value == null ? null : Moment(descItem.value, 'YYYY-MM-DD').toDate();

        if (readMode) {
            const displayValue = isCalculated
                ? i18n('subNodeForm.descItemType.calculable')
                : descItem.value == null
                    ? null
                    : Moment(descItem.value, 'YYYY-MM-DD').format('DD. MM. YYYY');
            return (
                <DescItemLabel
                    value={displayValue}
                    cal={cal}
                    isValueUndefined={descItem.undefined}
                    isValueInhibited={descItem.inhibited}
                />
            );
        }

        let cls = [];
        if (cal) {
            cls.push(CLS_CALCULABLE);
        }

        return (
            <div className="desc-item-value">
                <ItemTooltipWrapper tooltipTitle="dataType.date.format">
                    <Localization date={momentLocalizer}>
                        <DateTimePicker
                            ref={ref => (this.focusEl = ref)}
                            {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                            time={false}
                            value={dateValue}
                            onChange={this.handleChange}
                            disabled={isCalculated}
                            placeholder={isCalculated ? i18n('subNodeForm.descItemType.calculable') : DATE_FORMAT}
                            valueDisplayFormat="DD. MM. YYYY"
                            valueEditFormat="DD.MM.YYYY"
                        />
                    </Localization>
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemDate;
