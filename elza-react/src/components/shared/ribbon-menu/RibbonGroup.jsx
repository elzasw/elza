/**
 * Ribbon menu.
 */

import React from 'react';
import classnames from 'classnames';

import './RibbonMenu.scss';

/**
 * Komponenta pro skupinu tlačítek v ribbonu.
 *
 * className: large = velká tlačítka v řadě
 * className: small = jednořádková tlačítka pod sebou, max 3 tlačítka na jednu skupinu
 */
// const GROUP_COLUMN_LINES = 3;

const RibbonGroup = props => {
    const {className, right} = props;
    const _className = classnames([
        "ribbonGroup",
        className, {
            "right": right,
        }
    ])

    return <div className={_className}>
        {props.children}
    </div>;
};

export default RibbonGroup;
