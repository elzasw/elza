import React from 'react';
import {AbstractReactComponent} from 'components';

var NoFocusButton = class NoFocusButton extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleClick')

    }

    handleClick() {
        const {disabled, onClick} = this.props
        if (!disabled) {
            onClick()
        }
    }

    render() {
        var cls = 'btn btn-default';
        if (this.props.disabled) {
            cls += ' disabled';
        }

        return (
            <div title={this.props.title} className={cls} onClick={this.handleClick}>{this.props.children}</div>
        )
    }
}

module.exports = NoFocusButton;
