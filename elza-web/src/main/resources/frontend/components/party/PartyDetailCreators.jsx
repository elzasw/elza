/**
 * Autoři zadané osoby 
 */

import React from 'react';
import {connect} from 'react-redux'
import {Button, Glyphicon} from 'react-bootstrap';
import {PartyCreatorForm, AbstractReactComponent, i18n} from 'components';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {AppActions} from 'stores';
import {deleteCreator, updateParty} from 'actions/party/party'


/**
 * PARTY DETAIL CREATORS
 * *********************************************
 * formulář autora osoby
 */
var PartyDetailCreators = class PartyDetailCreators extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.bindMethods(
            'handleAddCreator',
            'handleDeleteCreator', 
            'addCreator',
            'deleteCreator',
        );
    }

   /**
     * HANDLE DELETE CREATOR
     * *********************************************
     * Kliknutí na tlačítko smazání autora
     * @param creatorId - identifikátor autora osoby
     */     
    handleDeleteCreator(creatorId){
        if(confirm(i18n('party.detail.creator.delete'))){              // pokud uživatel potvrdí smazání
            this.deleteCreator(creatorId);                             // autor se smaže
        }
    }

   /**
     * DELETE CREATOR
     * *********************************************
     * Smazání autora
     * @param creatorId - identifikátor autora osoby
     */ 
    deleteCreator(creatorId){
        var party = this.props.partyRegion.selectedPartyData;                   // aktuální osoba
        var creators = []                                                       // nový seznam autorů
        for(var i = 0; i<party.creators.length; i++){                           // projdeme původní autory
            if(party.creators[i].creatorsId != creatorId){                      // a pokud se nejedna o mazaného autora
                creators[creators.length] = party.creators[i];                  // přidáme ho do seznamu autorů, co budou zachovány
            }
        }
        party.creators = creators;                                              // vyměníme starý seznam identifikátorů za nový
        this.dispatch(updateParty(party));                                      // změny uložíme
    }

   /**
     * ADD CREATOR
     * *********************************************
     * Vložení nového autora
     * @param obj data - data identifikátorů z formuláře
     */ 
    addCreator(data) {
        var party = this.props.partyRegion.selectedPartyData;                   // aktuálně upravovaná osoba
        party.creators[party.creators.length] = {                               // nového autora vložíme na konec seznamu autorů
            creatorId: data.creatorId
        }
        this.dispatch(updateParty(party));                                      // aurtor se uloží a osoba znovu načte     
    }

   /**
     * HANDLE ADD CREATOR
     * *********************************************
     * Kliknutí na vložení nového autora
     */ 
    handleAddCreator(){
        var party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        var data = {                                                                // výchozí data formuláře
            partyTypeId: party.partyType.partyTypeId,                               // identifikátor typu osoby, jejíž jménu upravujeme
        }
        this.dispatch(modalDialogShow(this, i18n('party.detail.creator.new') , <PartyCreatorForm initData={data} onSave={this.addCreator} />));    // otevře se formulář nového autora   
    }

   /**
     * HANDLE UPDATE CREATOR
     * *********************************************
     * Kliknutí na ikonu editace autora
     * @param creatorId - identifikátor autora osoby
     */ 
    handleUpdateCreator(creatorId){
        var party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        var creator = {};                                                           // pripravený objekt pro autora
        for(var i = 0; i<party.creators.length; i++){                               // prohledáme všechny autory
            if(party.creators[i].creatorId == creatorId){                           // a ten které hledáme
                creator = party.creators[i];                                        // si uložíme
            }
        };
       
        var data = {                                                                // data, která udou poslána formuláři
            creatorId: creator.creatorId,                                           // id upravovaného identifikátoru
            partyTypeId: party.partyType.partyTypeId                                // id typu osoby, jejíž identifikátor upravujeme
        }
        this.dispatch(modalDialogShow(this, "aa" , <PartyCreatorForm initData={data} onSave={this.updateCreator} />));
    }

   /**
     * UPDATE CREATOR
     * *********************************************
     * Uložení změn autora
     * @param obj data - data vyplněná v formuláři 
     */ 
    updateCreator(data){
        var party = this.props.partyRegion.selectedPartyData;                       // id osoby, které patří měněný identifikátor
        var creators = party.partyGroupCreators;                                    // původní autoři osoby
        for(var i = 0; i<creators.length; i++){                                     // je potřeba měněného autora najít mezi ostatními autory
            if(creators[i].partyGroupCreatorId == data.partyGroupCreatorId){        // to je on!            
                party.creators[i].creatorId = data.creatorId;                       // identifikátor autora
            }
        }
        this.dispatch(updateParty(party));                                          // uložení změn a znovu načtení dat osoby              
    }

   /**
     * RENDER
     * *********************************************
     * Vykreslení bloku identifikátorů
     */ 
    render() {
        var party = this.props.partyRegion.selectedPartyData;
        return  <div className="party-creators">
                    <table>
                        <tbody>
                            {party.creators.map(i=> {return <tr className="creator">
                                <th className="creator column">{i.name}</th> 
                                <td className="buttons">
                                    <Button classCreator="column" onClick={this.handleUpdateCreator.bind(this, i.creatorId)}><Glyphicon glyph="edit" /></Button>
                                    <Button classCreator="column" onClick={this.handleDeleteCreator.bind(this, i.creatorId)}><Glyphicon glyph="trash" /></Button>
                                </td>
                            </tr>})}
                        </tbody>
                    </table>
                    <Button className="column" onClick={this.handleAddCreator}><Glyphicon glyph="plus" /> { i18n('party.detail.creator.new')}</Button>
                </div>
    }
}

module.exports = connect()(PartyDetailCreators);
