import React from 'react';
import {AbstractReactComponent} from 'components/index.jsx';

const NoFocusButton = class NoFocusButton extends AbstractReactComponent {
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
        const {className, disabled, ...otherProps} = this.props;
        let cls = 'btn btn-default';
        if (disabled) {
            cls += ' disabled';
        }
        if (className) {
            cls += ' ' + className;
        }


        return (
            <div className={cls} onClick={this.handleClick} {...otherProps}>{this.props.children}</div>
        )
    }
}

export default NoFocusButton;
