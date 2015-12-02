/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./PartyPage.less');

import {RibbonMenu} from 'components';

var PartyPage = class PartyPage extends React.Component {
    render() {
        return (
            <div>
                <RibbonMenu />
                PARTY
            </div>
        )
    }
}

module.exports = PartyPage;

