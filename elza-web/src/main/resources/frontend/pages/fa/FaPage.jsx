/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./FaPage.less');

import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {RibbonMenu, ToggleContent, FindindAidFileTree} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';

var FaPage = class FaPage extends React.Component {
    constructor(props) {
        super(props);

        this.handleRibbonShowHide = this.handleRibbonShowHide.bind(this);

        this.state = {
            ribbonOpened: true
        };
    }

    componentDidMount() {
        var splitPane1 = $(this.refs.splitPane1);
        var splitPane2 = $(this.refs.splitPane2);
        splitPane1.splitPane();
        splitPane2.splitPane();

        // Ukázka nastavení šířek, které budou předány, ve verzi 0.6.0 je již na toto funkce, zatím tato verze ale není v npm
        this.setLeftSplitterWidth(220);
        this.setRightSplitterWidth(100);
    }

    setLeftSplitterWidth(width) {
        var splitPane = $(this.refs.splitPane1);

        var size = width + "px";
        $('.split-pane-component', splitPane)[0].style.width = size
        $('.split-pane-component', splitPane)[1].style.left = size
        $('.split-pane-divider', splitPane)[0].style.left = size
        splitPane.resize()
    }

    setRightSplitterWidth(width) {
        var splitPane = $(this.refs.splitPane2);
        var size = width + "px";
        $('.split-pane-component', splitPane)[1].style.width = size
        $('.split-pane-component', splitPane)[0].style.right = size
        $('.split-pane-divider', splitPane)[0].style.right = size
        splitPane.resize()
    }

    handleRibbonShowHide(opened) {
        this.setState({ribbonOpened: opened});
    }

    render() {
        var mainCls = 'fa-page app-container';
        if (!this.state.ribbonOpened) {
            mainCls += " noRibbon";
        }

        return (
            <div className={mainCls}>
                <div className='app-header'>
                    <ToggleContent className="ribbon-toggle-container" opened={this.state.ribbonOpened} onShowHide={this.handleRibbonShowHide}>
                        <RibbonMenu opened={this.state.ribbonOpened} onShowHide={this.handleRibbonShowHide}>
                            <ButtonGroup>
                                <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><span>{i18n('ribbon.action.findingAid')}</span></Button></IndexLinkContainer>
                                <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><span>{i18n('ribbon.action.record')}</span></Button></LinkContainer>
                                <LinkContainer to="/party"><Button><Glyphicon glyph="user" /><span>{i18n('ribbon.action.party')}</span></Button></LinkContainer>
                            </ButtonGroup>
                        </RibbonMenu>
                    </ToggleContent>
                </div>
                <div className='app-content'>
                    <ToggleContent className="fa-file-toggle-container" alwaysRender opened={false} closedIcon="chevron-right" openedIcon="chevron-left">
                        <FindindAidFileTree />
                    </ToggleContent>
                    <div ref="splitPane1" className="split-pane fixed-left">
                        <div className="split-pane-component" id="left-component">
                            <FaTreeTabs/>
                        </div>
                        <div className="split-pane-divider" id="my-divider"></div>
                        <div className="split-pane-component" id="right-component-container">
                            <div ref="splitPane2" className="split-pane fixed-right">
                                <div className="split-pane-component" id="inner-left-component">
                                    <NodeTabs/>
                                    {false && <ModalDialog title="Upraveni osoby">
nnn
                                    </ModalDialog>}
                                </div>
                                <div className="split-pane-divider" id="inner-my-divider"></div>
                                <div className="split-pane-component" id="inner-right-component">
                                    FINDING_AID-right
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}

module.exports = FaPage;

