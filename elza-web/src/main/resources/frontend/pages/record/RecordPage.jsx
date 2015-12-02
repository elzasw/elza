/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./RecordPage.less');

import {RibbonMenu} from 'components';

var RecordPage = class RecordPage extends React.Component {
    render() {
        return (
            <div>
                <RibbonMenu />
                RECORD
            </div>
        )
    }
}

module.exports = RecordPage;

