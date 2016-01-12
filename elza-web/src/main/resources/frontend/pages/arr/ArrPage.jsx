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
import {createFa, approveFa} from 'actions/arr/fa'

var ArrPage = class ArrPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getActiveInfo', 'buildRibbon', 'handleAddFa', 'handleApproveFaVersion', 'handleCallAddFa', 'handleCallApproveFaVersion');

        this.state = {faFileTreeOpened: false};
    }

    handleCallAddFa(data) {
        this.dispatch(createFa(data));
    }
    
    handleCallApproveFaVersion(data) {
        var activeInfo = this.getActiveInfo();
        this.dispatch(approveFa(activeInfo.activeFa.versionId, data.ruleSetId, data.rulArrTypeId, activeInfo.activeFa.faId));
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

        var altActions = [];
        altActions.push(
            <Button onClick={this.handleAddFa}><Glyphicon glyph="plus" /><div><span className="btnText">{i18n('ribbon.action.arr.fa.add')}</span></div></Button>
        );

        var itemActions = [];
        if (activeInfo.activeFa) {
            itemActions.push(
                <Button onClick={this.handleApproveFaVersion}><Glyphicon glyph="plus" /><div><span className="btnText">{i18n('ribbon.action.arr.fa.approveVersion')}</span></div></Button>
            )
        }

        var altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        var itemSection = <RibbonGroup className="large">{itemActions}</RibbonGroup>

        return (
            <Ribbon arr altSection={altSection} itemSection={itemSection} />
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

