/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */

import React from 'react';
import ReactDOM from 'react-dom';

require('./RegistryPage.less');
const classNames = require('classnames');
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components/index.jsx';
import {Icon, RibbonGroup,Ribbon, ModalDialog, NodeTabs, ArrPanel,
        SearchWithGoto, RegistryPanel, DropDownTree, AddRegistryForm, ImportForm,
        ListBox} from 'components';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {Button} from 'react-bootstrap';
import {PageLayout} from 'pages/index.jsx';
import {indexById} from 'stores/app/utils.jsx'
import {registryRegionDataSelectRecord,
        registrySearchData,
        registryClearSearch,
        registryChangeParent,
        registryDeleteRegistry,
        registryStartMove,
        registryCancelMove,
        registryUnsetParents,
        registryRecordUpdate,
        registryRecordMove,
        registryDelete
} from 'actions/registry/registryRegionData.jsx'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {
    fetchRegistryIfNeeded,
    registrySetTypesId,
    fetchRegistry,
    registryAdd,
    registryClickNavigation,
    registryArrReset
} from 'actions/registry/registryRegionList.jsx'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes.jsx'
const ShortcutsManager = require('react-shortcuts');
const Shortcuts = require('react-shortcuts/component');
import {Utils} from 'components/index.jsx';
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus.jsx'
import {setFocus} from 'actions/global/focus.jsx'
import * as perms from 'actions/user/Permission.jsx';

const keyModifier = Utils.getKeyModifier();

const keymap = {
    Registry: {
        addRegistry: keyModifier + 'n',
        registryMove: keyModifier + 'x',
        registryMoveApply: keyModifier + 'v',
        registryMoveCancel: keyModifier + 'w',
        area1: keyModifier + '1',
        area2: keyModifier + '2'
    }
};
const shortcutManager = new ShortcutsManager(keymap);

const RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'buildRibbon',
            'canMoveApplyCancelRegistry',
            'canMoveRegistry',
            'canDeleteRegistry',
            'handleAddRegistry',
            'handleArrReset',
            'handleCallAddRegistry',
            'handleCancelMoveRegistry',
            'handleClickNavigation',
            'handleDoubleClick',
            'handleRegistryImport',
            'handleRegistryTypesSelectNavigation',
            'handleDeleteRegistry',
            'handleDeleteRegistryDialog',
            'handleSaveMoveRegistry',
            'handleSearch',
            'handleSearchClear',
            'handleSelect',
            'handleShortcuts',
            'handleStartMoveRegistry',
            'handleUnsetParents',
            'renderListItem',
            'trySetFocus'
        );
    }

    componentWillReceiveProps(nextProps) {
        this.initData(nextProps);
    }

    componentDidMount() {
        this.initData(this.props);
    }

    initData(props) {
        const {registryRegion: {filterText, registryParentId, registryTypesId, panel: {versionId}}} = props;
        this.dispatch(fetchRegistryIfNeeded(filterText, registryParentId, registryTypesId, versionId));
        this.dispatch(refRecordTypesFetchIfNeeded());
        this.trySetFocus(props)
    }

    trySetFocus(props) {
        const {focus} = props;

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.registryList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                       this.refs.registryList.focus();
                       focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, 'registry', 1) || isFocusFor(focus, 'registry', 1, 'list')) {
                this.setState({}, () => {
                   this.refs.registryList.focus();
                   focusWasSet()
                })
            }
        }
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'addRegistry':
                this.handleAddRegistry();
                break;
            case 'registryMove':
                if (this.canMoveRegistry()) {
                    this.handleStartMoveRegistry()
                }
                break;
            case 'registryMoveApply':
                if (this.canMoveApplyCancelRegistry()) {
                    this.handleSaveMoveRegistry()
                }
                break;
            case 'registryMoveCancel':
                if (this.canMoveApplyCancelRegistry()) {
                    this.handleCancelMoveRegistry()
                }
                break;
            case 'area1':
                this.dispatch(setFocus('registry', 1));
                break;
            case 'area2':
                this.dispatch(setFocus('registry', 2));
                break
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleAddRegistry() {
        const {registryRegion: {registryParentId, panel, parents}} = this.props;
        let parentName = '';

        if (indexById(parents, registryParentId, 'id') !== null) {
            parentName = parents[indexById(parents, registryParentId, 'id')].name;
        }
        this.dispatch(registryAdd(registryParentId, panel.versionId, this.handleCallAddRegistry, parentName, false));
    }

    handleCallAddRegistry(data) {
        const {registryRegion: {filterText, registryParentId, registryTypesId, panel: {versionId}}} = this.props;
        this.dispatch(fetchRegistry(filterText, registryParentId, registryTypesId, versionId));
        this.dispatch(registryRegionDataSelectRecord({
            ...data,
            selectedId: data.recordId
        }));
    }

    handleDeleteRegistryDialog(){
        const result = confirm(i18n('registry.deleteRegistryQuestion'));
        if (result) {
            this.dispatch(this.handleDeleteRegistry());
        }

    }

    handleDeleteRegistry() {
        this.dispatch(registryDelete(this.props.registryRegion.selectedId));
    }


    handleStartMoveRegistry() {
        this.dispatch(registryStartMove());
    }

    handleSaveMoveRegistry() {
        this.dispatch(registryRecordMove({
            ...this.props.registryRegion.recordForMove,
            parentRecordId: this.props.registryRegion.registryParentId
        }));
    }

    handleCancelMoveRegistry() {
        this.dispatch(registryCancelMove());
    }


    handleRegistryImport() {
       this.dispatch(
           modalDialogShow(this,
               i18n('import.title.registry'),
               <ImportForm record/>
           )
       );
    }

    canMoveRegistry() {
        const {registryRegion: {selectedId, registryRegionData, registryParentId, recordForMove}} = this.props;

        return selectedId &&
            registryRegionData.item &&
            !recordForMove &&
            !registryRegionData.item.partyId &&
            registryRegionData.item.hierarchical &&
            selectedId != registryParentId
    }

    canDeleteRegistry() {
        const {registryRegion: {selectedId, registryRegionData, registryParentId}} = this.props;

        return selectedId &&
            registryRegionData.item &&
            registryRegionData.item.childs &&
            registryRegionData.item.childs.length === 0 &&
            selectedId != registryParentId
    }

    canMoveApplyCancelRegistry() {
        const {registryRegion: {selectedId, registryRegionData, recordForMove}} = this.props;

        return selectedId &&
            registryRegionData.item &&
            recordForMove &&
            !registryRegionData.item.partyId
    }

    buildRibbon() {
        const {registryRegion: {registryRegionData}, userDetail} = this.props;

        const altActions = [];

// zakomentováno pro test, nutno opravit pri refaktorizaci scope a permissions
// uzivatel nemusi mit pravo pro zapis vsech scope, aby mohl vytvaret hesla a osoby
//        if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL)) {
            altActions.push(
                <Button key='addRegistry' onClick={this.handleAddRegistry}><Icon glyph="fa-download"/>
                    <div><span className="btnText">{i18n('registry.addNewRegistry')}</span></div>
                </Button>
            );
            altActions.push(
                <Button key='registryImport' onClick={this.handleRegistryImport}><Icon glyph='fa-download'/>
                    <div><span className="btnText">{i18n('ribbon.action.registry.import')}</span></div>
                </Button>
            );
//        }

        const itemActions = [];
        if (this.canDeleteRegistry()) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: registryRegionData.item ? registryRegionData.item.scopeId : null})) {
                itemActions.push(
                    <Button key='registryRemove' onClick={this.handleDeleteRegistryDialog}><Icon
                        glyph="fa-trash"/>
                        <div><span className="btnText">{i18n('registry.deleteRegistry')}</span></div>
                    </Button>
                );
            }
        }
        if (this.canMoveRegistry()) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: registryRegionData.item ? registryRegionData.item.scopeId : null})) {
                itemActions.push(
                    <Button key='registryMove' onClick={this.handleStartMoveRegistry}><Icon glyph="fa-share"/>
                        <div><span className="btnText">{i18n('registry.moveRegistry')}</span></div>
                    </Button>
                );
            }
        }
        if (this.canMoveApplyCancelRegistry()) {
            if (userDetail.hasOne(perms.REG_SCOPE_WR_ALL, {type: perms.REG_SCOPE_WR, scopeId: registryRegionData.item ? registryRegionData.item.scopeId : null})) {
                itemActions.push(
                    <Button key='registryMoveApply' onClick={this.handleSaveMoveRegistry}><Icon
                        glyph="fa-check-circle"/>
                        <div><span className="btnText">{i18n('registry.applyMove')}</span></div>
                    </Button>
                );
                itemActions.push(
                    <Button key='registryMoveCancel' onClick={this.handleCancelMoveRegistry}><Icon glyph="fa-times"/>
                        <div><span className="btnText">{i18n('registry.cancelMove')}</span></div>
                    </Button>
                );
            }
        }

        let altSection;
        if (altActions.length > 0) {
            altSection = <RibbonGroup key="ribbon-alt-actions" className="small">{altActions}</RibbonGroup>
        }
        let itemSection;
        if (itemActions.length > 0) {
            itemSection = <RibbonGroup key="ribbon-item-actions" className="small">{itemActions}</RibbonGroup>
        }

        return (
            <Ribbon registry altSection={altSection} itemSection={itemSection} {...this.props} />
        )
    }

    handleSelect(record, event) {
        this.dispatch(registryRegionDataSelectRecord({...record, selectedId: record.recordId}));
    }

    handleDoubleClick(item, event) {
        if (!item.hierarchical) {
            return
        }
        const {recordForMove} = this.props.registryRegion;

        if (recordForMove && recordForMove.selectedId === item.recordId) {
            this.dispatch(addToastrWarning(i18n('registry.disallowedMoveAction.title'), i18n('registry.disallowedMoveAction.text')));
            return false;
        }
        const parents = item.parents.slice();
        parents.push({id: item.recordId, name:item.record});
        this.dispatch(registryClearSearch());
        this.dispatch(registryChangeParent({
            registryParentId: item.recordId,
            parents,
            typesToRoot: item.typesToRoot,
            filterText: '',
            registryTypesId: item.registerTypeId
        }));
    }

    handleClickNavigation(recordIdForOpen, event) {
        this.dispatch(registryClickNavigation(recordIdForOpen));
    }

    handleSearch(search) {
        this.dispatch(registrySearchData({filterText: search}));
    }

    handleSearchClear(){
        this.dispatch(registrySetTypesId(null));
        this.dispatch(registryUnsetParents(null));
        this.dispatch(registryClearSearch());
    }

    handleRegistryTypesSelect(selectedId, event) {
            this.dispatch(registrySetTypesId(selectedId));
    }

    handleRegistryTypesSelectNavigation(selectedId){
        this.dispatch(registryUnsetParents(null));
        this.dispatch(registrySetTypesId(selectedId));
    }

    handleUnsetParents(){
        this.dispatch(registryUnsetParents(null));
        this.dispatch(registrySetTypesId(null));
    }

    handleArrReset() {
        this.dispatch(registryArrReset());
    }

    renderListItem(item) {
        const {registryRegion: {parents, typesToRoot, selectedId, registryParentId, registryTypesId}} = this.props;

        const parentsShown = [];
        const parentsTypeShown = [];
        if (parents && parents.length > 0) {
            parents.map((val) => {
                parentsShown.push(val.id);
            });
        }
        if (typesToRoot) {
            typesToRoot.map((val) => {
                parentsTypeShown.push(val.id);
            });
        }
        const cls = classNames({
            active: selectedId === item.recordId,
            'search-result-row': 'search-result-row'
        });

        let doubleClick = this.handleDoubleClick.bind(this, item);
        let iconName = 'fa-folder';
        let clsItem = 'registry-list-icon-record';

        if (item.hierarchical === false) {
            iconName = 'fa-file-o';
            clsItem = 'registry-list-icon-list';
            doubleClick = false;
        }


        // výsledky z vyhledávání
        if (!registryParentId) {
            const path = [];
            if (item.parents) {
                item.parents.map((val) => {
                    if (parentsShown.indexOf(val.id) === -1) {
                        path.push(val.name);
                    }
                });
            }

            if (item.typesToRoot) {
                item.typesToRoot.map((val) => {
                    if (registryTypesId !== val.id) {
                        path.push(val.name);
                    }
                });
            }

            return (
                <div key={'record-id-' + item.recordId} title={path} className={cls} onDoubleClick={doubleClick}>
                    <div><Icon glyph={iconName} /></div>
                    <div title={item.record} className={clsItem}>{item.record}</div>
                    <div className="path" >{path.join(' | ')}</div>
                </div>
            )
        }  else {
            // jednořádkový výsledek
            return (
                <div key={'record-id-' + item.recordId} className={cls} onDoubleClick={doubleClick}>
                    <div><Icon glyph={iconName} key={item.recordId} /></div>
                    <div key={'record-' + item.recordId + '-name'} title={item.record} className={clsItem}>{item.record}</div>
                </div>
            )
        }
    }

    render() {
        const {splitter, registryRegion: {countRecords, records, selectedId, registryTypesId, parents, typesToRoot, panel, fetched, filterText, registryParentId}} = this.props;

        let regListBox = <div className='search-norecord'>{i18n('registry.listNoRecord')}</div>
        if (records.length) {
            const activeIndex = indexById(records, selectedId, 'recordId')
            regListBox = <ListBox
                className='registry-listbox'
                ref='registryList'
                items={records}
                activeIndex={activeIndex}
                renderItemContent={this.renderListItem}
                onFocus={this.handleSelect}
                onSelect={this.handleDoubleClick}
            />
        }

        let navParents = null;

        if (registryTypesId !== null && parents && parents.length > 0) {
            const tmpParents = parents.slice();
            const nazevRodice = tmpParents.pop().name;
            const cestaRodice = [];
            tmpParents.map(val => {
                cestaRodice.push(<span className='clickAwaiblePath parentPath' key={'parent'+val.id}  title={val.name} onClick={this.handleClickNavigation.bind(this,val.id)}>{val.name}</span>);
            });

            if (typesToRoot) {
                typesToRoot.map(val => {
                    cestaRodice.push(<span className='clickAwaiblePath parentPath' key={'regType'+val.id} title={val.name} onClick={this.handleRegistryTypesSelectNavigation.bind(this,val.id)} >{val.name}</span>);
                });
            }

            const breadcrumbs = [];
             cestaRodice.map((val, key) => {
                 if (key) {
                     breadcrumbs.push(<span className='parentPath' key={key}><span className='parentPath'>&nbsp;|&nbsp;</span>{val}</span>);
                 } else {
                     breadcrumbs.push(<span key={key} className='parentPath'>{val}</span>);
                 }
             });

            navParents = <div className="record-parent-info">
                <div className='record-selected-name'>
                    <div className="icon"><Icon glyph="fa-folder-open"/></div>
                    <div className="title" title={nazevRodice}>{nazevRodice}</div>
                    <div className="back" onClick={this.handleUnsetParents}><Icon glyph="fa-close"/></div>
                </div>
                <div className='record-selected-breadcrumbs'>{breadcrumbs}</div>
            </div>

        }

        const dropDownForSearch = <DropDownTree
            nullValue={{id: null, name: i18n('registry.all')}}
            key='search'
            items={this.props.refTables.recordTypes.items}
            value={registryTypesId}
            onChange={this.handleRegistryTypesSelect.bind(this)}
            disabled={registryParentId !== null}
        />;

        const arrPanel = panel.versionId != null ? <ArrPanel onReset={this.handleArrReset} name={panel.name} /> : null;

        const leftPanel = (
            <div className="registry-list">
                <div className='registry-list-header-container'>
                    {arrPanel}
                    {dropDownForSearch}
                    <SearchWithGoto
                        onFulltextSearch={this.handleSearch}
                        onClear={this.handleSearchClear}
                        placeholder={i18n('search.input.search')}
                        filterText={filterText}
                        showFilterResult={true}
                        type="INFO"
                        itemsCount={records.length}
                        allItemsCount={countRecords}
                        />
                </div>
                <div className='registry-list-breadcrumbs' key='breadcrumbs'>{navParents}</div>
                <div className="registry-list-results">{fetched ? regListBox : <Loading/>}</div>
            </div>
        );

        const centerPanel = (
            <div className='registry-page'>
                <RegistryPanel selectedId={selectedId}/>
            </div>
        );

        return (
            <Shortcuts name='Registry' handler={this.handleShortcuts}>
                <PageLayout
                    splitter={splitter}
                    key='registryPage'
                    ribbon={this.buildRibbon()}
                    leftPanel={leftPanel}
                    centerPanel={centerPanel}
                />
            </Shortcuts>
        )
    }
};

function mapStateToProps(state) {
    const {splitter, registryRegion, refTables, focus, userDetail} = state;
    return {
        splitter,
        registryRegion,
        refTables,
        focus,
        userDetail
    }
}

RegistryPage.propTypes = {
    splitter: React.PropTypes.object.isRequired,
    registryRegion: React.PropTypes.object.isRequired,
    refTables: React.PropTypes.object.isRequired,
    focus: React.PropTypes.object.isRequired,
    userDetail: React.PropTypes.object.isRequired
};

RegistryPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
};

module.exports = connect(mapStateToProps)(RegistryPage);
