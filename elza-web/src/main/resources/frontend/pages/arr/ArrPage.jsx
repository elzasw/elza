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
import {Icon, Ribbon, i18n} from 'components';
import {AddFaForm, RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FaFileTree, AbstractReactComponent, ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {approveFa} from 'actions/arr/fa'

var ArrPage = class ArrPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getActiveInfo', 'buildRibbon',
            'handleApproveFaVersion', 'handleCallApproveFaVersion');

        this.state = {faFileTreeOpened: false};
    }

    /**
     * Vyvolání akce uzavření verze AP.
     * @param data {Object} data pro uzavření - z formuláře
     */
    handleCallApproveFaVersion(data) {
        var activeInfo = this.getActiveInfo();
        this.dispatch(approveFa(activeInfo.activeFa.versionId, data.ruleSetId, data.rulArrTypeId, activeInfo.activeFa.faId));
    }

    /**
     * Zobrazení dualogu uzavření verze AP.
     */
    handleApproveFaVersion() {
        var activeInfo = this.getActiveInfo();
        var data = {
            name_: activeInfo.activeFa.name,
            ruleSetId: activeInfo.activeFa.activeVersion.arrangementType.ruleSetId,
            rulArrTypeId: activeInfo.activeFa.activeVersion.arrangementType.id
        }
        this.dispatch(modalDialogShow(this, i18n('arr.fa.title.approveVersion'), <AddFaForm initData={data} onSubmit={this.handleCallApproveFaVersion} />));
    }

    /**
     * Načtení informačního objektu o aktuálním zobrazení sekce archvní pomůcky.
     * @return {Object} informace o aktuálním zobrazení sekce archvní pomůcky
     */
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
                    var i = indexById(activeNode.childNodes, activeNode.selectedSubNodeId);
                    activeSubNode = activeNode.childNodes[i];
                }
            }
        }
        return {
            activeFa,
            activeNode,
            activeSubNode,
        }
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        var activeInfo = this.getActiveInfo();

        var altActions = [];

        var itemActions = [];
        if (activeInfo.activeFa) {
            itemActions.push(
                <Button onClick={this.handleApproveFaVersion}><Icon glyph="fa-calendar-check-o"/><div><span className="btnText">{i18n('ribbon.action.arr.fa.approveVersion')}</span></div></Button>
            )
        }

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup className="large">{altActions}</RibbonGroup>
        }
        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup className="large">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr altSection={altSection} itemSection={itemSection} />
        )
    }

    render() {
        var {arrRegion, faFileTree, rulDataTypes, calendarTypes} = this.props;

        var fas = arrRegion.fas;
        var activeFa = arrRegion.activeIndex != null ? arrRegion.fas[arrRegion.activeIndex] : null;
        var leftPanel = (
            <FaTreeTabs
                fas={fas}
                activeFa={activeFa}
            />
        )

        var centerPanel;
        if (activeFa && activeFa.nodes) {
            centerPanel = (
                <NodeTabs
                    versionId={activeFa.activeVersion.id}
                    nodes={activeFa.nodes.nodes}
                    activeIndex={activeFa.nodes.activeIndex}
                    rulDataTypes={rulDataTypes}
                    calendarTypes={calendarTypes}
                />
            )
        }

        var rightPanel = (
            <div>
                
            </div>
        )

        var appContentExt = (
            <ToggleContent className="fa-file-toggle-container" alwaysRender opened={this.state.faFileTreeOpened} onShowHide={(opened)=>this.setState({faFileTreeOpened: opened})} closedIcon="fa-chevron-right" openedIcon="fa-chevron-left">
                <FaFileTree {...faFileTree} onSelect={()=>this.setState({faFileTreeOpened: false})}/>
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
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
    }
}

ArrPage.propTypes = {
    arrRegion: React.PropTypes.object.isRequired,
    faFileTree: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
}

module.exports = connect(mapStateToProps)(ArrPage);
