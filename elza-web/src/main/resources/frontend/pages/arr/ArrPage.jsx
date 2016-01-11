/**
 * Stránka archivních pomůcek.
 */

require ('./ArrPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Ribbon, i18n} from 'components';
import {AddFaForm, RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FaFileTree, AbstractReactComponent, ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'

var ArrPage = class ArrPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getActiveInfo', 'buildRibbon', 'handleAddFa', 'handleApproveFaVersion', 'handleCallAddFa', 'handleCallApproveFaVersion');

        this.state = {faFileTreeOpened: false};
    }

    handleCallAddFa(data) {
        WebApi.createFindingAid(data.name, data.ruleSetId, data.rulArrTypeId)
            .then(this.dispatch(modalDialogHide()));
    }
    
    handleCallApproveFaVersion(data) {
        var activeInfo = this.getActiveInfo();
        WebApi.approveVersion(activeInfo.activeFa.versionId, data.ruleSetId, data.rulArrTypeId)
            .then(this.dispatch(modalDialogHide()));
    }

    handleAddFa() {
        this.dispatch(modalDialogShow(this, i18n('arr.fa.title.add'), <AddFaForm create onSubmit={this.handleCallAddFa} />));
    }

    handleApproveFaVersion() {
        var activeInfo = this.getActiveInfo();
        var data = {
            name_: activeInfo.activeFa.name,
            ruleSetId: activeInfo.activeFa.activeVersion.arrangementType.ruleSetId,
            rulArrTypeId: activeInfo.activeFa.activeVersion.arrangementType.id
        }
        this.dispatch(modalDialogShow(this, i18n('arr.fa.title.approveVersion'), <AddFaForm initData={data} onSubmit={this.handleCallApproveFaVersion} />));
    }

    getActiveInfo() {
        var arrRegion = this.props.arrRegion
        var activeFa = null;
        var activeNode = null;
        var activeSubNode = null;
        if (arrRegion.activeIndex != null) {
            activeFa = arrRegion.fas[arrRegion.activeIndex];
            if (activeFa.nodes.activeIndex != null) {
                activeNode = activeFa.nodes.nodes[activeFa.nodes.activeIndex];
                if (activeNode.selectedSubNodeId != null) {
                    var i = indexById(activeNode.nodeInfo.childNodes, activeNode.selectedSubNodeId);
                    activeSubNode = activeNode.nodeInfo.childNodes[i];
                }
            }
        }
        return {
            activeFa,
            activeNode,
            activeSubNode,
        }
    }

    buildRibbon() {
        var activeInfo = this.getActiveInfo();

        var altSection = [];
        altSection.push(
            <RibbonGroup className="small">
                <Button onClick={this.handleAddFa}><Glyphicon glyph="plus" /><div><span className="btnText">{i18n('ribbon.action.arr.fa.add')}</span></div></Button>
            </RibbonGroup>
        );
        if (activeInfo.activeFa) {
            altSection.push(
                <RibbonGroup className="small">
                    <Button onClick={this.handleApproveFaVersion}><Glyphicon glyph="plus" /><div><span className="btnText">{i18n('ribbon.action.arr.fa.approveVersion')}</span></div></Button>
                </RibbonGroup>
            )
        }

        return (
            <Ribbon arr altSection={altSection} />
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

        var centerPanel = [];
        if (activeFa && activeFa.nodes) {
            var nodes = activeFa.nodes.nodes;
            centerPanel.push(
                <NodeTabs versionId={activeFa.activeVersion.id} nodes={nodes} activeIndex={activeFa.nodes.activeIndex}/>
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
    const {arrRegion, faFileTree, refTables, form} = state
    return {
        arrRegion,
        faFileTree,
        refTables
    }
}

module.exports = connect(mapStateToProps)(ArrPage);

