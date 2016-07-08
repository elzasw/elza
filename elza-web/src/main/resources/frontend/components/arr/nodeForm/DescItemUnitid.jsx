/**
 * Input prvek pro desc item - typ UNITID.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components/index.jsx';
import {connect} from 'react-redux'
import {normalizeString} from 'components/validate.jsx'
import {decorateValue} from './DescItemUtils.jsx'

const DescItemString_MAX_LENGTH = 250;

var DescItemUnitid = class DescItemUnitid extends AbstractReactComponent {
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

module.exports = connect(null, null, null, { withRef: true })(DescItemUnitid);
