/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./PartyPage.less');

import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {RibbonMenu, ToggleContent, FindindAidFileTree} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';

var PartyPage = class PartyPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return (
            <RibbonMenu>
                <ButtonGroup>
                    <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><span>{i18n('ribbon.action.findingAid')}</span></Button></IndexLinkContainer>
                    <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><span>{i18n('ribbon.action.record')}</span></Button></LinkContainer>
                    <LinkContainer to="/party"><Button><Glyphicon glyph="user" /><span>{i18n('ribbon.action.party')}</span></Button></LinkContainer>
                </ButtonGroup>
            </RibbonMenu>
        )
    }

    render() {
        var leftPanel = (
            <div>LEFT - party</div>
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

