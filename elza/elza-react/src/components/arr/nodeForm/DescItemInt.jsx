/**
 * Input prvek pro desc item - typ INT.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {normalizeDuration, normalizeInt} from 'components/validate.jsx';
import {decorateValue, inputValue} from './DescItemUtils.jsx';
import {DescItemLabel} from './DescItemLabel';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';
import {CLS_CALCULABLE, DisplayType} from '../../../constants.tsx';

class DescItemInt extends AbstractReactComponent {

    focusEl = null;

    focus = () => {
        this.focusEl.focus();
    };

    handleChange = e => {
        let newValue;

        if (this.getDisplayType() === DisplayType.DURATION) {
            newValue = normalizeDuration(e.target.value);
        } else {
            newValue = normalizeInt(e.target.value);
        }

        if (newValue != this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    };

    getDisplayType = () => {
        const {
            refType: {viewDefinition},
        } = this.props;
        return viewDefinition ? viewDefinition : DisplayType.NUMBER;
    };

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        const value =
            cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : inputValue(descItem.value);

        if (readMode) {
            return <DescItemLabel value={value} cal={cal} isValueUndefined={descItem.undefined} />;
        }

        let cls = [];
        if (cal) {
            cls.push(CLS_CALCULABLE);
        }

        return (
            <div className="desc-item-value">
                <ItemTooltipWrapper tooltipTitle="dataType.int.format">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref={ref => this.focusEl = ref}
                        type="text"
                        disabled={locked || descItem.undefined}
                        value={descItem.undefined ? i18n('subNodeForm.descItemType.undefinedValue') : value}
                        onChange={this.handleChange}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemInt;
