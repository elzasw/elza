/**
 * Input prvek pro desc item - typ STRING.
 */

require ('./DescItemCoordinates.less')

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'
import {decorateValue} from './DescItemUtils'

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

        var decorate1 = decorateValue(this, descItem)
        var decorate2 = decorateValue(this, descItem)

        decorate1.className += ' part1';
        decorate2.className += ' part2';

        return (
            <div className='desc-item-value'>
                <input
                    {...decorate1}
                    type="text"
                    value={this.state.values.value1}
                    onChange={this.handleChange.bind(this, 0)}
                />
                <input
                    {...decorate2}
                    type="text"
                    value={this.state.values.value2}
                    onChange={this.handleChange.bind(this, 1)}
                />
            </div>
        )
    }
}

module.exports = connect()(DescItemCoordinates);

