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
import {registryData, registrySearchData, registryChangeParent, registryRemoveRegistry, registryStartMove, registryStopMove, registryCancelMove} from 'actions/registry/registryData'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {fetchRegistryIfNeeded, registrySetTypesId, fetchRegistry} from 'actions/registry/registryList'
import {refRecordTypesFetchIfNeeded} from 'actions/refTables/recordTypes'



var RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('buildRibbon', 'handleSelectType', 'handleSelect', 'handleSearch', 'handleDoubleClick', 'handleClickNavigation', 'handleAddRegistry', 'handleCallAddRegistry', 'handleRemoveRegistryDialog', 'handleRemoveRegistry', 'handleStartMoveRegistry', 'handleSaveMoveRegistry', 'handleCancelMoveRegistry');
        this.dispatch(fetchRegistryIfNeeded(props.registry.filterText, props.registry.registryParentId, props.registry.registryTypesId));
        this.dispatch(refRecordTypesFetchIfNeeded());

    }

    componentWillReceiveProps(nextProps) {
            this.dispatch(fetchRegistryIfNeeded(nextProps.registry.filterText, nextProps.registry.registryParentId, nextProps.registry.registryTypesId));
            this.dispatch(refRecordTypesFetchIfNeeded());
    }

    handleSelectType(){

    }

    handleAddRegistry( parentId, event) {

       this.dispatch(
           modalDialogShow(this,
               i18n('registry.addRegistry'),
               <AddRegistryForm
                   create
                   onSubmit={this.handleCallAddRegistry.bind(this, parentId)}

                   />
           )
       );
    }

    handleCallAddRegistry(parentId, data ) {
        WebApi.insertRegistry( data.nameMain, data.characteristics, data.registerType, parentId ).then(json => {
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
            <Button onClick={this.handleAddRegistry.bind(this, this.props.registry.registryParentId)}><Icon glyph="fa-download" /><div><span className="btnText">{i18n('registry.addNewRegistry')}</span></div></Button>
        );


        altActions.push(
            <Button><Icon glyph='fa-download' /><div><span className="btnText">{i18n('ribbon.action.registry.import')}</span></div></Button>
        );

        var itemActions = [];
        if (this.props.registry.selectedId) {

            itemActions.push(
                <Button onClick={this.handleRemoveRegistryDialog.bind(this)}><Icon glyph="fa-trash" /><div><span className="btnText">{i18n('registry.removeRegistry')}</span></div></Button>
            );

            if (this.props.registry.selectedId!=this.props.registry.registryParentId) {
                itemActions.push(
                    <Button onClick={this.handleAddRegistry.bind(this, this.props.registry.selectedId)}><Icon glyph="fa-download" /><div><span className="btnText">{i18n('registry.addNewRegistryHere')}</span></div></Button>
                );
            }

            if (!this.props.registry.recordForMove){
                itemActions.push(
                    <Button onClick={this.handleStartMoveRegistry.bind(this)}><Glyphicon glyph="share-alt" /><div><span className="btnText">{i18n('registry.moveRegistry')}</span></div></Button>
                );
            }
            if (this.props.registry.recordForMove){
                itemActions.push(
                    <Button onClick={this.handleSaveMoveRegistry.bind(this)}><Icon glyph="fa-check-circle" /><div><span className="btnText">{i18n('registry.applyMove')}</span></div></Button>
                );
                itemActions.push(
                    <Button onClick={this.handleCancelMoveRegistry.bind(this)}><Icon glyph="fa-times" /><div><span className="btnText">{i18n('registry.cancelMove')}</span></div></Button>
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
        var registry = Object.assign({}, registry,{registryParentId: item.recordId});
        this.dispatch(registryChangeParent(registry));
    }

    handleClickNavigation(item, event) {
        var registry = Object.assign({}, registry,{registryParentId: item.id});
        this.dispatch(registryChangeParent(registry));
    }

    handleSearch(search, event) {
        var registry = Object.assign({}, registry,{filterText: search});
        this.dispatch(registrySearchData(registry));
    }

    hlandleRegistryTypesSelect(selectedId, event) {
        this.dispatch(registrySetTypesId(selectedId));
    }

    render() {

        var navRows = (
            <div>
                <div key='registrysList'>

                    {this.props.registry.records.map(item=>{
                        var cls = classNames({
                                    active: this.props.registry.selectedId === item.recordId,
                                    'search-result-row': 'search-result-row'
                                    });

                        var clsItem = 'registry-list-icon-list';
                        var doubleClick = this.handleDoubleClick.bind(this, item);
                        if (item.hasChildren === false) {
                            clsItem = 'registry-list-icon-record';
                            doubleClick = false;
                        }

                        // výsledky z vyhledávání
                        if ( this.props.registry.filterText!==null ) {
                            var path = '';
                            item.parents.map(parent => {
                                if (path !== '')
                                    path += '<';
                                path += parent.record;
                            });

                            return (
                                <div key={item.recordId} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                                    <div key={item.recordId} className="path">
                                        <span key={item.recordId}>{path}</span>
                                    </div>
                                    <span key={item.recordId} className={clsItem}>{item.record}</span>
                                </div>
                            )
                        }
                        else{
                            // jednořádkový výsledek
                            return (
                                <div key={item.recordId} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                                    <span key={item.recordId} className={clsItem}>{item.record}</span>
                                </div>
                            )
                        }
                    })}
                </div>
                <div key='registryCount' className='registry-list-count'>{i18n('registry.shown')} {this.props.registry.records.length} {i18n('registry.z.celkoveho.poctu')} {this.props.registry.countRecords}</div>
            </div>
        )

        var navParents = '';
        if (this.props.registry.records[0] && this.props.registry.filterText === null && this.props.registry.records[0].parents.length>0){
            var parentsArr = this.props.registry.records[0].parents.slice();
            navParents = (
                <ul className='breadcrumbs'>
                <li onClick={this.handleClickNavigation.bind(this, {id:null})}>/</li>
                {parentsArr.reverse().mapmap(item => {
                    if (this.props.registry.selectedId===item.id)
                        return <li key={item.id} className='selected'>{item.record}</li>
                    return <li key={item.id} onClick={this.handleClickNavigation.bind(this, item)}>{item.record}</li>
                    }
                )}
                </ul>
            )
        }


        var leftPanel = (
            <div className="registry-list">
                <div>
                    <Search
                        onSearch={this.handleSearch.bind(this)}
                        placeholder={i18n('search.input.search')}
                        afterInput={
                            <DropDownTree
                                nullValue = {{id: null, name: i18n('registry.all')}}
                                key='search'
                                items = {this.props.refTables.recordTypes.items}
                                value = {this.props.registry.registryTypesId}
                                onSelect = {this.hlandleRegistryTypesSelect.bind(this)}
                            />
                        }
                        filterText={this.props.registry.filterText}
                    />
                </div>
                <div>
                    {navParents}
                </div>
                <div>
                    {(this.props.registry.isFetching || !this.props.registry.fetched) && <Loading/>}
                    {(!this.props.registry.isFetching && this.props.registry.fetched) && navRows}
                </div>
            </div>
        )

        var centerPanel = (
            <div>
                <RegistryPanel selectedId = {this.props.registry.selectedId}/>

            </div>
        )

        var rightPanel = (
            <div>

            </div>
        )



        return (
            <PageLayout
                className='registry-page'
                ribbon={this.buildRibbon()}
                leftPanel={leftPanel}
                centerPanel={centerPanel}
                rightPanel={rightPanel}

            />
        )
    }
}
function mapStateToProps(state) {
    const {registry, refTables} = state
    return {
        registry, refTables
    }
}

module.exports = connect(mapStateToProps)(RegistryPage);
