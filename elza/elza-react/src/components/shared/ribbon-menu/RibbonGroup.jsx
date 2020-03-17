/**
 * Ribbon menu.
 */

import React from 'react';


import './RibbonMenu.scss';

/**
 * Komponenta pro skupinu tlačítek v ribbonu.
 *
 * className: large = velká tlačítka v řadě
 * className: small = jednořádková tlačítka pod sebou, max 3 tlačítka na jednu skupinu
 */
const GROUP_COLUMN_LINES = 3;

const RibbonGroup = props => {
    const {className, right} = props;
    const classes = 'ribbonGroup ' + className;
    const rightClass = right ? 'right' : '';  //Pokud má příznak "right"
    if (className.indexOf('small') !== -1) {
        let parts = [];

        for (let a = 0; a < props.children.length; a += GROUP_COLUMN_LINES) {
            const sub = props.children.slice(a, a + GROUP_COLUMN_LINES);
            parts.push(<div key={'part-' + a} className={classes}>{sub}</div>);
        }
        return <div className={'ribbonSmallGroupsContainer ' + rightClass} key="small-container">{parts}</div>;
    } else {
        return (
            <div className={classes + ' ' + rightClass}>
                {props.children}
            </div>
        );
    }

};

export default RibbonGroup;
