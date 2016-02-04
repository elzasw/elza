import React from 'react';
import {AbstractReactComponent} from 'components';

var NoFocusButton = class NoFocusButton extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        var cls = 'btn btn-default';
        if (this.props.disabled) {
            cls += ' disabled';
        }

        return (
            <div title={this.props.title} className={cls} onClick={this.props.onClick}>{this.props.children}</div>
        )
    }
}

module.exports = NoFocusButton;
