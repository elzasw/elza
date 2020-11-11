import classNames from 'classnames';
import React, {forwardRef, memo, MouseEvent, RefObject} from 'react';
import {Button as BootstrapButton, ButtonProps} from 'react-bootstrap';
import styles from './Button.module.scss';

interface IProps extends ButtonProps {
    className?: string;
    onClick?: (e: MouseEvent<HTMLButtonElement>) => void;
}

export const Button = memo(forwardRef<
    RefObject<HTMLButtonElement>, 
    IProps & any
>(({
    children, 
    className, 
    ...buttonProps
}, ref) => {
    const buttonClass = classNames({
        [`${styles.button}`]: true,
        [`${className}`]: className,
    });

    return (
        <BootstrapButton {...buttonProps} ref={ref} className={buttonClass} variant={buttonProps.variant || 'secondary'}>
            {children}
        </BootstrapButton>
    );
}));
