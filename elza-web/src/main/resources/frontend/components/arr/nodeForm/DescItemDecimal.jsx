/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
var classNames = require('classnames');
import {normalizeDouble} from 'components/validate'

var DescItemDecimal = class DescItemDecimal extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChange');
    }

    handleChange(e) {
        var newValue = normalizeDouble(e.target.value);

        if (newValue != this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    }

    render() {
        const {descItem, locked} = this.props;

        var cls = classNames({
            'form-control': true,
            value: true,
            error: descItem.error,
            active: descItem.hasFocus,
        });

        return (
            <div className='desc-item-value'>
                <input
                    className={cls}
                    type="text"
                    disabled={locked}
                    value={descItem.value}
                    title={descItem.error}
                    onChange={this.handleChange}
                    onFocus={() => this.props.onFocus()}
                    onBlur={() => this.props.onBlur()}
                />
            </div>
        )
    }
}

module.exports = connect()(DescItemDecimal);
