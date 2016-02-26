/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
import {normalizeString} from 'components/validate'
import {decorateValue} from './DescItemUtils'

const DescItemString_MAX_LENGTH = 1000;

var DescItemString = class DescItemString extends AbstractReactComponent {
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

module.exports = connect(null, null, null, { withRef: true })(DescItemString);
