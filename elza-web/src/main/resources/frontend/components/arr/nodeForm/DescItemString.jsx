/**
 * Input prvek pro desc item - typ STRING.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {AbstractReactComponent} from 'components';
import {connect} from 'react-redux'

var DescItemString = class DescItemString extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleChange');

        this.state = {descItem: props.descItem};
    }

    componentWillReceiveProps(nextProps) {
        this.setState({descItem: nextProps.descItem});
    }

    validateText(value, maxLength) {
        if (value && value.length >= maxLength) {
            return "Moc dlouhy";
        }
    }

    normalizeText(prevValue, value) {
        return value;
    }

    handleChange(e) {
        var descItem = this.state.descItem;

        var newValue = e.target.value;
        newValue = this.normalizeText(descItem.value, newValue);
        var msg = this.validateText(newValue, 14);

        if (typeof msg !== 'undefined') {
            descItem.error = msg;
        } else {
            delete descItem.error;
        }

        descItem.value = newValue;

        this.setState({descItem: {...descItem}});
    }

    render() {
        const {descItem} = this.state;

        return (
            <div className='desc-item-value'>
                <input
                    className='form-control value'
                    type="text"
                    value={descItem.value}
                    onChange={this.handleChange}
                />
                {descItem.error && <div>ERR: {descItem.error}</div>}
            </div>
        )
    }
}

module.exports = connect()(DescItemString);
