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
import {AbstractReactComponent, i18n, Loading, Toastr} from 'components';
import {Icon, RibbonGroup,Ribbon, ModalDialog, NodeTabs, ArrPanel,
        Search, RegistryPanel, DropDownTree, AddRegistryForm, ImportRegistryForm} from 'components';
import {WebApi} from 'actions'
import {MenuItem, DropdownButton, ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {Nav, Glyphicon, NavItem} from 'react-bootstrap';
import {registryRegionData, registrySearchData, registryClearSearch, registryChangeParent, registryRemoveRegistry, registryStartMove, registryCancelMove, registryUnsetParents, registryRecordUpdate, registryRecordMove} from 'actions/registry/registryRegionData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {fetchRegistryIfNeeded, registrySetTypesId, fetchRegistry, registryAdd, registryClickNavigation, registryArrReset} from 'actions/registry/registryRegionList'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'

var RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('buildRibbon', 'handleSelect', 'handleSearch', 'handleSearchClear', 'handleDoubleClick',
                'handleClickNavigation', 'handleAddRegistry', 'handleCallAddRegistry',
                'handleRemoveRegistryDialog', 'handleRemoveRegistry', 'handleStartMoveRegistry',
                'handleSaveMoveRegistry', 'handleCancelMoveRegistry', 'handleCloseTypesRegistry',
                'handleUnsetParents', 'handleArrReset');

    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fetchRegistryIfNeeded(nextProps.registryRegion.filterText,
                nextProps.registryRegion.registryParentId,
                nextProps.registryRegion.registryTypesId,
                nextProps.registryRegion.panel.versionId));
        this.dispatch(refRecordTypesFetchIfNeeded());
    }

    componentDidMount(){
        this.dispatch(fetchRegistryIfNeeded(this.props.registryRegion.filterText,
                this.props.registryRegion.registryParentId,
                this.props.registryRegion.registryTypesId,
                this.props.registryRegion.panel.versionId));
        this.dispatch(refRecordTypesFetchIfNeeded());
    }

    handleAddRegistry(parentId) {
        this.dispatch(registryAdd(parentId, this.props.registryRegion.panel.versionId, this.handleCallAddRegistry));
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
            var registry = Object.assign({}, registry);
            this.dispatch(registryRemoveRegistry(registry));
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
               i18n('registry.importRegistry'),
               <ImportRegistryForm onSubmitForm={this.handleCallImportRegistry.bind(this)} />
           )
       );
    }

    handleCallImportRegistry(values) {
        var data = Object.assign({}, values);
        console.log('import rejstriku', data);
        WebApi.importRegistry(data.transformationName, data.registryScopeId, data.stopOnError, data.xmlFile ).then(json => {
            this.dispatch(modalDialogHide());
        });    
    }
    
    buildRibbon() {


        var altActions = [];
        altActions.push(
            <Button key='addRegistry' onClick={this.handleAddRegistry.bind(this, this.props.registryRegion.registryParentId)}><Icon glyph="fa-download" /><div><span className="btnText">{i18n('registry.addNewRegistry')}</span></div></Button>
        );


        altActions.push(
            <Button key='registryImport' onClick={this.handleRegistryImport.bind(this)}><Icon glyph='fa-download' /><div><span className="btnText">{i18n('ribbon.action.registry.import')}</span></div></Button>
        );

        var itemActions = [];
        if (this.props.registryRegion.selectedId) {
            if (this.props.registryRegion.registryRegionData && !this.props.registryRegion.registryRegionData.item.childs) {
                itemActions.push(
                    <Button key='registryRemove' onClick={this.handleRemoveRegistryDialog.bind(this)}><Icon
                        glyph="fa-trash"/>
                        <div><span className="btnText">{i18n('registry.removeRegistry')}</span></div>
                    </Button>
                );
            }

            if (!this.props.registryRegion.recordForMove && this.props.registryRegion.registryRegionData && !this.props.registryRegion.registryRegionData.item.partyId){
                itemActions.push(
                    <Button key='registryMove' onClick={this.handleStartMoveRegistry.bind(this)}><Icon glyph="fa-share" /><div><span className="btnText">{i18n('registry.moveRegistry')}</span></div></Button>
                );
            }
            if (this.props.registryRegion.recordForMove && this.props.registryRegion.registryRegionData && !this.props.registryRegion.registryRegionData.item.partyId){
                itemActions.push(
                    <Button key='registryMoveApply' onClick={this.handleSaveMoveRegistry.bind(this)}><Icon glyph="fa-check-circle" /><div><span className="btnText">{i18n('registry.applyMove')}</span></div></Button>
                );
                itemActions.push(
                    <Button key='registryMoveCancel' onClick={this.handleCancelMoveRegistry.bind(this)}><Icon glyph="fa-times" /><div><span className="btnText">{i18n('registry.cancelMove')}</span></div></Button>
                );
            }
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
        if (this.props.registryRegion.recordForMove && this.props.registryRegion.recordForMove.selectedId === item.recordId) {
            this.dispatch(Toastr.Actions.warning({title: i18n('registry.danger.disallowed.action.title'), message: i18n('registry.danger.disallowed.action.can.not.move.into.myself')}));
            return false;
        }
        var rodice = item.parents.slice();
        rodice.push({id: item.recordId, name:item.record});
        var registry = Object.assign({}, registry,{registryParentId: item.recordId, parents: rodice, typesToRoot: item.typesToRoot, filterText: ''});
        this.dispatch(registryChangeParent(registry));
        this.dispatch(registryClearSearch());

    }

    handleClickNavigation(recordIdForOpen, event) {
        this.dispatch(registryClickNavigation(recordIdForOpen));
    }

    handleSearch(search, event) {
        var registry = Object.assign({}, registry,{filterText: search});
        this.dispatch(registrySearchData(registry));
    }

    handleSearchClear(){
        this.dispatch(registryClearSearch());
    }

    hlandleRegistryTypesSelect(selectedId, event) {
        this.dispatch(registrySetTypesId(selectedId));
    }

    handleCloseTypesRegistry() {
        this.dispatch(registrySetTypesId(null));
    }

    handleUnsetParents(parent, event){
        if (!parent) {
            this.dispatch(registryUnsetParents(null));
        }
        else{
            this.dispatch(this.handleClickNavigation(parent, event));
        }
    }

    handleArrReset() {
        this.dispatch(registryArrReset());
    }

    render() {
        const {splitter, registryRegion} = this.props;

        var listOfRecord = <div className='search-norecord'>{i18n('registry.list.norecord')}</div>;
        if (this.props.registryRegion.records.length) {
            listOfRecord = this.props.registryRegion.records.map(item=>{

                var parentsShown = [];
                var parentsTypeShown = [];
                if (this.props.registryRegion.parents) {
                    this.props.registryRegion.parents.map((val) => {
                        parentsShown.push(val.id);
                    });
                }
                if (this.props.registryRegion.typesToRoot) {
                    this.props.registryRegion.typesToRoot.map((val) => {
                        parentsTypeShown.push(val.id);
                    });
                }
                var cls = classNames({
                    active: this.props.registryRegion.selectedId === item.recordId,
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
                if ( this.props.registryRegion.filterText ) {
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
                            if(this.props.registryRegion.registryTypesId!==val.id) {
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
            })
        }


        var navParents = '';

        if (this.props.registryRegion.parents && this.props.registryRegion.parents.length){
            var nazevRodice = this.props.registryRegion.parents[this.props.registryRegion.parents.length-1].name;
            var cestaRodice = [];
            var tmpParents = this.props.registryRegion.parents.slice();

            tmpParents.pop();
            tmpParents.map(val => {
                cestaRodice.push(<span className='clickAwaiblePath parentPath' key={'parent'+val.id}  title={val.name} onClick={this.handleClickNavigation.bind(this,val.id)}>{val.name}</span>);
            });

            if (this.props.registryRegion.typesToRoot) {
                this.props.registryRegion.typesToRoot.map(val => {
                    cestaRodice.push(<span className='clickAwaiblePath parentPath' key={'regType'+val.id} title={val.name} onClick={this.hlandleRegistryTypesSelect.bind(this,val.id)} >{val.name}</span>);
                });
            }

            var parentId = null;
            if (this.props.registryRegion.parents.length > 1)
                parentId = this.props.registryRegion.parents[0].id;
            navParents = (
                <div className="record-parent-info">
                    <div className='record-selected-name'>
                        <div className="icon"><Icon glyph="fa-folder-open" /></div>
                        <div className="title"  title={nazevRodice}>{nazevRodice}</div>
                        <div className="back" onClick={this.handleUnsetParents.bind(this,parentId)}><Icon glyph="fa-reply" /></div>
                    </div>
                    <div className='record-selected-breadcrumbs'>{cestaRodice.map((val, key)=>{if (key) {return  <span className='parentPath' key={key}><span className='parentPath'>&nbsp;|&nbsp;</span>{val}</span>} else {return <span key={key} className='parentPath'>{val}</span>;}})}</div>
                </div>
            )
        }

        var dropDownForSearch = <DropDownTree
            nullValue={{id: null, name: i18n('registry.all')}}
            key='search'
            items={this.props.refTables.recordTypes.items}
            value={this.props.registryRegion.registryTypesId}
            onChange={this.hlandleRegistryTypesSelect.bind(this)}
            />

        var arrPanel = null;

        if (registryRegion.panel.versionId != null) {
            arrPanel = <ArrPanel onReset={this.handleArrReset} name={registryRegion.panel.name} />
        }

        var leftPanel = (
            <div className="registry-list">
                <div>
                    {arrPanel}
                    {dropDownForSearch}
                    <Search
                        onSearch={this.handleSearch}
                        onClear={this.handleSearchClear.bind(this)}
                        placeholder={i18n('search.input.search')}
                        filterText={this.props.registryRegion.filterText}
                    />
                </div>
                <div>
                    {navParents}
                </div>
                <div className="registry-list-results">
                    {(!this.props.registryRegion.fetched) && <Loading/>}
                    {(this.props.registryRegion.fetched) && listOfRecord}
                </div>
            </div>
        )

        var centerPanel = (
            <div className='registry-page'>
                <RegistryPanel selectedId = {this.props.registryRegion.selectedId}/>

            </div>
        )

        return (
            <PageLayout
                splitter={splitter}
                key='registryPage'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}

            />
        )
    }
}
function mapStateToProps(state) {
    const {splitter, registryRegion, refTables} = state
    return {
        splitter,
        registryRegion,
        refTables
    }
}

module.exports = connect(mapStateToProps)(RegistryPage);
