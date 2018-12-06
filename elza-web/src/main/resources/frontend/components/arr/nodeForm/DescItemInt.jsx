/**
 * Input prvek pro desc item - typ INT.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux';
import {AbstractReactComponent, i18n} from 'components/shared';
import {Checkbox} from 'react-bootstrap'
import {normalizeInt, fromDuration, toDuration, normalizeDuration} from 'components/validate.jsx'
import {decorateValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'
import ItemTooltipWrapper from "./ItemTooltipWrapper.jsx";
import {DisplayType} from "../../../constants.tsx";

class DescItemInt extends AbstractReactComponent {

    focus = () => {
        this.refs.focusEl.focus()
    };

    handleChange = (e) => {
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
        const {refType:{viewDefinition}} = this.props;
        return viewDefinition ? viewDefinition : DisplayType.NUMBER;
    };

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        const value = cal && descItem.value == null ? i18n("subNodeForm.descItemType.calculable") : descItem.value;

        if (readMode) {
            return (
                <DescItemLabel value={value} cal={cal} notIdentified={descItem.undefined} />
            )
        }

        let cls = [];
        if (cal) {
            cls.push("calculable");
        }

        return (
            <div className='desc-item-value'>
                <ItemTooltipWrapper tooltipTitle="dataType.int.format">
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
        )
    }
}

export default connect(null, null, null, {withRef: true})(DescItemInt);
