/**
 * Input prvek pro desc item - typ INT.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
import {normalizeInt} from 'components/validate'
import {decorateValue} from './DescItemUtils'

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
        const {descItem, locked} = this.props;
        return (
            <div className='desc-item-value'>
                <input
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                    ref='focusEl'
                    type="text"
                    disabled={locked}
                    value={descItem.value}
                    onChange={this.handleChange}
                />
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemInt);
