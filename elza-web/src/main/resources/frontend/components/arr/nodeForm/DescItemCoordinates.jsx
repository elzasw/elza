/**
 * Input prvek pro desc item - typ STRING.
 */

require ('./DescItemCoordinates.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
var classNames = require('classnames');

var DescItemCoordinates = class DescItemCoordinates extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChange');

        this.state = {
            values: this.splitValue(props.descItem.value)
        }
    }

    componentWillReceiveProps(nextProps) {
        this.setState({
            values: this.splitValue(nextProps.descItem.value)
        })
    }

    handleChange(valueIndex, e) {
        var newValues = {...this.state.values};

        switch (valueIndex) {
            case 0:
                newValues.value1 = e.target.value;
                break;
            case 1:
                newValues.value2 = e.target.value;
                break;
        }

        var newValue = "x=" + newValues.value1 + "&y=" + newValues.value2;
        if (newValue != this.props.descItem.value) {
            this.props.onChange(newValue);
        }
    }

    splitValue(value) {
        var values = value.split("&");
        
        var result = {
            value1: values[0],
            value2: values[1],
        }

        if (result.value1.startsWith("x=")) {
            result.value1 = result.value1.substring(2);
        }
        if (result.value2.startsWith("y=")) {
            result.value2 = result.value2.substring(2);
        }

        return result;
    }

    render() {
        const {descItem} = this.props;

        var cls1 = classNames({
            part1: true,
            'form-control': true,
            value: true,
            error: descItem.error,
            active: descItem.hasFocus,
        });
        var cls2 = classNames({
            part2: true,
            'form-control': true,
            value: true,
            error: descItem.error,
            active: descItem.hasFocus,
        });

        return (
            <div className='desc-item-value'>
                <input
                    className={cls1}
                    type="text"
                    value={this.state.values.value1}
                    title={descItem.error}
                    onChange={this.handleChange.bind(this, 0)}
                    onFocus={() => this.props.onFocus()}
                    onBlur={() => this.props.onBlur()}
                />
                <input
                    className={cls2}
                    type="text"
                    value={this.state.values.value2}
                    title={descItem.error}
                    onChange={this.handleChange.bind(this, 1)}
                    onFocus={() => this.props.onFocus()}
                    onBlur={() => this.props.onBlur()}
                />
            </div>
        )
    }
}

module.exports = connect()(DescItemCoordinates);

