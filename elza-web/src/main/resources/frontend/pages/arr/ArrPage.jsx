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
import {FaExtendedView, AddFaForm, RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FaFileTree, AbstractReactComponent, ModalDialog, NodeTabs, FaTreeTabs} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {approveFa, showRegisterJp} from 'actions/arr/fa'
import {packetsFetchIfNeeded} from 'actions/arr/packets'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes'

var ArrPage = class ArrPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getActiveInfo', 'buildRibbon', 'handleRegisterJp',
            'handleApproveFaVersion', 'handleCallApproveFaVersion', 'getActiveFindingAidId');

        this.state = {faFileTreeOpened: false};
    }

    componentDidMount() {
        this.dispatch(packetTypesFetchIfNeeded());
        var findingAidId = this.getActiveFindingAidId();
        if (findingAidId !== null) {
            this.dispatch(packetsFetchIfNeeded(findingAidId));
        }
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(packetTypesFetchIfNeeded());
        var findingAidId = this.getActiveFindingAidId();
        if (findingAidId !== null) {
            this.dispatch(packetsFetchIfNeeded(findingAidId));
        }
    }

    getActiveFindingAidId() {
        var arrRegion = this.props.arrRegion;
        var activeFa = arrRegion.activeIndex != null ? arrRegion.fas[arrRegion.activeIndex] : null;
        if (activeFa) {
            return activeFa.faId;
        } else {
            return null;
        }
    }

    /**
     * Vyvolání akce uzavření verze AP.
     * @param data {Object} data pro uzavření - z formuláře
     */
    handleCallApproveFaVersion(data) {
        var activeInfo = this.getActiveInfo();
        this.dispatch(approveFa(activeInfo.activeFa.versionId, data.ruleSetId, data.rulArrTypeId));
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
                    if (i != null) {
                        activeSubNode = activeNode.childNodes[i];
                    }
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
     * Zobrazení / skrytí záznamů u JP o rejstřících.
     *
     * @param show zobrazit/skrýt
     */
    handleRegisterJp(show) {
        this.dispatch(showRegisterJp(show));
    }

    /**
     * Sestavení Ribbonu.
     * @return {Object} view
     */
    buildRibbon() {
        var activeInfo = this.getActiveInfo();

        var altActions = [];

        var itemActions = [];
        altActions.push(
            <Button><Icon glyph='fa-download' /><div><span className="btnText">{i18n('ribbon.action.arr.fa.import')}</span></div></Button>
        );
        if (activeInfo.activeFa) {
            itemActions.push(
                <Button key="approve-version" onClick={this.handleApproveFaVersion}><Icon glyph="fa-calendar-check-o"/><div><span className="btnText">{i18n('ribbon.action.arr.fa.approveVersion')}</span></div></Button>
            )
        }

        var show = this.props.arrRegion.showRegisterJp;

        itemActions.push(
                <Button active={show} onClick={this.handleRegisterJp.bind(this, !show)} key="toggle-record-jp">
                    <Icon glyph="fa-calendar-check-o" />
                    <div>
                        <span className="btnText">{i18n('ribbon.action.arr.fa.record-jp')}</span>
                    </div>
                </Button>
        )

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
        const {arrRegion, faFileTree, rulDataTypes, calendarTypes, packetTypes} = this.props;

        var showRegisterJp = arrRegion.showRegisterJp;

        var fas = arrRegion.fas;
        var activeFa = arrRegion.activeIndex != null ? arrRegion.fas[arrRegion.activeIndex] : null;
        var leftPanel;
        if (arrRegion.extendedView) {   // rozšířené zobrazení stromu AP
        } else {
            leftPanel = (
                <FaTreeTabs
                    fas={fas}
                    activeFa={activeFa}
                />
            )
        }

        var packets = [];
        var findingAidId = this.getActiveFindingAidId();
        if (findingAidId && arrRegion.packets[findingAidId]) {
            packets = arrRegion.packets[findingAidId].items;
        }

        var centerPanel;
        if (activeFa) {
            if (arrRegion.extendedView) {   // rozšířené zobrazení stromu AP
                centerPanel = (
                    <FaExtendedView
                        fa={activeFa}
                        versionId={activeFa.versionId}
                    />
                )
            } else if (activeFa.nodes) {
                centerPanel = (
                    <NodeTabs
                        versionId={activeFa.versionId}
                        fa={activeFa}
                        nodes={activeFa.nodes.nodes}
                        activeIndex={activeFa.nodes.activeIndex}
                        rulDataTypes={rulDataTypes}
                        calendarTypes={calendarTypes}
                        packetTypes={packetTypes}
                        packets={packets}
                        findingAidId={findingAidId}
                        showRegisterJp={showRegisterJp}
                    />
                )
            }
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
        packetTypes: refTables.packetTypes,
    }
}

ArrPage.propTypes = {
    arrRegion: React.PropTypes.object.isRequired,
    faFileTree: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
}

module.exports = connect(mapStateToProps)(ArrPage);
