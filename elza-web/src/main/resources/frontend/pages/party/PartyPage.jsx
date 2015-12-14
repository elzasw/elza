/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./PartyPage.less');

import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {Ribbon, ModalDialog, NodeTabs, PartySearch} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';

var PartyPage = class PartyPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return (
            <Ribbon party {...this.props} />
        )
    }

    render() {
        
        var filterText = "aaa";
        var activeParty = 25;
        var leftPanel = (
            <PartySearch filterText={filterText} activeParty={activeParty} />
        )
        
        var centerPanel = (
            <div>
                CENTER - party
            </div>
        )

        var rightPanel = (
            <div>
                RIGHT - party
            </div>
        )

        return (
            <PageLayout
                className='party-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        )
    }
}

module.exports = PartyPage;

