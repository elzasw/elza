/**
 * Input prvek pro desc item - typ UNITID.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {normalizeString} from 'components/validate.jsx';
import {decorateValue, inputValue} from './DescItemUtils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';
import {CLS_CALCULABLE} from '../../../constants';

const DescItemUnitid_MAX_LENGTH = 250;

class DescItemUnitid extends AbstractReactComponent {
    private readonly focusEl: React.RefObject<HTMLInputElement>;

    constructor(props) {
        super(props);
        this.focusEl = React.createRef();
    }

    focus() {
        this.focusEl.current?.focus();
    }

    handleChange(e) {
        const newValue = normalizeString(e.target.value, DescItemUnitid_MAX_LENGTH);

        if (newValue != this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    }

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value =
            cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : inputValue(descItem.value);

        if (readMode) {
            return <DescItemLabel value={value} cal={cal} notIdentified={descItem.undefined} />;
        }

        let cls: string[] = [];
        if (cal) {
            cls.push(CLS_CALCULABLE);
        }

        return (
            <div className="desc-item-value">
                <ItemTooltipWrapper tooltipTitle="dataType.unitid.format">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref={this.focusEl}
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

export default DescItemUnitid;
