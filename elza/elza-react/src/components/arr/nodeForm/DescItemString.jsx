/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n} from 'components/shared';
import {connect} from 'react-redux'
import {normalizeString} from 'components/validate.jsx'
import {decorateValue, inputValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";

const DescItemString_MAX_LENGTH = 1000;

const DescItemString = class DescItemString extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChange', 'focus');
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleChange(e) {
        var newValue = normalizeString(e.target.value, DescItemString_MAX_LENGTH);

        if (newValue != this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    }

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value = cal && descItem.value == null ? i18n("subNodeForm.descItemType.calculable") : inputValue(descItem.value);

        if (readMode) {
            return (
                <DescItemLabel value={descItem.value} cal={cal} notIdentified={descItem.undefined} />
            )
        }

        let cls = [];
        if (cal) {
            cls.push("calculable");
        }

        return (
            <div className='desc-item-value'>
                <ItemTooltipWrapper tooltipTitle="dataType.string.format">
                    <input
                        {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                        ref='focusEl'
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

export default DescItemString;
