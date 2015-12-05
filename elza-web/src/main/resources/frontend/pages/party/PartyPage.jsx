/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./PartyPage.less');

import {RibbonMenu} from 'components';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';

var PartyPage = class PartyPage extends React.Component {
    render() {
        return (
            <div>
                <RibbonMenu>
                    <ButtonGroup>
                        <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><span>{i18n('ribbon.action.findingAid')}</span></Button></IndexLinkContainer>
                        <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><span>{i18n('ribbon.action.record')}</span></Button></LinkContainer>
                        <LinkContainer to="/party"><Button><Glyphicon glyph="user" /><span>{i18n('ribbon.action.party')}</span></Button></LinkContainer>
                    </ButtonGroup>
                </RibbonMenu>
                PARTY
            </div>
        )
    }
}

module.exports = PartyPage;

