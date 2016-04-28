/**
 * Identifikátory zadané osoby - pouze u korporací
 */

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {PartyIdentifierForm, AbstractReactComponent, i18n, Icon} from 'components';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {AppActions} from 'stores';
import {deleteIdentifier, updateParty} from 'actions/party/party'

/*
@@@@@@@@@@@@@@@@@@@

POZOR - zde se pracuje s props.partyRegion.selectedPartyData a to se primo upravuje !!!!!!!!!!!!!!!!!!!!!!!

@@@@@@@@@@@@@@@@@@@
*/

var PartyDetailIdentifiers = class PartyDetailIdentifiers extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleAddIdentifier',
            'handleUpdateIdentifier',
            'handleDeleteIdentifier', 
            'addIdentifier',
            'updateIdentifier',
            'deleteIdentifier',
        );

        var party = this.props.partyRegion.selectedPartyData
        if (!party.partyGroupIdentifiers) {
            party.partyGroupIdentifiers = []
        }
    }

   /**
     * HANDLE DELETE IDENTIFIER
     * *********************************************
     * Kliknutí na tlačítko smazání identifikátoru
     * @param identifierId - identifikátor jména osoby
     * @param event - událost kliknutí
     */     
    handleDeleteIdentifier(identifierId, event){
        if(confirm(i18n('party.detail.identifier.delete'))){              // pokud uživatel potvrdí smazání
            this.deleteIdentifier(identifierId);                          // identifikátor se smaže
        }
    }

   /**
     * DELETE IDENTIFIER
     * *********************************************
     * Smazání identifikátoru
     * @param identifierId - identifikátor jména osoby
     */ 
    deleteIdentifier(identifierId){
        var party = this.props.partyRegion.selectedPartyData;                               // aktuální osoba
        var identifiers = []                                                                // nový seznam identifikátorů
        for(var i = 0; i<party.partyGroupIdentifiers.length; i++){                          // projdeme původní identifikátory
            if(party.partyGroupIdentifiers[i].partyGroupIdentifierId != identifierId){      // a pokud se nejedna o mazaný identifikátor
                identifiers[identifiers.length] = party.partyGroupIdentifiers[i];           // přidáme ho do seznamu identifikátorů, co budou zachovány
            }
        }
        party.partyGroupIdentifiers = identifiers;                                          // vyměníme starý seznam identifikátorů za nový
        this.dispatch(updateParty(party));                                                  // změny uložíme
    }

   /**
     * ADD IDENTIFIER
     * *********************************************
     * Vložení nového identifikátorů
     * @param obj data - data identifikátorů z formuláře
     */ 
    addIdentifier(data) {
        var party = this.props.partyRegion.selectedPartyData;                   // aktuálně upravovaná osoba
        party.partyGroupIdentifiers[party.partyGroupIdentifiers.length] = {     // nový identifikátor vložíme na konec seznamu jmen
            note: data.note,
            identifier: data.identifier,
            source: data.source,
            from: data.from,
            to: data.to
        }
        this.dispatch(updateParty(party));                                      // identifikátor se uloží a osoba znovu načte     
    }

   /**
     * HANDLE ADD IDENTIFIER
     * *********************************************
     * Kliknutí na vložení nového identifikátoru
     */ 
    handleAddIdentifier(){
        var party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        var data = {                                                                // výchozí data formuláře
            partyTypeId: party.partyType.partyTypeId,                               // identifikátor typu osoby, jejíž jménu upravujeme
            from: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId        
            },
            to: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId           
            }
        }
        this.dispatch(modalDialogShow(this, i18n('party.detail.identifier.new') , <PartyIdentifierForm initData={data} onSave={this.addIdentifier} />));    // otevře se formuláš nového identifikátoru   
    }

   /**
     * HANDLE UPDATE IDENTIFIER
     * *********************************************
     * Kliknutí na ikonu editace identifikátoru
     * @param identifierId - identifikátor jména osoby
     */ 
    handleUpdateIdentifier(identifierId){
        var party = this.props.partyRegion.selectedPartyData;                               // načtení aktualní osoby ze store
        var identifier = {};                                                                // pripravený objekt pro identifikátor
        for(var i = 0; i<party.partyGroupIdentifiers.length; i++){                          // prohledáme všechny identifikátory
            if(party.partyGroupIdentifiers[i].partyGroupIdentifierId == identifierId){      // a ten které hledáme
                identifier = party.partyGroupIdentifiers[i];                                // si uložíme
            }
        };
       
        var data = {                                                                // data, která udou poslána formuláři
            partyGroupIdentifierId: identifier.partyGroupIdentifierId,              // id upravovaného identifikátoru
            partyTypeId: party.partyType.partyTypeId,                               // id typu osoby, jejíž identifikátor upravujeme
            source: identifier.source,                                              // zdroje dat
            note: identifier.note,                                                  // poznámka
            identifier: identifier.identifier,                                      // identifikátor
            from: {                                                                 // datace od
                textDate: (identifier.from != null ? identifier.from.textDate : ""),    
                calendarTypeId: (identifier.from != null ? identifier.from.calendarTypeId : 0)
            },
            to: {                                                                   // datace do
                textDate: (identifier.to != null ? identifier.to.textDate : ""), 
                calendarTypeId: (identifier.to != null ? identifier.to.calendarTypeId : 0)
            }
        }
        this.dispatch(modalDialogShow(this, identifier.mainPart , <PartyIdentifierForm initData={data} onSave={this.updateIdentifier} />));
    }

   /**
     * UPDATE IDENTIFIER
     * *********************************************
     * Uložení změn v identifikátoru
     * @param obj data - data vyplněná v formuláři 
     */ 
    updateIdentifier(data){
        console.log("DATA");
        console.log(data);
        var party = this.props.partyRegion.selectedPartyData;                           // id osoby, které patří měněný identifikátor
        var identifiers = party.partyGroupIdentifiers;                                  // původní identifikátory osoby
        for(var i = 0; i<identifiers.length; i++){                                      // je potřeba ho najít mezi ostatními jmény
            if(identifiers[i].partyGroupIdentifierId == data.partyGroupIdentifierId){   // to je ono            
                party.partyGroupIdentifiers[i].source = data.source;                    // zdroj dat
                party.partyGroupIdentifiers[i].note = data.note;                        // poznámka
                party.partyGroupIdentifiers[i].identifier = data.identifier;            // identifikátoru
                party.partyGroupIdentifiers[i].from = data.from;                        // datace identifikátoru od
                party.partyGroupIdentifiers[i].to = data.to;                            // datace identifikátoru do
            }
        }
        this.dispatch(updateParty(party));                                              // uložení změn a znovu načtení dat osoby              
    }

   /**
     * RENDER
     * *********************************************
     * Vykreslení bloku identifikátorů
     */ 
    render() {
       const {canEdit} = this.props

        var party = this.props.partyRegion.selectedPartyData;
        return  <div className="party-identifiers">
                    <table>
                        <tbody>
                            {party.partyGroupIdentifiers.map(i=> {return <tr className="identifier">
                                <th className="identifier column">{i.identifier}</th> 
                                <td className="buttons">
                                    {canEdit && <Button classIdentifier="column" onClick={this.handleUpdateIdentifier.bind(this, i.partyGroupIdentifierId)}><Icon glyph="fa-pencil" /></Button>}
                                    {canEdit && <Button classIdentifier="column" onClick={this.handleDeleteIdentifier.bind(this, i.partyGroupIdentifierId)}><Icon glyph="fa-trash" /></Button>}
                                </td>
                            </tr>})}
                        </tbody>
                    </table>
                    {canEdit && <Button className="column" onClick={this.handleAddIdentifier}><Icon glyph="fa-plus" /> { i18n('party.detail.identifier.new')}</Button>}
                </div>
    }
}

module.exports = connect()(PartyDetailIdentifiers);
