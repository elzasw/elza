/**
 * Input prvek pro desc item - typ UNITID.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {normalizeString} from 'components/validate.jsx';
import {decorateValue, inputValue} from './DescItemUtils.jsx';
import {DescItemLabel} from './DescItemLabel';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';
import {CLS_CALCULABLE} from '../../../constants';
import {DescItemComponentProps} from './DescItemTypes';

const DescItemUnitid_MAX_LENGTH = 250;

type Props = DescItemComponentProps<string>;

class DescItemUnitid extends AbstractReactComponent<Props> {
    private readonly focusEl: React.RefObject<HTMLInputElement>;

    public constructor(props: Props) {
        super(props);
        this.focusEl = React.createRef();
    }

    public focus = () => this.focusEl.current?.focus();

    public render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value = cal && descItem.value == null ?
            i18n('subNodeForm.descItemType.calculable') :
            inputValue(descItem.value);

        if (readMode) {
            return <DescItemLabel value={value} cal={cal} isValueUndefined={descItem.undefined} />;
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
                        value={descItem.undefined ? i18n('subNodeForm.descItemType.undefinedValue') : value}
                        onChange={this.handleChange}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }

    private handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newValue = normalizeString(e.target.value, DescItemUnitid_MAX_LENGTH);

        if (newValue != this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    }
}

export default DescItemUnitid;
