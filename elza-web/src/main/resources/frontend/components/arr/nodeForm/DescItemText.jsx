/**
 * Input prvek pro desc item - typ STRING.
 */

require ('./DescItemText.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'

var DescItemText = class DescItemText extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('focus')
    }

    focus() {
        this.refs.focusEl.focus()
    }

    render() {
        const {descItem, locked} = this.props;

        return (
            <div className='desc-item-value'>
                <textarea
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value, locked)}
                    ref='focusEl'
                    type="text"
                    disabled={locked}
                    value={descItem.value}
                    onChange={(e) => this.props.onChange(e.target.value)}
                />
            </div>
        )
    }
}

module.exports = connect(null, null, null, { withRef: true })(DescItemText);
