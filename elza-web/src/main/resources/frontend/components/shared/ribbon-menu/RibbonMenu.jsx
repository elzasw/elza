/**
 * Ribbon menu.
 */

import React from 'react';

import {i18n} from 'components/index.jsx';
import {ButtonToolbar} from 'react-bootstrap';

require ('./RibbonMenu.less');

/**
 * Ribbon menu v pro záhlaví aplikace
 */
var RibbonMenu = class RibbonMenu extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <ButtonToolbar className="ribbon-menu">
                <div className="content">
                    {this.props.children}
                </div>
            </ButtonToolbar>
        );
    }
}

module.exports = RibbonMenu;