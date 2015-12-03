/**
 * Ribbon menu.
 */

import React from 'react';
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';

require ('./RibbonMenu.less');

import {ButtonToolbar, ButtonGroup, Button, Glyphicon} from 'react-bootstrap';

var RibbonMenu = class RibbonMenu extends React.Component {
    constructor(props) {
        super(props);
    }

    renderStatic() {
        return (
            <ButtonGroup>
                <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><span>{i18n('ribbon.action.findingAid')}</span></Button></IndexLinkContainer>
                <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><span>{i18n('ribbon.action.record')}</span></Button></LinkContainer>
                <LinkContainer to="/party"><Button><Glyphicon glyph="user" /><span>{i18n('ribbon.action.party')}</span></Button></LinkContainer>
            </ButtonGroup>
        );
    }

    render() {
        return (
            <ButtonToolbar className="ribbon-menu">
                <div className="content">
                    {this.renderStatic()}
                </div>
            </ButtonToolbar>
        );
    }
}

module.exports = RibbonMenu;