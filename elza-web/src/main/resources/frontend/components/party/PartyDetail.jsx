/**
 * Komponenta detailu osoby
 */

require ('./PartyFormStyles.less');

import React from 'react';
import {connect} from 'react-redux'
import {Button, Input, SplitButton} from 'react-bootstrap';
import {PartyDetailCreators, PartyDetailIdentifiers, PartyDetailNames, AbstractReactComponent, Search, i18n} from 'components';
import {AppActions} from 'stores';
import {refPartyTypesFetchIfNeeded} from 'actions/refTables/partyTypes'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes'
import {updateParty} from 'actions/party/party'
import {findPartyFetchIfNeeded} from 'actions/party/party'

/**
 * PARTY DETAIL
 * *********************************************
 * Detail osoby
 */ 
var PartyDetail = class PartyDetail extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(refPartyTypesFetchIfNeeded());    // nacteni typu osob (osoba, rod, událost, ...)
        this.dispatch(calendarTypesFetchIfNeeded());    // načtení typů kalendářů (gregoriánský, juliánský, ...)
        this.bindMethods(                               // pripojením funkcím "this"
            'updateValue',                              // aktualizace nějaké hodnoty
            'changeValue'                               // změna nějakéh políčka ve formuláři
        );
        this.state={
            party: this.props.partyRegion.selectedPartyData,
            dynastyId: 2,
            groupId: 3,
        };
    }

    /**
     * CHANGE VALUE
     * *********************************************
     * Zpracování změny nějaké hodnoty ve formuláři
     * @param event - událost změny
     */ 
    changeValue(event){
        var value = event.target.value;                             // hodnota změna         
        var variable = event.target.name;                           // políčko (název hodnoty) změny
        var p = this.props.partyRegion.selectedPartyData;           // původní osoba
        var party = this.mergePartyChanges(p, variable, value);     // osoba po změne 
        this.setState({party: party});                              // znovuvykresleni formuláře se změnou
    }

    /**
     * UPDATE VALUE
     * *********************************************
     * Uložení změny nekteré hodnoty
     * @param event - událost, která změnu vyvolala
     */ 
    updateValue(event){
        var value = event.target.value;                             // hodnota změna         
        var variable = event.target.name;                           // políčko (název hodnoty) změny
        var party = this.props.partyRegion.selectedPartyData;       // původní osoba
        party = this.mergePartyChanges(party, variable, value);     // osoba po změne 
        this.dispatch(updateParty(party));                          // uložení změn a znovu načtení dat osoby   
        this.setState({party:null});
    }

    /**
     * MERGE PARTY CHANGES
     * *********************************************
     * Sloučí změnu v jednou políčku s původním objektem osoby
     * @param party - původní osoba
     * @param variable - název měné hodnoty 
     * @param value - nová hoddnota měnené položky
     */ 
    mergePartyChanges(party, variable, value){
        switch(variable){
            case "history" : party.history = value; break;
            case "sourceInformation" : party.sourceInformation = value; break;
            case "fromText" : party.from.textDate = value;  break;
            case "toText" : party.to.textDate = value;  break;
            case "fromCalendar" : party.from.calendarTypeId = value;  break;
            case "toCalendar" : party.to.calendarTypeId = value;  break;
            case "genealogy" : party.genealogy = value; break;
            case "organization" : party.organization = value; break;
        };
        return party;
    }

    /**
     * RENDER
     * *********************************************
     * Vykreslení detailu osoby
     */ 
    render() {
        if(this.state.party){
            var party = this.state.party;
        }else{
            var party = this.props.partyRegion.selectedPartyData;
        }
        if(!party){
            return <div>Nenalezeno</div>
        }
        return <div className={"partyDetail"}>
                    <h1>{party.record.record}</h1>
                    <label>{i18n('party.detail.characteristics')}</label>
                    <p className={"characteristics"}>{party.record.characteristics}</p>

                    <div className="line">
                        <Input type="select" disabled={true} value={party.partyType.partyTypeId} label={i18n('party.detail.type')}>
                            {this.props.refTables.partyTypes.items.map(i=> {return <option key={i.partyTypeId} value={i.partyTypeId}>{i.name}</option>})}
                        </Input>
                        <Input type="text" label={i18n('party.detail.number')}/>
                    </div>
                    <div className="line">
                        <Input type="text" label={i18n('party.nameValidFrom')} name="fromText" value={party.from == null || party.from.textDate == null ? '' : party.from.textDate} onChange={this.changeValue} onBlur={this.updateValue}/>
                        <Input type="select" label={i18n('party.calendarTypeFrom')} name="fromCalendar" value={party.from == null || party.from.calendarTypeId == null ? 0 : party.from.calendarTypeId} onChange={this.updateValue}>
                            <option value="0" key="0"></option> 
                            {this.props.refTables.calendarTypes.items.map(i=> {return <option key={i.id} value={i.id}>{i.name}</option>})}
                        </Input>
                    </div>

                    <div className="line">
                        <Input type="text" label={i18n('party.nameValidTo')} name="toText" value={(party.to == null || party.to.textDate == null ? '' : party.to.textDate)} onChange={this.changeValue} onBlur={this.updateValue}/>
                        <Input type="select" label={i18n('party.calendarTypeTo')} name="toCalendar" value={party.to == null || party.to.calendarTypeId == null ? 0 : party.to.calendarTypeId} onChange={this.updateValue}>
                            <option value="0" key="0"></option> 
                            {this.props.refTables.calendarTypes.items.map(i=> {return <option key={i.id} value={i.id}>{i.name}</option>})}
                        </Input>
                    </div>                       
                    <hr/>

                    <div className="party-names">
                        <label>{i18n('party.detail.names')}</label>
                        <PartyDetailNames partyRegion={this.props.partyRegion} /> 
                        <hr/>
                    </div>

                    {party.partyType.partyTypeId == this.state.groupId ? <div className="party-identifiers">
                        <label>{i18n('party.detail.identifiers')}</label>
                        <PartyDetailIdentifiers partyRegion={this.props.partyRegion} />
                        <hr/>
                    </div> :  ''}


                    {party.partyType.partyTypeId == this.state.dynastyId ? <Input type="text" label={i18n('party.detail.history')} name="genealogy" value={party.genealogy != undefined ? party.genealogy : ''} onChange={this.changeValue} onBlur={this.updateValue}/> :  ''}

                    <Input type="text" label={i18n('party.detail.note')} name="note" />
                    <Input type="text" label={i18n('party.detail.history')} name="history" value={party.history != undefined ? party.history : ''} onChange={this.changeValue} onBlur={this.updateValue}/>
                    <Input type="text" label={i18n('party.detail.sources')} name="sourceInformation" value={party.sourceInformation == null ? '' : party.sourceInformation} onChange={this.changeValue} onBlur={this.updateValue}/>

                    {party.partyType.partyTypeId == this.state.groupId ? <Input type="text" label={i18n('party.detail.groupFunction')} name="organization" value={party.organization != undefined ? party.organization : ''} onChange={this.changeValue} onBlur={this.updateValue}/> :  ''}
    

                    <div className="party-creators">
                        <label>{i18n('party.detail.creators')}</label>
                        <PartyDetailCreators partyRegion={this.props.partyRegion} /> 
                        <hr/>
                    </div>

                </div>
    }
}

module.exports = connect()(PartyDetail);
