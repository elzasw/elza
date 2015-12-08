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
import {PageLayout} from 'pages';
import {MainNodesStore, FaAppStore} from 'stores';

var FaPage = class FaPage extends React.Component {
    constructor(props) {
        super(props);

        this.buildRibbon = this.buildRibbon.bind(this);

        this.state = {store: FaAppStore};

        FaAppStore.listen(status => {
            this.setState( {store: status});
        });
    }

    buildRibbon() {
        return (
            <RibbonMenu>
                <ButtonGroup>
                    <IndexLinkContainer to="/"><Button><Glyphicon glyph="film" /><span>{i18n('ribbon.action.findingAid')}</span></Button></IndexLinkContainer>
                    <LinkContainer to="/record"><Button><Glyphicon glyph="th-list" /><span>{i18n('ribbon.action.record')}</span></Button></LinkContainer>
                    <LinkContainer to="/party"><Button><Glyphicon glyph="user" /><span>{i18n('ribbon.action.party')}</span></Button></LinkContainer>
                </ButtonGroup>
            </RibbonMenu>
        )
    }

    render() {
        var mainFas = this.state.store ? this.state.store.getMainFas() : {};
        var activeFa = mainFas.activeFa;

        var leftPanel = (
            <FaTreeTabs fas={mainFas.fas} activeFa={mainFas.activeFa} />
        )

        var mainNodes = activeFa ? activeFa.getMainNodes() : null;
        var centerPanel;
        if (mainNodes) {
            centerPanel = (
                <div>
                    <NodeTabs nodes={mainNodes.nodes} activeNode={mainNodes.activeNode}/>
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
            <ToggleContent className="fa-file-toggle-container" alwaysRender opened={false} closedIcon="chevron-right" openedIcon="chevron-left">
                <FindindAidFileTree />
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

module.exports = FaPage;

