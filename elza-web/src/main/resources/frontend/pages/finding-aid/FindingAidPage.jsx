/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./FindingAidPage.less');

import {RibbonMenu, ToggleContent, FindindAidFileTree} from 'components';

var FindingAidPage = class FindingAidPage extends React.Component {
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
    }

    handleRibbonShowHide(opened) {
        this.setState({ribbonOpened: opened});
    }

    render() {
        var mainCls = 'finding-aid-page app-container';
        if (!this.state.ribbonOpened) {
            mainCls += " noRibbon";
        }

        return (
            <div className={mainCls}>
                <div className='app-header'>
                    <ToggleContent className="ribbon-toggle-container" opened={this.state.ribbonOpened} onShowHide={this.handleRibbonShowHide}>
                        <RibbonMenu opened={this.state.ribbonOpened} onShowHide={this.handleRibbonShowHide} />
                    </ToggleContent>
                </div>
                <div className='app-content'>
                    <ToggleContent className="finding-aid-file-toggle-container" alwaysRender opened={false} closedIcon="chevron-right" openedIcon="chevron-left">
                        <FindindAidFileTree />
                    </ToggleContent>
                    <div ref="splitPane1" className="split-pane fixed-left">
                        <div className="split-pane-component" id="left-component">
                            FINDING_AID-left
                        </div>
                        <div className="split-pane-divider" id="my-divider"></div>
                        <div className="split-pane-component" id="right-component-container">
                            <div ref="splitPane2" className="split-pane fixed-right">
                                <div className="split-pane-component" id="inner-left-component">
                                    FINDING_AID-center
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

module.exports = FindingAidPage;

