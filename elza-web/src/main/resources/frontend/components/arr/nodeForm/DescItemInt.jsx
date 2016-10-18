/**
 * Input prvek pro desc item - typ INT.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent, i18n} from 'components/index.jsx';
import {connect} from 'react-redux'
import {normalizeInt} from 'components/validate.jsx'
import {decorateValue} from './DescItemUtils.jsx'
import DescItemLabel from './DescItemLabel.jsx'

var DescItemInt = class DescItemInt extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChange', 'focus');
    }

    focus() {
        this.refs.focusEl.focus()
    }

    handleChange(e) {
        var newValue = normalizeInt(e.target.value);

        if (newValue != this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    }

    render() {
        const {descItem, locked, readMode, cal} = this.props;
        let value = cal && descItem.value == null ? i18n("subNodeForm.descItemType.calculable") : descItem.value;

        if (readMode) {
            return (
                <DescItemLabel value={value} cal={cal} />
            )
        }

        let cls = [];
        if (cal) {
            cls.push("calculable");
        }

        return (
            <div className='desc-item-value'>
                <input
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked, cls)}
                    ref='focusEl'
                    type="text"
                    disabled={locked}
                    value={value}
                    onChange={this.handleChange}
                />
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemInt);
