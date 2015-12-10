/**
 * Standardní layout stránky, který ribbon, obsahuje levý panel, prostřední panel a pravý panel, které jsou odděleny splitterem.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./PageLayout.less');

var classNames = require('classnames');
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {RibbonMenu, ToggleContent, FindindAidFileTree} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';

var PageLayout = class PageLayout extends React.Component {
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
        this.setLeftSplitterWidth(260);
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
        var cls = classNames({
            'app-container': true,
            noRibbon: !this.state.ribbonOpened,
            [this.props.className]: true
        });

        return (
            <div className={cls}>
                <div className='app-header'>
                    <ToggleContent className="ribbon-toggle-container" opened={this.state.ribbonOpened} onShowHide={this.handleRibbonShowHide}>
                        {this.props.ribbon}
                    </ToggleContent>
                </div>
                <div className='app-content'>
                    {this.props.appContentExt}
                    <div ref="splitPane1" className="split-pane fixed-left">
                        <div className="split-pane-component" id="left-component">
                            {this.props.leftPanel}
                        </div>
                        <div className="split-pane-divider" id="my-divider"></div>
                        <div className="split-pane-component" id="right-component-container">
                            <div ref="splitPane2" className="split-pane fixed-right">
                                <div className="split-pane-component" id="inner-left-component">
                                    {this.props.centerPanel}
                                </div>
                                <div className="split-pane-divider" id="inner-my-divider"></div>
                                <div className="split-pane-component" id="inner-right-component">
                                    {this.props.rightPanel}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
}

module.exports = PageLayout;


