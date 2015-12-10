/**
 * Stránka archivních pomůcek.
 */

require ('./FaPage.less');

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

var FaPage = class FaPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('buildRibbon');

        this.state = {faFileTreeOpened: false};
    }

    buildRibbon() {
        return (
                <RibbonMenu opened onShowHide={this.handleRibbonShowHide}>
                    <RibbonGroup className="large">
                        <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></Button></IndexLinkContainer>
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

    render() {
        //console.log("FA_PAGE:::PROPS", this.props, "STATE", this.state);

        var fas = this.props.fas.items;
        var activeFa = this.props.fas.activeIndex != null ? this.props.fas.items[this.props.fas.activeIndex] : null;
        var leftPanel = (
            <FaTreeTabs fas={fas} activeFa={activeFa} />
        )

        var centerPanel;
        if (activeFa && activeFa.nodes) {
            var nodes = activeFa.nodes.items;
            var activeNode = activeFa.nodes.activeIndex != null ? nodes[activeFa.nodes.activeIndex] : null;
            centerPanel = (
                <div>
                    <NodeTabs nodes={nodes} activeNode={activeNode}/>
                    {false && <ModalDialog title="Upraveni osoby">
                        nnn
                    </ModalDialog>}
                </div>
            )
        }

        var rightPanel = (
            <div>
                FINDING_AID-right
            </div>
        )

        var appContentExt = (
            <ToggleContent className="fa-file-toggle-container" alwaysRender opened={this.state.faFileTreeOpened} onShowHide={(opened)=>this.setState({faFileTreeOpened: opened})} closedIcon="chevron-right" openedIcon="chevron-left">
                <FindindAidFileTree {...this.props.faFileTree} onSelect={()=>this.setState({faFileTreeOpened: false})}/>
            </ToggleContent>
        )

        return (
            <PageLayout
                className='fa-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
                appContentExt={appContentExt}
            />
        )
    }
}

function mapStateToProps(state) {
    const {fas, faFileTree} = state
    return {
        fas,
        faFileTree
    }
}

module.exports = connect(mapStateToProps)(FaPage);

