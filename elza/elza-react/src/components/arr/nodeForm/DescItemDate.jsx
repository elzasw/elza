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
import {DateTimePicker} from 'react-widgets';
import {formatDate} from '../../validate';
import {CLS_CALCULABLE} from "../../../constants";

const DATE_FORMAT = "DD.MM.RRRR";

class DescItemDate extends AbstractReactComponent {
    focusEl = null;
    focus() {
        this.focusEl && this.focusEl.focus();
    }

    handleChange = e => {
        const newValue = e == null ? null : formatDate(e);
        if (newValue !== null && newValue !== this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    };

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value = cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : Moment(descItem.value).format('l');

        if (readMode) {
            return (
                <DescItemLabel
                    value={value}
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
                    <DateTimePicker
                        ref={ref => (this.focusEl = ref)}
                        {...decorateAutocompleteValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        time={false}
                        value={value == null ? null : new Date(value)}
                        onChange={this.handleChange}
                        placeholder={DATE_FORMAT}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemDate;
