import React from 'react';
import {AbstractReactComponent} from 'components/shared';
import { injectIntl } from 'react-intl';
import { nodeMessages } from 'components/arr/nodeMessages';

import {decorateValue} from './DescItemUtils';
import DescItemLabel from './DescItemLabel';

import './DescItemUnitdate.scss';
import {CLS_CALCULABLE} from "../../../constants";
import UnitdateField from 'components/registry/field/UnitdateField';

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

        let value = cal && descItem.value == null ? this.props.intl.formatMessage(nodeMessages.subNodeFormDescItemTypeCalculable) : descItem.value;

        if (readMode) {
            return <DescItemLabel
                value={value || ""} cal={cal}
                isValueUndefined={descItem.undefined}
                isValueInhibited={descItem.inhibited}
            />;
        }

        let cls = ['unitdate-input'];
        if (cal) {
            cls.push(CLS_CALCULABLE);
        }

        return (
            <div className="desc-item-value desc-item-value-parts">
                <UnitdateField
                    {...decorateValue(
                        this,
                        descItem.hasFocus,
                        descItem.error.value,
                        locked || descItem.undefined,
                        cls,
                    )}
                    ref={ref => (this.focusEl = ref)}
                    type="text"
                    value={descItem.undefined ? this.props.intl.formatMessage(nodeMessages.subNodeFormDescItemTypeUndefinedValue) : value || ''}
                    onChange={this.handleValueChange}
                />
            </div>
        );
    }
}

export default injectIntl(DescItemUnitdate);
