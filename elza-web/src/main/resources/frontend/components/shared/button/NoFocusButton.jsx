import React from 'react';
import {AbstractReactComponent} from 'components';

var NoFocusButton = class NoFocusButton extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className='btn btn-default' onClick={this.props.onClick}>{this.props.children}</div>
        )
    }
}

module.exports = NoFocusButton;
