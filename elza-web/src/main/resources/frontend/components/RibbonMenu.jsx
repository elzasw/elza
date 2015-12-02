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

        this.handleToggle = this.handleToggle.bind(this);
        this.state = {
            opened: typeof this.props.opened == 'undefined' ? true : this.props.opened
        };        
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

    handleToggle() {
        this.setState({ opened: !this.state.opened });
        this.props.onShowHide && this.props.onShowHide(!this.state.opened);
    }

    render() {
        var toggleGlyph = this.state.opened ? "chevron-up" : "chevron-down";
        var cls = "ribbon-menu";
        cls += this.state.opened ? " opened" : " closed";

        return (
            <ButtonToolbar className={cls}>
                <div className="content">
                    {this.renderStatic()}
                </div>
                <Button className="showHideToggle" onClick={this.handleToggle}><Glyphicon glyph={toggleGlyph} /></Button>
            </ButtonToolbar>
        )
    }
}

module.exports = RibbonMenu;