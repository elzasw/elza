/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./RegistryPage.less');

import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {Ribbon, ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';

var RegistryPage = class RegistryPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return (
            <Ribbon record {...this.props} />
        )
    }

    render() {
        var leftPanel = (
            <div>LEFT - record</div>
        )

        var centerPanel = (
            <div>
                CENTER - record
            </div>
        )

        var rightPanel = (
            <div>
                RIGHT - record
            </div>
        )

        return (
            <PageLayout
                className='record-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        )
    }
}

module.exports = RegistryPage;

