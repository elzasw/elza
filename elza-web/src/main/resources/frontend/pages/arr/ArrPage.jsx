/**
 * Stránka archivních pomůcek.
 */

require('./ArrPage.less');

import React from 'react';
import ReactDOM from 'react-dom';
import {indexById} from 'stores/app/utils.jsx'
import {connect} from 'react-redux'
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {Tabs, Icon, Ribbon, i18n} from 'components';
import {FaExtendedView, FaForm, BulkActionsDialog, VersionValidationDialog, RibbonMenu, RibbonGroup, RibbonSplit, ToggleContent, FaFileTree, AbstractReactComponent, ModalDialog, NodeTabs, FaTreeTabs, ImportForm} from 'components';
import {ButtonGroup, Button, DropdownButton, MenuItem} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {AppStore} from 'stores'
import {WebApi} from 'actions'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {approveFa, showRegisterJp} from 'actions/arr/fa'
import {scopesDirty} from 'actions/refTables/scopesData'
import {versionValidate} from 'actions/arr/versionValidation'
import {packetsFetchIfNeeded} from 'actions/arr/packets'
import {packetTypesFetchIfNeeded} from 'actions/refTables/packetTypes'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {Utils} from 'components'
import {barrier} from 'components/Utils';
import {setFocus} from 'actions/global/focus'

var _developerSelectedTab = 0

var keyModifier = Utils.getKeyModifier()

var keymap = {
    Arr: {
        approveFaVersion: keyModifier + 'z',
        bulkActions: keyModifier + 'h',
        registerJp: keyModifier + 'j',
        area0: keyModifier + '0',
        area1: keyModifier + '1',
        area2: keyModifier + '2',
        area3: keyModifier + '3',
    },
    Tree: {}
}
var shortcutManager = new ShortcutsManager(keymap)

