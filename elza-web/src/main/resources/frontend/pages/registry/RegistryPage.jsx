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
import {registryData, registrySearchData} from 'actions/registry/registryData'

import {fetchRegistryIfNeeded} from 'actions/registry/registryList'

var RegistryPage = class RegistryPage extends AbstractReactComponent {
    constructor(props) {
        super(props);

        this.bindMethods('handleSelect');
        this.bindMethods('handleSearch');
        this.buildRibbon = this.buildRibbon.bind(this);
        this.dispatch(fetchRegistryIfNeeded(props.registry.search));

    }

    componentWillReceiveProps(nextProps) {
            this.dispatch(fetchRegistryIfNeeded(nextProps.registry.search));
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
                                    })

                        return (
                            <div key={item.recordId} className={cls} onClick={this.handleSelect.bind(this, item)}>
                                <span>{item.record}</span>
                            </div>
                        )
                    })}
                </div>
                <div key='registrysCouns' className='seznamRejstrikuCelkem'>Zobrazeno {this.props.registry.items.length} z celkoveho poctu {this.props.registry.countItems}</div>
            </div>
        )

        var leftPanel = (
            <div>
                <div>
                    <Search onSearch={this.handleSearch.bind(this)} filterText={this.props.registry.search}/>
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
