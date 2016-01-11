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
import {Ribbon, ModalDialog, NodeTabs, Search, RegistryPanel} from 'components';
import {ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {Nav, NavItem} from 'react-bootstrap';
import {registryData, registrySearchData, registryChangeParent} from 'actions/registry/registryData'

import {fetchRegistryIfNeeded} from 'actions/registry/registryList'

var RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelect');
        this.bindMethods('handleSearch');
        this.bindMethods('handleDoubleClick');
        this.bindMethods('handleClickNavigation');
        this.buildRibbon = this.buildRibbon.bind(this);
        this.dispatch(fetchRegistryIfNeeded(props.registry.search, props.registry.idRegistryParent));

    }

    componentWillReceiveProps(nextProps) {
            this.dispatch(fetchRegistryIfNeeded(nextProps.registry.search, nextProps.registry.idRegistryParent));
    }

    buildRibbon() {
        return (
            <Ribbon registry {...this.props} />
        )
    }

    handleSelect(registry, event) {
        var registry = Object.assign({}, registry,{selectedId: registry.recordId});
        this.dispatch(registryData(registry));
    }

    handleDoubleClick(item, event) {
        var registry = Object.assign({}, registry,{idRegistryParent: item.recordId});
        this.dispatch(registryChangeParent(registry));
    }

    handleClickNavigation(item, event) {
        
        var registry = Object.assign({}, registry,{idRegistryParent: item.id});
        this.dispatch(registryChangeParent(registry));
    }

    handleSearch(search, event) {
        var registry = Object.assign({}, registry,{search: search});
        this.dispatch(registrySearchData(registry));
    }

    render() {
        var navRows = (
            <div>
                <div key='registrysList'>
                    {this.props.registry.items.map(item=>{
                        var cls = classNames({
                                    active: this.props.registry.selectedId === item.recordId
                                    });
                        
                        var clsItem = 'registry-list-icon-record';
                        if (item.hasChildren === false)
                            clsItem = 'registry-list-icon-list';
                        
                        return (
                            <div key={item.recordId} className={cls} onDoubleClick={this.handleDoubleClick.bind(this, item)} onClick={this.handleSelect.bind(this, item)}>
                                <span className={clsItem}>{item.record}</span>
                            </div>
                        )
                    })}
                </div>
                <div key='registrysCouns' className='registry-list-count'>Zobrazeno {this.props.registry.items.length} z celkoveho poctu {this.props.registry.countItems}</div>
            </div>
        )
        
        var navParents = '';
console.log(this.props.registry);
        if (this.props.registry.items[0] && this.props.registry.search === null){ 
            navParents = (
                <ul className='breadcrumbs'>
                <li onClick={this.handleClickNavigation.bind(this, {id:null})}>/</li>
                {this.props.registry.items[0].parents.map(item => {
                    if (this.props.registry.selectedId===item.id)
                        return <li className='selected'>{item.record}</li>
                    return <li onClick={this.handleClickNavigation.bind(this, item)}>{item.record}</li>
                    }
                )}
                </ul>
            )
        } 

        var leftPanel = (
            <div>
                <div>
                    <Search onSearch={this.handleSearch.bind(this)} filterText={this.props.registry.search}/>
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
                RIGHT - registry
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
    const {registry} = state
    return {
        registry
    }
}

module.exports = connect(mapStateToProps)(RegistryPage);
