/**
 * Stránka archivních pomůcek.
 */

require ('./ArrPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Ribbon, i18n} from 'components';
import {RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FaFileTree} from 'components';
import {AbstractReactComponent, ModalDialog, NodeTabs, FaTreeTabs} from 'components'; 
import {ButtonGroup, Button, DropdownButton, MenuItem, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'

var ArrPage = class ArrPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('buildRibbon');

        this.state = {faFileTreeOpened: false};
    }

    buildRibbon() {
        return (
                <Ribbon fa {...this.props} />
        )
    }

    render() {
        var fas = this.props.arrRegion.fas;
        var activeFa = this.props.arrRegion.activeIndex != null ? this.props.arrRegion.fas[this.props.arrRegion.activeIndex] : null;
        var leftPanel = (
            <FaTreeTabs
                fas={fas}
                activeFa={activeFa}
            />
        )

        var centerPanel;
        if (activeFa && activeFa.nodes) {
            var nodes = activeFa.nodes.nodes;
            var activeNode = activeFa.nodes.activeIndex != null ? nodes[activeFa.nodes.activeIndex] : null;
            centerPanel = (
                <div>
                    <NodeTabs nodes={nodes} activeNode={activeNode} nodeForm={this.props.arrRegion.nodeForm}/>
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
                <FaFileTree {...this.props.faFileTree} onSelect={()=>this.setState({faFileTreeOpened: false})}/>
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
    const {arrRegion, faFileTree} = state
    return {
        arrRegion,
        faFileTree,
    }
}

module.exports = connect(mapStateToProps)(ArrPage);

