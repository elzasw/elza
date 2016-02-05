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
import {Icon, RibbonGroup,Ribbon, ModalDialog, NodeTabs, Search, RegistryPanel, DropDownTree, AddRegistryForm} from 'components';
import {WebApi} from 'actions'
import {MenuItem, DropdownButton, ButtonGroup, Button} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {Nav, Glyphicon, NavItem} from 'react-bootstrap';
import {registryData, registrySearchData, registryClearSearch, registryChangeParent, registryRemoveRegistry, registryStartMove, registryStopMove, registryCancelMove, registryUnsetParents} from 'actions/registry/registryData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {fetchRegistryIfNeeded, registrySetTypesId, fetchRegistry} from 'actions/registry/registryList'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'

var RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods('buildRibbon', 'handleSelect', 'handleSearch', 'handleSearchClear', 'handleDoubleClick', 'handleClickNavigation', 'handleAddRegistry', 'handleCallAddRegistry', 'handleRemoveRegistryDialog', 'handleRemoveRegistry', 'handleStartMoveRegistry', 'handleSaveMoveRegistry', 'handleCancelMoveRegistry', 'handleCloseTypesRegistry', 'handleUnsetParents');

    }

    componentWillReceiveProps(nextProps) {
        this.dispatch(fetchRegistryIfNeeded(nextProps.registry.filterText, nextProps.registry.registryParentId, nextProps.registry.registryTypesId));
        this.dispatch(refRecordTypesFetchIfNeeded());
    }

    componentDidMount(){
        this.dispatch(fetchRegistryIfNeeded(this.props.registry.filterText, this.props.registry.registryParentId, this.props.registry.registryTypesId));
        this.dispatch(refRecordTypesFetchIfNeeded());
    }

    handleAddRegistry( parentId, event) {
        var registryParentTypesId = this.props.registry.registryTypesId;
        if (this.props.registry.registryData){
            registryParentTypesId = this.props.registry.registryData.item.registerTypeId;
        }
        this.dispatch(
           modalDialogShow(this,
               i18n('registry.addRegistry'),
               <AddRegistryForm
                   create
                   onSubmit={this.handleCallAddRegistry.bind(this, parentId)}
                   parentRecordId = {parentId}
                   parentRegisterTypeId = {registryParentTypesId}
                   />
           )
       );
    }

    handleCallAddRegistry(parentId, data ) {

        WebApi.insertRegistry( data.nameMain, data.characteristics, data.registerTypeId, parentId, data.scopeId ).then(json => {
            this.dispatch(modalDialogHide());
            this.dispatch(fetchRegistry(this.props.registry.filterText, this.props.registry.registryParentId, this.props.registry.registryTypesId));
            this.dispatch(registryData({selectedId: json.recordId}));
        });

    }

    handleRemoveRegistryDialog(){
        var result = confirm(i18n('registry.removeRegistryQuestion'));
        if (result) {
            this.dispatch(this.handleRemoveRegistry());
        }

    }
    handleRemoveRegistry() {
        WebApi.removeRegistry( this.props.registry.selectedId ).then(json => {
            var registry = Object.assign({}, registry);
            this.dispatch(registryRemoveRegistry(registry));
        });
    }


    handleStartMoveRegistry(){
        this.dispatch(registryStartMove());
    }

    handleSaveMoveRegistry(){
        var data = Object.assign({}, this.props.registry.recordForMove);
        data['parentRecordId'] = this.props.registry.selectedId;
        WebApi.updateRegistry(data).then(json => {
            this.dispatch(registryStopMove());
        });
    }
    handleCancelMoveRegistry(){
        var registry = Object.assign({}, registry);
        this.dispatch(registryCancelMove(registry));
    }

    buildRibbon() {


        var altActions = [];
        altActions.push(
            <Button key='addRegistry' onClick={this.handleAddRegistry.bind(this, this.props.registry.registryParentId)}><Icon glyph="fa-download" /><div><span className="btnText">{i18n('registry.addNewRegistry')}</span></div></Button>
        );


        altActions.push(
            <Button key='registryImport'><Icon glyph='fa-download' /><div><span className="btnText">{i18n('ribbon.action.registry.import')}</span></div></Button>
        );

        var itemActions = [];
        if (this.props.registry.selectedId) {

            itemActions.push(
                <Button key='registryRemove' onClick={this.handleRemoveRegistryDialog.bind(this)}><Icon glyph="fa-trash" /><div><span className="btnText">{i18n('registry.removeRegistry')}</span></div></Button>
            );

            if (!this.props.registry.recordForMove && this.props.registry.registryData && !this.props.registry.registryData.item.partyId){
                itemActions.push(
                    <Button key='registryMove' onClick={this.handleStartMoveRegistry.bind(this)}><Icon glyph="fa-share" /><div><span className="btnText">{i18n('registry.moveRegistry')}</span></div></Button>
                );
            }
            if (this.props.registry.recordForMove && this.props.registry.registryData && !this.props.registry.registryData.item.partyId){
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
        this.dispatch(registryData(registry));
    }

    handleDoubleClick(item, event) {
        var rodice = item.parents.slice();
        rodice.push(item.record);
        var registry = Object.assign({}, registry,{registryParentId: item.recordId, parents: rodice, filterText: ''});
        this.dispatch(registryChangeParent(registry));
        this.dispatch(registryClearSearch());

    }

    handleClickNavigation(item, event) {
        var registry = Object.assign({}, registry,{registryParentId: item.id});
        this.dispatch(registryChangeParent(registry));
    }

    handleSearch(search, event) {
        var registry = Object.assign({}, registry,{filterText: search});
        this.dispatch(registrySearchData(registry));
        this.dispatch(registryUnsetParents(null));
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

    handleUnsetParents(){
        this.dispatch(registryUnsetParents(null));
    }
    render() {
        const {splitter} = this.props;

        var listOfRecord = <div className='search-norecord'>{i18n('registry.list.norecord')}</div>;
        if (this.props.registry.records.length) {
            listOfRecord = this.props.registry.records.map(item=>{
                var cls = classNames({
                    active: this.props.registry.selectedId === item.recordId,
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
                if ( this.props.registry.filterText!==null ) {
                    var path = item.parents.join(' | ');
                    if (path && item.typesToRoot)
                        path += ' | ';
                    if (item.typesToRoot)
                        path += item.typesToRoot.join(' | ');

                    return (
                        <div key={item.recordId} title={path} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                            <div><Icon glyph={iconName} /></div>
                            <div className={clsItem}>{item.record}</div>
                            <div className="path" >{path}</div>
                        </div>
                    )
                }
                else{
                    // jednořádkový výsledek
                    return (
                        <div key={item.recordId} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                            <div><Icon glyph={iconName} key={item.recordId} /></div>
                            <div key={item.recordId} className={clsItem}>{item.record}</div>
                        </div>
                    )
                }
            })
        }
        var navRows = (
            <div className="registry-nav">
                <div key='registrysList'>

                    {listOfRecord}
                </div>
            </div>
        )

        var navParents = '';
        if (this.props.registry.parents){
            var nazevRodice = this.props.registry.parents[this.props.registry.parents.length-1];
            var cestaRodice = this.props.registry.parents.slice();
            cestaRodice.pop();
            navParents = (
                <div className="record-parent-info">
                    <div className='record-selected-name'>
                        <div><Icon glyph="fa-folder-open" /></div>
                        <div>{nazevRodice}</div>
                        <div onClick={this.handleUnsetParents}><Icon glyph="fa-mail-reply" /></div>
                    </div>
                    <div className='record-selected-breadcrumbs'>{cestaRodice.join(' | ')}</div>
                </div>
            )
        }

        var dropDownForSearch = <DropDownTree
            nullValue={{id: null, name: i18n('registry.all')}}
            key='search'
            items={this.props.refTables.recordTypes.items}
            value={this.props.registry.registryTypesId}
            onChange={this.hlandleRegistryTypesSelect.bind(this)}
            />

        var leftPanel = (
            <div className="registry-list">
                <div>
                    <Search
                        onSearch={this.handleSearch.bind(this)}
                        onClear={this.handleSearchClear.bind(this)}
                        placeholder={i18n('search.input.search')}
                        beforeInput={dropDownForSearch}
                        filterText={this.props.registry.filterText}
                    />
                </div>
                <div>
                    {navParents}
                </div>
                <div className="registry-list-results">
                    {(this.props.registry.isFetching || !this.props.registry.fetched) && <Loading/>}
                    {(!this.props.registry.isFetching && this.props.registry.fetched) && navRows}
                </div>
            </div>
        )

        var centerPanel = (
            <div className='registry-page'>
                <RegistryPanel selectedId = {this.props.registry.selectedId}/>

            </div>
        )

        var rightPanel = (
            <div>

            </div>
        )

        return (
            <PageLayout
                splitter={splitter}
                key='registryPage'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}

            />
        )
    }
}
function mapStateToProps(state) {
    const {splitter, registry, refTables} = state
    return {
        splitter,
        registry,
        refTables
    }
}

module.exports = connect(mapStateToProps)(RegistryPage);
