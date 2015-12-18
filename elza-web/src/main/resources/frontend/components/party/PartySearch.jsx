/**
 * Komponenta hledání osob
 */

require ('./partySearch.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search} from 'components';
import {AppActions} from 'stores';

import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded} from 'actions/party/party.jsx'

var PartySearch = class PartySearch extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.handleSearch = this.handleSearch.bind(this);               // funkce pro akci spoustící vyhledávání
        this.handlePartyDetail = this.handlePartyDetail.bind(this);     // funkce vyberu osoby zobrazeni detailu
        this.dispatch(findPartyFetchIfNeeded(this.props.filterText));
    }
    
    handleSearch(filterText){
        this.dispatch(findPartyFetchIfNeeded(filterText));
    }

    handlePartyDetail(e){
        this.dispatch(partyDetailFetchIfNeeded(3));
    }

    render() {
        var partyList = this.props.items.map((item) => {                                               // přidání všech nazelených osob
                return  <li key={item.id} eventKey={item.id} onClick={this.handlePartyDetail}>                                          
                            <span className="name">{item.name}</span>
                        </li>                          
        });

        return  <div>
                    <Search onSearch={this.handleSearch} filterText={this.props.filterText}/>
                    <ul>
                        {partyList}
                    </ul>
                </div>
    }
}

module.exports = connect()(PartySearch);
