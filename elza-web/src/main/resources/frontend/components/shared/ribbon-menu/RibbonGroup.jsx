/**
 * Ribbon menu.
 */

import React from 'react';

import {i18n} from 'components/index.jsx';
import {ButtonToolbar} from 'react-bootstrap';

require('./RibbonMenu.less');

/**
 * Komponenta pro skupinu tlačítek v ribbonu.
 *
 * className: large = velká tlačítka v řadě
 * className: small = jednořádková tlačítka pod sebou, max 3 tlačítka na jednu skupinu
 */
const GROUP_COLUMN_LINES = 3;
const RibbonGroup = class RibbonGroup extends React.Component {

    render() {
        const {className, right} = this.props
        const classes = "ribbonGroup " + className;
        var rightClass = right ? "right" : "";  //Pokud má příznak "right"
        if (className.indexOf("small") !== -1) {
            var parts = []

            for (let a=0; a<this.props.children.length; a += GROUP_COLUMN_LINES) {
                const sub = this.props.children.slice(a, a + GROUP_COLUMN_LINES)
                parts.push(<div key={"part-" + a} className={classes}>{sub}</div>)
            }
            return <div className={"ribbonSmallGroupsContainer " +  rightClass } key="small-container">{parts}</div>
        } else {
            return (
                <div className={classes +" "+ rightClass}>
                    {this.props.children}
                </div>
            )
        }

    }
}

export default RibbonGroup;
