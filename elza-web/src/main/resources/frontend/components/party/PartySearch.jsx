/**
 * Komponenta hledání osob
 */

require('./PartySearch.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {AbstractReactComponent, Search, i18n, ArrPanel} from 'components';
import {AppActions} from 'stores';

import {findPartyFetchIfNeeded, partyDetailFetchIfNeeded, partyArrReset} from 'actions/party/party.jsx'


var PartySearch = class PartySearch extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.handleSearch = this.handleSearch.bind(this);               // funkce pro akci spoustící vyhledávání
        this.handlePartyDetail = this.handlePartyDetail.bind(this);     // funkce vyberu osoby zobrazeni detailu
        this.handleClearSearch = this.handleClearSearch.bind(this);
    }

    handleSearch(filterText){
        const {partyRegion} = this.props;
        this.dispatch(findPartyFetchIfNeeded(filterText, partyRegion.panel.versionId));
    }

    handleClearSearch(){
        const {partyRegion} = this.props;
        this.dispatch(findPartyFetchIfNeeded(null, partyRegion.panel.versionId));
    }

    handlePartyDetail(item, e){
        this.dispatch(partyDetailFetchIfNeeded(item.partyId));
    }

    handleArrReset() {
        this.dispatch(partyArrReset());
    }

    render() {
        const {partyRegion} = this.props;

        var partyList = this.props.items;
        if(partyList && partyList.length>0){
            var description = '';
            for(var i = 0; i<partyList.length; i++){                                                            // projdu vsechny polozky abych jim nastavil popisek
                if(partyList[i].record && partyList[i].record.characteristics){                                 // pokud ma popisek zadany
                    var lineEndPosition = partyList[i].record.characteristics.indexOf("\n")                     // zjistim kde konci první řádek
                    if(lineEndPosition>=0){                                                                     // pokud vubec nekde konci (popisek muze mit jen 1 radek)
                        description = partyList[i].record.characteristics.substring(0, lineEndPosition);        // oriznu ho do konce radku
                    }else{
                        description = partyList[i].record.characteristics;                                      // jinak ho necham cely                
                    }   
                    partyList[i].record.description = description;                                              // ulozim popisek k objektu                
                }else{
                    partyList[i].record.description = '';                                                       // ulozim popisek k objektu                                                                           // popisek nezadan, nastavim prazdny
                }

            }
            
            var partyList = partyList.map((item) => {                                                           // přidání všech nazelených osob

                    return  <li 
                                key={item.partyId} 
                                eventKey={item.partyId} 
                                className={item.partyId==this.props.selectedPartyID ? 'active' : ''} 
                                onClick={this.handlePartyDetail.bind(this,item)}
                            >                                          
                                <span className="name">{item.record.record}</span>
                                <span>{item.partyType.description + " | " + item.partyId}</span>
                                <span title={item.record.description}>{item.record.description}</span>
                            </li>                          
            });
        }else{
            var label = i18n('search.action.noResult'); ;
            var partyList = <li className="noResult">{label}</li>
        }

        var arrPanel = null;

        if (partyRegion.panel.versionId != null) {
            arrPanel = <ArrPanel onReset={this.handleArrReset} name={partyRegion.panel.name} />
        }

        return  <div className="party-list">
                    <div>
                        {arrPanel}
                        <Search placeholder={i18n('search.input.search')} onSearch={this.handleSearch} filterText={this.props.filterText} onClear={this.handleClearSearch}/>
                    </div>
                    <ul className="partySearch">
                        {partyList}
                    </ul>
                </div>
    }
}

function mapStateToProps(state) {
    const {partyRegion} = state
    return {
        partyRegion
    }
}

module.exports = connect(mapStateToProps)(PartySearch);
