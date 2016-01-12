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
import {RibbonGroup,Ribbon, ModalDialog, NodeTabs, Search, RegistryPanel} from 'components';
import {MenuItem, DropdownButton, ButtonGroup, Button, Glyphicon} from 'react-bootstrap';
import {PageLayout} from 'pages';
import {Nav, NavItem} from 'react-bootstrap';
import {registryData, registrySearchData, registryChangeParent} from 'actions/registry/registryData'

import {fetchRegistryIfNeeded} from 'actions/registry/registryList'

var RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('buildRibbon', 'handleSelect', 'handleSearch', 'handleDoubleClick', 'handleClickNavigation');
        this.dispatch(fetchRegistryIfNeeded(props.registry.search, props.registry.idRegistryParent));

    }

    componentWillReceiveProps(nextProps) {
            this.dispatch(fetchRegistryIfNeeded(nextProps.registry.search, nextProps.registry.idRegistryParent));
    }

    buildRibbon() {
        var isSelected = this.props.registry.selectedId;

        var actions = [];

        isSelected && actions.push(
            <Button><Glyphicon glyph="share-alt" /><div><span className="btnText">Přesun hesla</span></div></Button>
        );
        actions.push(
            <DropdownButton title={<span className="dropContent"><Glyphicon glyph='plus-sign' /><div><span className="btnText">Import</span></div></span>}>
                <MenuItem eventKey="1">Hesel</MenuItem>
            </DropdownButton>
        );
        isSelected && actions.push(
            <Button><Glyphicon glyph="ok" /><div><span className="btnText">Validace</span></div></Button>
        );

        var altSection = <RibbonGroup className="large">{actions}</RibbonGroup>

        return (
            <Ribbon registry altSection={altSection} {...this.props} />
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
                        
                        var clsItem = 'registry-list-icon-list';
                        var doubleClick = this.handleDoubleClick.bind(this, item);
                        if (item.hasChildren === false) {
                            clsItem = 'registry-list-icon-record';
                            doubleClick = false;
                        }
                         
                        // výsledky z vyhledávání
                        if ( this.props.registry.search!==null ) {
                            var path = '';
                            item.parents.map(parent => {
                                path+= '/'+parent.record;
                            });
                            
                            return (
                                <div key={item.recordId} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                                    <div className="path">
                                        <span>{path.substr(0,5)}</span><span>{path.substr(5)}</span>
                                    </div>
                                    <span className={clsItem}>{item.record}</span>
                                </div>
                            )
                        }
                        else{
                            // jednořádkový výsledek
                            return (
                                <div key={item.recordId} className={cls} onDoubleClick={doubleClick} onClick={this.handleSelect.bind(this, item)}>
                                    <span className={clsItem}>{item.record}</span>
                                </div>
                            )
                        }
                    })}
                </div>
                <div key='registrysCouns' className='registry-list-count'>Zobrazeno {this.props.registry.items.length} z celkoveho poctu {this.props.registry.countItems}</div>
            </div>
        )
        
        var navParents = '';
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
            <div className="registry-list">
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
