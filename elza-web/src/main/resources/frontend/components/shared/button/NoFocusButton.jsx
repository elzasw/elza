import React from 'react';
import {AbstractReactComponent} from 'components/index.jsx';

class NoFocusButton extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleClick')

    }

    render() {
        const {className, onClick, disabled, ...otherProps} = this.props;
        let cls = 'btn btn-default';
        if (disabled) {
            cls += ' disabled';
        }
        if (className) {
            cls += ' ' + className;
        }


        return (
            <div className={cls} onClick={!disabled ? onClick : null} {...otherProps}>{this.props.children}</div>
        )
    }
}

export default NoFocusButton;
