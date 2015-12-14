import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FindindAidFileTree} from 'components';
import {AbstractReactComponent, ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'

var Ribbon = class Ribbon extends AbstractReactComponent {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <RibbonMenu opened onShowHide={this.handleRibbonShowHide}>
                <RibbonGroup className="large">
                    <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><div><span className="btnText">{i18n('ribbon.action.home')}</span></div></Button></IndexLinkContainer>
                    <LinkContainer to="/fa"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.record')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/party"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>

                    <DropdownButton title={<span className="dropContent"><Glyphicon glyph='film' /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></span>}>
                      <MenuItem eventKey="1">Action</MenuItem>
                      <MenuItem eventKey="2">Another action jdoias djaos ijdoas i</MenuItem>
                      <MenuItem eventKey="3">Active Item</MenuItem>
                    </DropdownButton>
                </RibbonGroup>

                <RibbonSplit />

                <RibbonGroup className="small">
                    <DropdownButton title={<span className="dropContent"><Glyphicon glyph='film' /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></span>}>
                        <MenuItem eventKey="1">Action</MenuItem>
                        <MenuItem eventKey="2">Another action</MenuItem>
                        <MenuItem eventKey="3">Active Item</MenuItem>
                      </DropdownButton>
                    <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></Button></IndexLinkContainer>
                    <LinkContainer to="/party"><Button><Glyphicon glyph="user" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>
                </RibbonGroup>

                <RibbonSplit />

                <RibbonGroup className="small">
                    <DropdownButton title={<span className="dropContent"><Glyphicon glyph='film' /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></span>}>
                      <MenuItem eventKey="1">Action</MenuItem>
                      <MenuItem eventKey="2">Another action</MenuItem>
                      <MenuItem eventKey="3">Active Item</MenuItem>
                    </DropdownButton>
                    <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.record')}</span></div></Button></LinkContainer>
                </RibbonGroup>
            </RibbonMenu>
        )
    }
}

function mapStateToProps(state) {
    const {arrangementRegion, faFileTree} = state
    return {
        arrangementRegion,
        faFileTree
    }
}

module.exports = Ribbon;


