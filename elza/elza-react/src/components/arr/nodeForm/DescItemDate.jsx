/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {decorateAutocompleteValue} from './DescItemUtils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';

import './DescItemDate.scss';

import Moment from 'moment';
import {DateTimePicker} from 'react-widgets';
import {formatDate} from '../../validate';
import {CLS_CALCULABLE} from "../../../constants";

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
        let value = cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : descItem.value;

        if (readMode) {
            return (
                <DescItemLabel
                    value={Moment(descItem.value).format('l')}
                    cal={cal}
                    notIdentified={descItem.undefined}
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
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemDate;
