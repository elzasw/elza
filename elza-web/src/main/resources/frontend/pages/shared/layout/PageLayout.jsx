/**
 * Standardní layout stránky, který ribbon, obsahuje levý panel, prostřední panel a pravý panel, které jsou odděleny splitterem.
 */

require ('./PageLayout.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
var classNames = require('classnames');
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components/index.jsx';
import {Splitter, RibbonMenu, ToggleContent, FindindAidFileTree} from 'components/index.jsx';
import {ModalDialog, NodeTabs, FundTreeTabs} from 'components/index.jsx';
import {ButtonGroup, Button} from 'react-bootstrap';
import {splitterResize} from 'actions/global/splitter.jsx';

var PageLayout = class PageLayout extends React.Component {
    constructor(props) {
        super(props);

        this.handleRibbonShowHide = this.handleRibbonShowHide.bind(this);

        this.state = {
            ribbonOpened: true
        };
    }

    componentDidMount() {
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

                    <Splitter
                        leftSize={this.props.splitter.leftWidth}
                        rightSize={this.props.splitter.rightWidth}
                        onChange={(size) => {this.props.dispatch(splitterResize(size.leftSize, size.rightSize))}}
                        left={this.props.leftPanel}
                        center={this.props.centerPanel}
                        right={this.props.rightPanel}
                    />
                </div>
            </div>
        )
    }
}

module.exports = connect()(PageLayout);


