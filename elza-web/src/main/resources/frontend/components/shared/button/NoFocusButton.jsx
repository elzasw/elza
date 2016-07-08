import React from 'react';
import {AbstractReactComponent} from 'components/index.jsx';

var NoFocusButton = class NoFocusButton extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleClick')

    }

    handleClick(e) {
        const {disabled, onClick} = this.props
        if (!disabled) {
            onClick(e)
        }
    }

    render() {
        var cls = 'btn btn-default';
        if (this.props.disabled) {
            cls += ' disabled';
        }
        if (this.props.className) {
            cls += ' ' + this.props.className;
        }

        return (
            <div title={this.props.title} className={cls} onClick={this.handleClick}>{this.props.children}</div>
        )
    }
}

module.exports = NoFocusButton;
