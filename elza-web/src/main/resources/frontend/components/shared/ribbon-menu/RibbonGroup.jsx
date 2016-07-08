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
var RibbonGroup = class RibbonGroup extends React.Component {

    render() {
        const {className} = this.props

        var classes = "ribbonGroup " + className;

        if (className.indexOf("small") !== -1) {
            var parts = []
            for (var a=0; a<this.props.children.length; a += 3) {
                var sub = this.props.children.slice(a, a + 3)
                parts.push(<div className={classes}>{sub}</div>)
            }
            return <div className="ribbonSmallGroupsContainer">{parts}</div>
        } else {
            return (
                <div className={classes}>
                    {this.props.children}
                </div>
            )
        }

    }
}

module.exports = RibbonGroup;