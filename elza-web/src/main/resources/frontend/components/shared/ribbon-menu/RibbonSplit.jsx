/**
 * Ribbon menu.
 */

import React from 'react';

import {i18n} from 'components/index.jsx';
import {ButtonToolbar} from 'react-bootstrap';

require('./RibbonMenu.less');

/**
 * Oddělovač skupin komponent v RibbonMenu.
 */
var RibbonSplit = class RibbonSplit extends React.Component {

    render() {
        return (
                <div className="ribbonSplit">
                </div>
        );
    }
}

module.exports = RibbonSplit;