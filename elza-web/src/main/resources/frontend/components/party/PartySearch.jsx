/**
 * Komponenta hledání osob
 */

require ('./partySearch.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search} from 'components';
import {AppActions} from 'stores';

import {findPartyFetchIfNeeded} from 'actions/party/party.jsx'

var PartySearch = class PartySearch extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.handleSearch = this.handleSearch.bind(this);               // funkce pro akci spoustící vyhledávání
        this.state = {                                                  // inicializace stavu komponenty
            filterText: this.props.filterText,                          // hledaný text
            partyList: this.props.partyList                             // seznam vyhledaných osob
        } 

        //this.dispatch(findPartyFetchIfNeeded("aa"));

    }
    
    handleSearch(filterText){
        this.setState({
            filterText: filterText,                                     // uložení zadaného řezezce ve stavu komponenty
            partyList: null
        });
    }

    render() {
        /*
        var partyList = this.state.partyList((item) => {                                               // procházení všech nazelenýcch osob
                return  <li key={item.id} eventKey={item.id}>                                          // přidání osoby do seznamu
                            <span class="title">{title}</span>
                        </li>                          
        });
        */
var partyList = "aa";
        return  <div>
                    <Search onSearch={this.handleSearch} filterText={this.state.filterText}/>
                    <ul>
                        {partyList}
                    </ul>
                </div>
    }
}

module.exports = connect()(PartySearch);
