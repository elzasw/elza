/**
 * Komponenta hledání osob
 */

require ('./partySearch.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search, i18n} from 'components';
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

    handlePartyDetail(item, e){
        this.dispatch(partyDetailFetchIfNeeded(item.partyId));
    }

    render() {
        if(this.props.items && this.props.items.length>0){
            console.log(this.props.items);
            var partyList = this.props.items.map((item) => {                                               // přidání všech nazelených osob
                    return  <li 
                                key={item.partyId} 
                                eventKey={item.partyId} 
                                className={item.partyId==this.props.selectedPartyID ? 'active' : ''} 
                                onClick={this.handlePartyDetail.bind(this,item)}
                            >                                          
                                <span className="name">{item.record.record}</span> 
                                <span>{item.partyType.description}</span>
                                <span>1976-2015</span>
                            </li>                          
            });
        }else{
            var label = i18n('search.action.noResult'); ;
            var partyList = <li className="noResult">{label}</li>
        }
        return  <div className="party-list">
                    <Search onSearch={this.handleSearch} filterText={this.props.filterText}/>
                    <ul className="partySearch">
                        {partyList}
                    </ul>
                </div>
    }
}

module.exports = connect()(PartySearch);
