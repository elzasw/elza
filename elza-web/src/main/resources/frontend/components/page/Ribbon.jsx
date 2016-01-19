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

        this.state = {};
    }

    render() {
        var section = null;

        if (this.props.admin) {
            section = (
                <RibbonGroup className="">
                    <LinkContainer to="/admin/packages"><Button><Glyphicon glyph="cog" /><div><span className="btnText">{i18n('ribbon.action.admin.packages')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/admin/fulltext"><Button><Glyphicon glyph="cog" /><div><span className="btnText">{i18n('ribbon.action.admin.fulltext')}</span></div></Button></LinkContainer>
                </RibbonGroup>
            );
        } else if (this.props.arr) {
        }

        return (
            <RibbonMenu opened onShowHide={this.handleRibbonShowHide}>
                <RibbonGroup className="large">
                    <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><div><span className="btnText">{i18n('ribbon.action.home')}</span></div></Button></IndexLinkContainer>
                    <LinkContainer to="/arr"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.arr')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/registry"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.registry')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/party"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/admin"><Button><Glyphicon glyph="cog" /><div><span className="btnText">{i18n('ribbon.action.admin')}</span></div></Button></LinkContainer>

                    {false && <DropdownButton title={<span className="dropContent"><Glyphicon glyph='film' /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></span>}>
                      <MenuItem eventKey="1">Action</MenuItem>
                      <MenuItem eventKey="2">Another action jdoias djaos ijdoas i</MenuItem>
                      <MenuItem eventKey="3">Active Item</MenuItem>
                    </DropdownButton>}
                </RibbonGroup>


                <RibbonSplit />

                {section}
                {this.props.altSection}
                {this.props.altSection && <RibbonSplit />}
                {this.props.itemSection}

            </RibbonMenu>
        )
    }
}

function mapStateToProps(state) {
    const {arrRegion, faFileTree} = state
    return {
        arrRegion,
        faFileTree
    }
}

module.exports = Ribbon;


