/**
 * Stránka rejstříků.
 * Zobrazuje stranku s vyberem rejstriku a jeho detailem/editaci
 */

import React from 'react';
import ReactDOM from 'react-dom';

require ('./RegistryPage.less');
var classNames = require('classnames');
import {LinkContainer, IndexLinkContainer} from 'react-router-bootstrap';
import {Link, IndexLink} from 'react-router';
import {connect} from 'react-redux'
import {AbstractReactComponent, i18n, Loading} from 'components';
import {Icon, RibbonGroup,Ribbon, ModalDialog, NodeTabs, ArrPanel,
        Search, RegistryPanel, DropDownTree, AddRegistryForm, ImportForm,
        ListBox} from 'components';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions'
import {WebApi} from 'actions'
import {MenuItem, DropdownButton, ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {indexById} from 'stores/app/utils.jsx'
import {Nav, Glyphicon, NavItem} from 'react-bootstrap';
import {registryRegionData,
        registrySearchData,
        registryClearSearch,
        registryChangeParent,
        registryRemoveRegistry,
        registryStartMove,
        registryCancelMove,
        registryUnsetParents,
        registryRecordUpdate,
        registryRecordMove
} from 'actions/registry/registryRegionData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {fetchRegistryIfNeeded,
        registrySetTypesId,
        fetchRegistry,
        registryAdd,
        registryClickNavigation,
        registryArrReset
} from 'actions/registry/registryRegionList'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'
var ShortcutsManager = require('react-shortcuts');
var Shortcuts = require('react-shortcuts/component');
import {Utils} from 'components'
import {canSetFocus, focusWasSet, isFocusFor} from 'actions/global/focus'
import {setFocus} from 'actions/global/focus'

var keyModifier = Utils.getKeyModifier()

var keymap = {
    Registry: {
        addRegistry: keyModifier + 'n',
        registryMove: keyModifier + 'x',
        registryMoveApply: keyModifier + 'v',
        registryMoveCancel: keyModifier + 'w',
        area1: keyModifier + '1',
        area2: keyModifier + '2',
    },
}
var shortcutManager = new ShortcutsManager(keymap)

var RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('buildRibbon', 'handleSelect', 'handleSearch', 'handleSearchClear', 'handleDoubleClick',
                'handleClickNavigation', 'handleAddRegistry', 'handleCallAddRegistry',
                'handleRemoveRegistryDialog', 'handleRemoveRegistry', 'handleStartMoveRegistry',
                'handleSaveMoveRegistry', 'handleCancelMoveRegistry',
                'handleUnsetParents', 'handleArrReset', 'handleRegistryImport', 'handleRegistryTypesSelectNavigation',
                'renderListItem', 'handleShortcuts', 'canRemoveRegistry', 'canMoveRegistry', 'canMoveApplyCancelRegistry',
                'trySetFocus');
    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fetchRegistryIfNeeded(nextProps.registryRegion.filterText,
                nextProps.registryRegion.registryParentId,
                nextProps.registryRegion.registryTypesId,
                nextProps.registryRegion.panel.versionId));
        this.dispatch(refRecordTypesFetchIfNeeded());
        this.trySetFocus(nextProps)
    }

    componentDidMount(){
        this.dispatch(fetchRegistryIfNeeded(this.props.registryRegion.filterText,
                this.props.registryRegion.registryParentId,
                this.props.registryRegion.registryTypesId,
                this.props.registryRegion.panel.versionId));
        this.dispatch(refRecordTypesFetchIfNeeded());
        this.trySetFocus(this.props)
    }

    trySetFocus(props) {
        var {focus} = props

        if (canSetFocus()) {
            if (isFocusFor(focus, null, 1)) {   // focus po ztrátě
                if (this.refs.registryList) {   // ještě nemusí existovat
                    this.setState({}, () => {
                       this.refs.registryList.focus()
                       focusWasSet()
                    })
                }
            } else if (isFocusFor(focus, 'registry', 1) || isFocusFor(focus, 'registry', 1, 'list')) {
                this.setState({}, () => {
                   this.refs.registryList.focus()
                   focusWasSet()
                })
            }
        }
    }

    handleShortcuts(action) {
        console.log("#handleShortcuts", '[' + action + ']', this);
        switch (action) {
            case 'addRegistry':
                this.handleAddRegistry()
                break
            case 'registryMove':
                if (this.canMoveRegistry()) {
                    this.handleStartMoveRegistry()
                }
                break
            case 'registryMoveApply':
                if (this.canMoveApplyCancelRegistry()) {
                    this.handleSaveMoveRegistry()
                }
                break
            case 'registryMoveCancel':
                if (this.canMoveApplyCancelRegistry()) {
                    this.handleCancelMoveRegistry()
                }
                break
            case 'area1':
                this.dispatch(setFocus('registry', 1))
                break
            case 'area2':
                this.dispatch(setFocus('registry', 2))
                break
        }
    }

    getChildContext() {
        return { shortcuts: shortcutManager };
    }

    handleAddRegistry() {
        const parentId = this.props.registryRegion.registryParentId
        var parentName = '';

        if (indexById(this.props.registryRegion.parents, parentId, 'id')!==null) {
            parentName = this.props.registryRegion.parents[indexById(this.props.registryRegion.parents, parentId, 'id')].name;
        }
        this.dispatch(registryAdd(parentId, this.props.registryRegion.panel.versionId, this.handleCallAddRegistry, parentName, false));
    }

    handleCallAddRegistry(data) {
        this.dispatch(fetchRegistry(this.props.registryRegion.filterText,
                this.props.registryRegion.registryParentId,
                this.props.registryRegion.registryTypesId,
                this.props.registryRegion.panel.versionId));
        data['selectedId'] = data.recordId;
        this.dispatch(registryRegionData(data));
    }

    handleRemoveRegistryDialog(){
        var result = confirm(i18n('registry.removeRegistryQuestion'));
        if (result) {
            this.dispatch(this.handleRemoveRegistry());
        }

    }
    handleRemoveRegistry() {
        WebApi.removeRegistry( this.props.registryRegion.selectedId ).then(json => {
            this.dispatch(registryRemoveRegistry({}));
        });
    }


    handleStartMoveRegistry(){
        this.dispatch(registryStartMove());
    }

    handleSaveMoveRegistry(){
        var data = Object.assign({}, this.props.registryRegion.recordForMove);
        data['parentRecordId'] = this.props.registryRegion.registryParentId;
        this.dispatch(registryRecordMove(data));
    }

    handleCancelMoveRegistry(){
        var registry = Object.assign({}, registry);
        this.dispatch(registryCancelMove(registry));
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
        const {registryRegion} = this.props;

        if (registryRegion.selectedId && registryRegion.registryRegionData && registryRegion.registryRegionData.item) {
            if (!registryRegion.recordForMove && !registryRegion.registryRegionData.item.partyId && registryRegion.registryRegionData.item.hierarchical && registryRegion.selectedId != registryRegion.registryParentId){
                return true
            }
        }
        return false
    }

    canRemoveRegistry() {
        const {registryRegion} = this.props;

        if (registryRegion.selectedId && registryRegion.registryRegionData && registryRegion.registryRegionData.item) {
            if (!registryRegion.registryRegionData.item.childs && registryRegion.selectedId != registryRegion.registryParentId) {
                return true
            }
        }
        return false
    }

    canMoveApplyCancelRegistry() {
        const {registryRegion} = this.props;

        if (registryRegion.selectedId && registryRegion.registryRegionData && registryRegion.registryRegionData.item) {
            if (registryRegion.recordForMove && !registryRegion.registryRegionData.item.partyId) {
                return true
            }
        }
        return false
    }

    buildRibbon() {
        const {registryRegion} = this.props;

        var altActions = [];

        altActions.push(
            <Button key='addRegistry' onClick={this.handleAddRegistry}><Icon glyph="fa-download" /><div><span className="btnText">{i18n('registry.addNewRegistry')}</span></div></Button>
        );

        altActions.push(
            <Button key='registryImport' onClick={this.handleRegistryImport}><Icon glyph='fa-download'/>
                <div><span className="btnText">{i18n('ribbon.action.registry.import')}</span></div>
            </Button>
        );

        var itemActions = [];
        if (this.canRemoveRegistry()) {
            itemActions.push(
                <Button key='registryRemove' onClick={this.handleRemoveRegistryDialog}><Icon
                    glyph="fa-trash"/>
                    <div><span className="btnText">{i18n('registry.removeRegistry')}</span></div>
                </Button>
            );
        }
        if (this.canMoveRegistry()) {
            itemActions.push(
                <Button key='registryMove' onClick={this.handleStartMoveRegistry}><Icon glyph="fa-share" /><div><span className="btnText">{i18n('registry.moveRegistry')}</span></div></Button>
            );
        }
        if (this.canMoveApplyCancelRegistry()) {
            itemActions.push(
                <Button key='registryMoveApply' onClick={this.handleSaveMoveRegistry}><Icon glyph="fa-check-circle" /><div><span className="btnText">{i18n('registry.applyMove')}</span></div></Button>
            );
            itemActions.push(
                <Button key='registryMoveCancel' onClick={this.handleCancelMoveRegistry}><Icon glyph="fa-times" /><div><span className="btnText">{i18n('registry.cancelMove')}</span></div></Button>
            );
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
            <Ribbon registry altSection={altSection} itemSection={itemSection} {...this.props} />
        )
    }

    handleSelect(registry, event) {
        var registry = Object.assign({}, registry,{selectedId: registry.recordId});
        this.dispatch(registryRegionData(registry));
    }

    handleDoubleClick(item, event) {
        if (!item.hierarchical) {
            return
        }

        if (this.props.registryRegion.recordForMove && this.props.registryRegion.recordForMove.selectedId === item.recordId) {
            this.dispatch(addToastrWarning(i18n('registry.danger.disallowed.action.title'), i18n('registry.danger.disallowed.action.can.not.move.into.myself')));
            return false;
        }
        var rodice = item.parents.slice();

        rodice.push({id: item.recordId, name:item.record});
        var registry = Object.assign({}, registry,{registryParentId: item.recordId, parents: rodice, typesToRoot: item.typesToRoot, filterText: '', registryTypesId: item.registerTypeId});
        this.dispatch(registryClearSearch());
        this.dispatch(registryChangeParent(registry));
    }

    handleClickNavigation(recordIdForOpen, event) {
        this.dispatch(registryClickNavigation(recordIdForOpen));
    }

    handleSearch(search, event) {
        var registry = Object.assign({}, registry,{filterText: search});
        this.dispatch(registrySearchData(registry));
    }

    handleSearchClear(){
        this.dispatch(registrySetTypesId(null));
        this.dispatch(registryUnsetParents(null));
        this.dispatch(registryClearSearch());
    }

    hlandleRegistryTypesSelect(selectedId, event) {
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
        const {registryRegion} = this.props;

        var parentsShown = [];
        var parentsTypeShown = [];
        if (registryRegion.parents) {
            registryRegion.parents.map((val) => {
                parentsShown.push(val.id);
            });
        }
        if (registryRegion.typesToRoot) {
            registryRegion.typesToRoot.map((val) => {
                parentsTypeShown.push(val.id);
            });
        }
        var cls = classNames({
            active: registryRegion.selectedId === item.recordId,
            'search-result-row': 'search-result-row'
        });

        var doubleClick = this.handleDoubleClick.bind(this, item);
        var iconName = 'fa-folder';
        var clsItem = 'registry-list-icon-record';

        if (item.hierarchical === false) {
            iconName = 'fa-file-o';
            clsItem = 'registry-list-icon-list';
            doubleClick = false;
        }


        // výsledky z vyhledávání
        if ( !registryRegion.registryParentId ) {
            var path = [];
            if (item.parents) {
                item.parents.map((val) => {
                    if(parentsShown.indexOf(val.id)===-1) {
                        path.push(val.name);
                    }
                });
            }

            if (item.typesToRoot) {
                item.typesToRoot.map((val) => {
                    if(registryRegion.registryTypesId!==val.id) {
                        path.push(val.name);
                    }
                });
            }

            return (
                <div key={item.recordId} title={path} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                    <div><Icon glyph={iconName} /></div>
                    <div  title={item.record} className={clsItem}>{item.record}</div>
                    <div className="path" >{path.join(' | ')}</div>
                </div>
            )
        }
        else{
            // jednořádkový výsledek
            return (
                <div key={item.recordId} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                    <div><Icon glyph={iconName} key={item.recordId} /></div>
                    <div key={item.recordId} title={item.record} className={clsItem}>{item.record}</div>
                </div>
            )
        }
    }

    render() {
        const {splitter, registryRegion} = this.props;

        var lb = <div className='search-norecord'>{i18n('registry.list.norecord')}</div>
        if (registryRegion.records.length) {
            var activeIndex = indexById(registryRegion.records, registryRegion.selectedId, 'recordId')
            var lb = <ListBox 
                className='registry-listbox'
                ref='registryList'
                items={registryRegion.records}
                activeIndex={activeIndex}
                renderItemContent={this.renderListItem}
                onFocus={this.handleSelect}
                onSelect={this.handleDoubleClick}
                />
        }

        var navParents = null;

        if (registryRegion.registryTypesId!==null && registryRegion.parents && registryRegion.parents.length) {
            var nazevRodice = registryRegion.parents[registryRegion.parents.length-1].name;
            var cestaRodice = [];
            var tmpParents = registryRegion.parents.slice();
            tmpParents.pop();
            tmpParents.map(val => {
                cestaRodice.push(<span className='clickAwaiblePath parentPath' key={'parent'+val.id}  title={val.name} onClick={this.handleClickNavigation.bind(this,val.id)}>{val.name}</span>);
            });

            if (registryRegion.typesToRoot) {
                registryRegion.typesToRoot.map(val => {
                    cestaRodice.push(<span className='clickAwaiblePath parentPath' key={'regType'+val.id} title={val.name} onClick={this.handleRegistryTypesSelectNavigation.bind(this,val.id)} >{val.name}</span>);
                });
            }

            var parentId = null;
            if (registryRegion.parents.length > 1)
                parentId = registryRegion.parents[0].id;
            var breadcrumbs = [];
             cestaRodice.map((val, key) => {
                 if (key) {
                     breadcrumbs.push(<span className='parentPath' key={key}><span className='parentPath'>&nbsp;|&nbsp;</span>{val}</span>);
                 } else {
                     breadcrumbs.push(<span key={key} className='parentPath'>{val}</span>);
                 }
             });

            navParents =    <div className="record-parent-info">
                                <div className='record-selected-name'>
                                    <div className="icon"><Icon glyph="fa-folder-open" /></div>
                                    <div className="title"  title={nazevRodice}>{nazevRodice}</div>
                                    <div className="back" onClick={this.handleUnsetParents}><Icon glyph="fa-close" /></div>
                                </div>
                                <div className='record-selected-breadcrumbs'>{breadcrumbs}</div>
                            </div>

        }

        var dropDownForSearch = <DropDownTree
            nullValue={{id: null, name: i18n('registry.all')}}
            key='search'
            items={this.props.refTables.recordTypes.items}
            value={registryRegion.registryTypesId}
            onChange={this.hlandleRegistryTypesSelect.bind(this)}
            disabled={registryRegion.registryParentId !== null}
            />

        var arrPanel = null;

        if (registryRegion.panel.versionId != null) {
            arrPanel = <ArrPanel onReset={this.handleArrReset} name={registryRegion.panel.name} />
        }

        var leftPanel = (
            <div className="registry-list">
                <div className='registry-list-header-container'>
                    {arrPanel}
                    {dropDownForSearch}
                    <Search
                        onSearch={this.handleSearch}
                        onClear={this.handleSearchClear.bind(this)}
                        placeholder={i18n('search.input.search')}
                        filterText={registryRegion.filterText}
                        />
                </div>
                <div className='registry-list-breadcrumbs' key='breadcrumbs'>
                    {navParents}
                </div>
                <div className="registry-list-results">
                    {(!registryRegion.fetched) && <Loading/>}
                    {(registryRegion.fetched) && lb}
                </div>
            </div>
        )

        var centerPanel = (
            <div className='registry-page'>
                <RegistryPanel selectedId={registryRegion.selectedId}/>
            </div>
        )

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
}

function mapStateToProps(state) {
    const {splitter, registryRegion, refTables, focus} = state
    return {
        splitter,
        registryRegion,
        refTables,
        focus
    }
}

RegistryPage.childContextTypes = {
    shortcuts: React.PropTypes.object.isRequired
}

module.exports = connect(mapStateToProps)(RegistryPage);