var ArrPage = class ArrPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('getActiveInfo', 'buildRibbon', 'handleRegisterJp',
            'handleApproveFaVersion', 'handleCallApproveFaVersion', 'getActiveFindingAidId', 'handleBulkActionsDialog',
            'handleValidationDialog', 'handleEditFaVersion', 'handleCallEditFaVersion', 'handleShortcuts', 'handleImport',
            'renderDeveloperPanel', 'renderDeveloperDescItems');

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
        var activeFa = this.getActiveInfo(nextProps.arrRegion).activeFa;
        if (activeFa) {
            var validation = activeFa.versionValidation;
            this.requestValidationData(validation.isDirty, validation.isFetching, activeFa.versionId);
        }
    }

    requestValidationData(isDirty, isFetching, versionId) {
        isDirty && !isFetching && this.dispatch(versionValidate(versionId, false))
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'approveFaVersion':
                this.handleApproveFaVersion()
                break
            case 'bulkActions':
                this.handleBulkActionsDialog()
                break
            case 'registerJp':
                this.handleRegisterJp()
                break
            case 'area0':
                this.dispatch(setFocus('arr', 0))
                break
            case 'area1':
                this.dispatch(setFocus('arr', 1))
                break
            case 'area2':
                this.dispatch(setFocus('arr', 2))
                break
            case 'area3':
                this.dispatch(setFocus('arr', 3))
                break
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
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
        this.dispatch(
            modalDialogShow(
                this,
                i18n('arr.fa.title.approve'),
                <FaForm
                    approve
                    initData={data}
                    onSubmitForm={this.handleCallApproveFaVersion}/>
            )
        );
    }

    /**
     * Načtení informačního objektu o aktuálním zobrazení sekce archvní pomůcky.
     * @return {Object} informace o aktuálním zobrazení sekce archvní pomůcky
     */
    getActiveInfo(from = this.props.arrRegion) {
        var arrRegion = from;
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
     */
    handleRegisterJp() {
        this.dispatch(showRegisterJp(!this.props.arrRegion.showRegisterJp));
    }


    handleBulkActionsDialog() {
        this.dispatch(modalDialogShow(this, i18n('arr.fa.title.bulkActions'),
            <BulkActionsDialog mandatory={false}/>
            )
        );
    }

    handleValidationDialog() {
        this.dispatch(modalDialogShow(this, i18n('arr.fa.title.versionValidation'), <VersionValidationDialog />));
    }

    handleEditFaVersion() {
        var activeInfo = this.getActiveInfo();
        var that = this;
        barrier(
            WebApi.getScopes(activeInfo.activeFa.versionId),
            WebApi.getAllScopes()
        )
            .then(data => {
                return {
                    scopes: data[0].data,
                    scopeList: data[1].data
                }
            })
            .then(json => {
                var data = {
                    name: activeInfo.activeFa.name,
                    regScopes: json.scopes
                };
                that.dispatch(modalDialogShow(that, i18n('arr.fa.title.update'),
                    <FaForm update initData={data} scopeList={json.scopeList}
                            onSubmitForm={that.handleCallEditFaVersion}/>));
            });
    }

    handleCallEditFaVersion(data) {
        let activeFa = this.getActiveInfo().activeFa;
        data.id = activeFa.faId;
        this.dispatch(scopesDirty(activeFa.versionId));
        WebApi.updateFindingAid(data).then((json) => {
            this.dispatch(modalDialogHide());
            this.dispatch(modalDialogHide());
        })
    }

    handleImport() {
        this.dispatch(
            modalDialogShow(this,
                i18n('import.title.fa'),
                <ImportForm fa/>
            )
        );
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
            <Button key="fa-import" onClick={this.handleImport}><Icon glyph='fa-download'/>
                <div><span className="btnText">{i18n('ribbon.action.arr.fa.import')}</span></div>
            </Button>
        );
        if (activeInfo.activeFa && !activeInfo.activeFa.closed) {
            itemActions.push(
                <Button key="edit-version" onClick={this.handleEditFaVersion}><Icon glyph="fa-pencil"/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fa.update')}</span></div>
                </Button>,
                <Button key="approve-version" onClick={this.handleApproveFaVersion}><Icon glyph="fa-calendar-check-o"/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fa.approve')}</span></div>
                </Button>,
                <Button key="bulk-actions" onClick={this.handleBulkActionsDialog}><Icon glyph="fa-cogs"/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fa.bulkActions')}</span></div>
                </Button>,
                <Button key="validation" onClick={this.handleValidationDialog}>
                    <Icon className={activeInfo.activeFa.versionValidation.isFetching ? "fa-spin" : ""} glyph={
                    activeInfo.activeFa.versionValidation.isFetching ? "fa-refresh" : (
                        activeInfo.activeFa.versionValidation.count > 0 ? "fa-exclamation-triangle" : "fa-check"
                    )
                }/>
                    <div><span className="btnText">{i18n('ribbon.action.arr.fa.validation')}</span></div>
                </Button>
            )
        }

        var show = this.props.arrRegion.showRegisterJp;

        itemActions.push(
            <Button active={show} onClick={this.handleRegisterJp} key="toggle-record-jp">
                <Icon glyph="fa-th-list"/>
                <div>
                    <span className="btnText">{i18n('ribbon.action.arr.show-register-jp')}</span>
                </div>
            </Button>
        )

        var altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="alt" className="large">{altActions}</RibbonGroup>
        }

        var itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="item" className="large">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon arr altSection={altSection} itemSection={itemSection}/>
        )
    }

    renderDeveloperDescItems(activeFa, node) {
        var rows = []
        if (node.subNodeForm.fetched) {
            node.subNodeForm.infoGroups.forEach(group => {
                var types = []
                group.types.forEach(type => {
                    var infoType = node.subNodeForm.infoTypesMap[type.id]
                    var refType = node.subNodeForm.refTypesMap[type.id]

                    var specs;
                    if (refType.useSpecification) {
                        specs = refType.descItemSpecs.map(spec => <div key={'spec' + spec.id}>{spec.name} [{spec.code}]</div>)
                        specs = <div>{specs}</div>
                    }

                    types.push(
                        <div key={'type' + refType.id} className={'desc-item-type ' + infoType.type}>
                            <h2 title={refType.name}>{refType.shortcut} <small>[{refType.code}]</small></h2>
                            <div key='1' className='desc'>{refType.description}</div>
                            <div key='2' className='attrs'>
                                <div key='1'><label>type:</label>{infoType.type}</div>
                                <div key='2'><label>dataType:</label>{refType.dataType.code}</div>
                                <div key='3'><label>width:</label>{infoType.width}</div>
                                <div key='4'><label>viewOrder:</label>{refType.viewOrder}</div>
                            </div>
                            {false && specs}
                        </div>
                    )
                })

                rows.push(
                    <div key={'group' + group.code}>
                        <h1 key='1'>{group.code}</h1>
                        <div key='2'>{types}</div>
                    </div>
                )
            })
        }

        return <div className='desc-items-container'>{rows}</div>
    }

    renderDeveloperPanel() {
        const {arrRegion} = this.props;
        var fas = arrRegion.fas;
        var activeFa = arrRegion.activeIndex != null ? arrRegion.fas[arrRegion.activeIndex] : null;
        if (!activeFa) {
            return
        }

        var node
        if (activeFa.nodes && activeFa.nodes.activeIndex !== null) {
            node = activeFa.nodes.nodes[activeFa.nodes.activeIndex]
        }
        if (!node) {
            return
        }
        return (
            <div className='developer-panel'>
                <Tabs.Container>
                    <Tabs.Tabs items={[{id: 0, title: i18n('developer.title.descItems')}, {id: 1, title: i18n('developer.title.xxx')}]} activeItem={{id: _developerSelectedTab}}
                        onSelect={(item)=>{_developerSelectedTab = item.id;this.setState({})}}
                    />
                    <Tabs.Content>
                        {_developerSelectedTab === 0 && this.renderDeveloperDescItems(activeFa, node)}
                    </Tabs.Content>
                </Tabs.Container>
            </div>
        )
    }

    render() {
        const {developer, focus, splitter, arrRegion, faFileTree, rulDataTypes, calendarTypes, descItemTypes, packetTypes} = this.props;

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
                    focus={focus}
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
                        closed={activeFa.closed}
                        nodes={activeFa.nodes.nodes}
                        activeIndex={activeFa.nodes.activeIndex}
                        rulDataTypes={rulDataTypes}
                        calendarTypes={calendarTypes}
                        descItemTypes={descItemTypes}
                        packetTypes={packetTypes}
                        packets={packets}
                        findingAidId={findingAidId}
                        showRegisterJp={showRegisterJp}
                    />
                )
            }
        }

        var rightPanel = (
            <div className="fa-right-container">
                {developer.enabled && this.renderDeveloperPanel()}
            </div>
        )

        var appContentExt = (
            <ToggleContent className="fa-file-toggle-container" alwaysRender opened={this.state.faFileTreeOpened}
                           onShowHide={(opened)=>this.setState({faFileTreeOpened: opened})}
                           closedIcon="fa-chevron-right" openedIcon="fa-chevron-left">
                <FaFileTree {...faFileTree} onSelect={()=>this.setState({faFileTreeOpened: false})}/>
            </ToggleContent>
        )

        return (
            <Shortcuts name='Arr' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    className='fa-page'
                    ribbon={this.buildRibbon()}
                    leftPanel={leftPanel}
                    centerPanel={centerPanel}
                    rightPanel={rightPanel}
                    appContentExt={appContentExt}
                />
            </Shortcuts>
        )
    }
}

function mapStateToProps(state) {
    const {splitter, arrRegion, faFileTree, refTables, form, focus, developer} = state
    return {
        splitter,
        arrRegion,
        faFileTree,
        focus,
        developer,
        rulDataTypes: refTables.rulDataTypes,
        calendarTypes: refTables.calendarTypes,
        descItemTypes: refTables.descItemTypes,
        packetTypes: refTables.packetTypes,
    }
}

ArrPage.propTypes = {
    splitter: React.PropTypes.object.isRequired,
    arrRegion: React.PropTypes.object.isRequired,
    faFileTree: React.PropTypes.object.isRequired,
    developer: React.PropTypes.object.isRequired,
    rulDataTypes: React.PropTypes.object.isRequired,
    calendarTypes: React.PropTypes.object.isRequired,
    descItemTypes: React.PropTypes.object.isRequired,
    packetTypes: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
}

ArrPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(ArrPage);
