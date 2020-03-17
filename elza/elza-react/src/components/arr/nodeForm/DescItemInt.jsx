/**
 * Input prvek pro desc item - typ INT.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {normalizeDuration, normalizeInt} from 'components/validate.jsx';
import {decorateValue, inputValue} from './DescItemUtils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';
import {DisplayType} from '../../../constants.tsx';

class DescItemInt extends AbstractReactComponent {
    focus = () => {
        this.refs.focusEl.focus();
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
            return <DescItemLabel value={value} cal={cal} notIdentified={descItem.undefined} />;
        }

        let cls = [];
        if (cal) {
            cls.push('calculable');
        }

        return (
            <div className="desc-item-value">
                <ItemTooltipWrapper tooltipTitle="dataType.int.format">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref="focusEl"
                        type="text"
                        disabled={locked || descItem.undefined}
                        value={descItem.undefined ? i18n('subNodeForm.descItemType.notIdentified') : value}
                        onChange={this.handleChange}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemInt;
