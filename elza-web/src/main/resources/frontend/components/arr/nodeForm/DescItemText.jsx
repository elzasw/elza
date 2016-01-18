/**
 * Input prvek pro desc item - typ STRING.
 */

require ('./DescItemText.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
var classNames = require('classnames');

var DescItemText = class DescItemText extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        const {descItem} = this.props;

        var cls = classNames({
            'form-control': true,
            value: true,
            error: descItem.error,
            active: descItem.hasFocus,
        });

        return (
            <div className='desc-item-value'>
                <textarea
                    className={cls}
                    type="text"
                    value={descItem.value}
                    title={descItem.error}
                    onChange={(e) => this.props.onChange(e.target.value)}
                    onFocus={() => this.props.onFocus()}
                    onBlur={() => this.props.onBlur()}
                />
            </div>
        )
    }
}

module.exports = connect()(DescItemText);
