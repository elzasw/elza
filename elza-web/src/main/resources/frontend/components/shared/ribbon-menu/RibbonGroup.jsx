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
        var classes = "ribbonGroup " + this.props.className;
        return (
                <div className={classes}>
                    {this.props.children}
                </div>
        );
    }
}

module.exports = RibbonGroup;