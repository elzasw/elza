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
    }

    render() {
        const {descItem} = this.props;

        return (
            <div className='desc-item-value'>
                <textarea
                    {...decorateValue(this, descItem.hasFocus, descItem.error.value)}
                    type="text"
                    value={descItem.value}
                    onChange={(e) => this.props.onChange(e.target.value)}
                />
            </div>
        )
    }
}

module.exports = connect()(DescItemText);
