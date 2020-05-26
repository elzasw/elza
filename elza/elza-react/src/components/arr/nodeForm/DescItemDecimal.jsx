/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import {AbstractReactComponent, i18n} from 'components/shared';
import {normalizeDouble} from 'components/validate.jsx';
import {decorateValue} from './DescItemUtils.jsx';
import DescItemLabel from './DescItemLabel.jsx';
import ItemTooltipWrapper from './ItemTooltipWrapper.jsx';

class DescItemDecimal extends AbstractReactComponent {
    focusEl = null;

    constructor(props) {
        super(props);

        this.bindMethods('handleChange', 'focus');

        this.state = {value: this.convertToClient(props.descItem.value)};
    }

    UNSAFE_componentWillReceiveProps(nextProps) {
        this.setState({value: this.convertToClient(nextProps.descItem.value)});
    }

    convertToClient(value) {
        if (typeof value !== 'undefined' && value !== '' && value !== null) {
            return ('' + value).replace(/(\.)/, ',');
        } else {
            return '';
        }
    }

    convertFromClient(value) {
        if (typeof value !== 'undefined' && value !== '' && value !== null) {
            return value.replace(/(,)/, '.');
        } else {
            return '';
        }
    }

    focus() {
        this.focusEl.focus();
    }

    handleChange(e) {
        const newValue = normalizeDouble(e.target.value);
        const value = this.convertFromClient(newValue);
        let prevValue;

        if (
            typeof this.props.descItem.value !== 'undefined' &&
            this.props.descItem.value !== '' &&
            this.props.descItem.value !== null
        ) {
            prevValue = '' + this.props.descItem.value;
        } else {
            prevValue = '';
        }

        if (value !== prevValue) {
            this.props.onChange(value);
        }
    }

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value = cal && descItem.value == null ? i18n('subNodeForm.descItemType.calculable') : descItem.value;

        if (readMode) {
            return <DescItemLabel value={value} cal={cal} notIdentified={descItem.undefined} />;
        }

        let cls = [];
        if (cal) {
            cls.push('calculable');
        }

        value = cal && this.state.value === '' ? value : this.state.value;

        return (
            <div className="desc-item-value">
                <ItemTooltipWrapper tooltipTitle="dataType.decimal.format">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref={ref => this.focusEl = ref}
                        type="text"
                        disabled={locked || descItem.undefined}
                        onChange={this.handleChange}
                        value={descItem.undefined ? i18n('subNodeForm.descItemType.notIdentified') : value}
                    />
                </ItemTooltipWrapper>
            </div>
        );
    }
}

export default DescItemDecimal;
