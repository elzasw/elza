/**
 * Stránka archivních pomůcek.
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./RecordPage.less');

import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {i18n} from 'components';
import {RibbonMenu, RibbonGroup, ToggleContent, FindindAidFileTree} from 'components';
import {ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';

var RecordPage = class RecordPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);
    }

    buildRibbon() {
        return (
            <RibbonMenu opened onShowHide={this.handleRibbonShowHide}>
                <RibbonGroup className="large">
                    <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><div><span className="btnText">{i18n('ribbon.action.findingAid')}</span></div></Button></IndexLinkContainer>
                    <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.record')}</span></div></Button></LinkContainer>
                    <LinkContainer to="/party"><Button><Glyphicon glyph="th-list" /><div><span className="btnText">{i18n('ribbon.action.party')}</span></div></Button></LinkContainer>
                </RibbonGroup>
            </RibbonMenu>
        )
    }

    render() {
        var leftPanel = (
            <div>LEFT - record</div>
        )

        var centerPanel = (
            <div>
                CENTER - record
            </div>
        )

        var rightPanel = (
            <div>
                RIGHT - record
            </div>
        )

        return (
            <PageLayout
                className='record-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}
            />
        )
    }
}

module.exports = RecordPage;

