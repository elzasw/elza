import classNames from 'classnames';
import React, { memo, MouseEvent, PropsWithChildren } from 'react';
import { Button as BootstrapButton, ButtonProps } from 'react-bootstrap';
import styles from './Button.module.scss';

interface IProps extends ButtonProps {
    className?: string
    onClick?: (e: MouseEvent<HTMLButtonElement>) => void
}

export const Button: React.FC<PropsWithChildren<IProps & any>> = memo(({children, className, ...buttonProps}) => {

    const buttonClass = classNames({
        [`${styles.button}`]: true,
        [`${className}`]: className,
    });

    return (
        <BootstrapButton
            {...buttonProps}
            className={buttonClass}
            variant={buttonProps.variant || 'secondary'}
        >
            {children}
        </BootstrapButton>
    );
});
