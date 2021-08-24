import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {decorateValue} from './DescItemUtils';
import DescItemLabel from './DescItemLabel';
import ItemTooltipWrapper from './ItemTooltipWrapper';

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
                value: newValue,
            });
        }
    };

    render() {
        const {descItem, locked, readMode, cal} = this.props;

        let value = cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : descItem.value;

        if (readMode) {
            if (value !== null) {
                return <DescItemLabel value={value} cal={false} notIdentified={descItem.undefined} />;
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
