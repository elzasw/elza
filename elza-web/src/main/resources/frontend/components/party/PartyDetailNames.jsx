/**
 * Jména zadané osoby
 */

import React from 'react';
import {connect} from 'react-redux'
import {Button} from 'react-bootstrap';
import {PartyNameForm, AbstractReactComponent, i18n, Icon} from 'components/index.jsx';
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {AppActions} from 'stores/index.jsx';
import {deleteName, updateParty} from 'actions/party/party.jsx'
import {calendarTypesFetchIfNeeded} from 'actions/refTables/calendarTypes.jsx'

/**
  * PARTY DETAIL NAMES
  * *********************************************
  * Blok v detailu osoby se zobrazení a funkcemi pri správu jmen
  */ 
const PartyDetailNames = class PartyDetailNames extends AbstractReactComponent {
    constructor(props) {
        super(props);
        this.dispatch(calendarTypesFetchIfNeeded());    // seznam typů kalendářů (gregoriánský, juliánský, ...)
        this.bindMethods(
            'handleAddName',                            // kliknutí na tlačítko přidat jméno
            'handleUpdateName',                         // kliknutí na tlačítko upravit jméno
            'handleDeleteName',                         // kliknutí na tlačítko smazat jméno
            'addName',                                  // uložení nového jména
            'updateName',                               // aktualizace jména
            'deleteName',                               // smazání jména
        );
    }

    /**
     * HANDLE DELETE NAME
     * *********************************************
     * Kliknutí na tlačítko smazání jména
     * @param nameId - identifikátor jména osoby
     * @param event - událost kliknutí
     */     
    handleDeleteName(nameId, event){
        if(confirm(i18n('party.detail.name.delete'))){              // pokud uživatel potvrdí smazání
            this.deleteName(nameId);                                // jméno se smaže
        }
    }

    handleSelectPrefferedName(nameId, event){
        if(confirm(i18n('party.detail.name.prefferedName'))){
            this.setPrefferedName(nameId);
        }
    }

    /**
     * DELETE NAME
     * *********************************************
     * Smazání jména
     * @param nameId - identifikátor jména osoby
     */ 
    deleteName(nameId){
        var party = this.props.partyRegion.selectedPartyData;       // aktuální osoba
        var names = []                                              // nový seznam jmen
        for(var i = 0; i<party.partyNames.length; i++){             // projdeme původní jména
            if(party.partyNames[i].partyNameId != nameId){          // a pokud se nejedna o mazané jméno
                names[names.length] = party.partyNames[i];          // přidáme ho do seznamu jmen, co budou zachována
            }
        }
        party.partyNames = names;                                   // vyměníme starý seznam jmen za nový
        this.dispatch(updateParty(party));                          // změny uložíme
    }

    /**
     * Nastaví preferované jméno osoby.
     * @param nameId id preferovaného jména
     */
    setPrefferedName(nameId) {
        var party = this.props.partyRegion.selectedPartyData;

        party.partyNames.forEach(p => {
            p.prefferedName = p.partyNameId == nameId;
        });

        this.dispatch(updateParty(party));
    }

    /**
     * ADD NAME
     * *********************************************
     * Vložení nového jména
     * @param data object - data jména z formuláře
     */ 
    addName(data) {
        var party = this.props.partyRegion.selectedPartyData;       // aktuálně upravovaná osoba
        party.partyNames[party.partyNames.length] = {               // nové jméno vložíme na mkonec seznamu jmen
            nameFormType: {
                nameFormTypeId:data.nameFormTypeId,
            },
            displayName: data.mainPart,
            mainPart: data.mainPart,
            otherPart: data.otherPart,
            degreeBefore: data.degreeBefore,
            degreeAfter: data.degreeAfter,
            validFrom: data.validFrom,
            validTo: data.validTo,
            partyNameComplements: data.complements
        }
        this.dispatch(updateParty(party));                          // jméno se uloží a osoba znovu načte     
    }

    /**
     * HANDLE ADD NAME
     * *********************************************
     * Kliknutí na vložení nového jména
     */ 
    handleAddName(){
        var party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
        var data = {                                                                // výchozí data formuláře
            partyTypeId: party.partyType.partyTypeId,                               // identifikátor typu osoby, jejíž jménu upravujeme
            partyTypeCode: party.partyType.code,
            validFrom: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId            
            },
            validTo: {
                textDate: "",    
                calendarTypeId: this.props.partyRegion.gregorianCalendarId            
            },
            complements:[]
        }
        this.dispatch(modalDialogShow(this, i18n('party.detail.name.new') , <PartyNameForm initData={data} onSave={this.addName} />));    // otevře se formuláš nového jména   
    }

    /**
     * HANDLE UPDATE RELATION
     * *********************************************
     * Kliknutí na ikonu editace vztahu
     * @param nameId - identifikátor jména osoby
     */ 
    handleUpdateName(nameId){
        var party = this.props.partyRegion.selectedPartyData;                       // načtení aktualní osoby ze store
            var name = {};                                                          // pripravený onbjekt pro jméno
        for(var i = 0; i<party.partyNames.length; i++){                             // prohledáme všechna jména
            if(party.partyNames[i].partyNameId == nameId){                          // a to které hledáme
                name = party.partyNames[i];                                         // si uložíme
            }
        };
        var complements = [];                                                       // seznam doplňků jména
        if(name.partyNameComplements){                                              // pokud má vztah nějakké doplňky
            for(var i = 0; i<name.partyNameComplements.length; i++){                // tak se projdou
                complements[complements.length]={                                   // a přidají so seznamu doplňků
                    complementTypeId: name.partyNameComplements[i].complementTypeId,// identifikátor typu doplňku jména
                    complement : name.partyNameComplements[i].complement,           // textový doplňek jména
                    partyNameComplementId : name.partyNameComplements[i].partyNameComplementId,       // identifikátor doplňku
                };
            };
        }
        var data = {                                                                // data, která udou poslána formuláři
            partyNameId: name.partyNameId,                                          // identifikátor upravovaného jména
            partyTypeId: party.partyType.partyTypeId,                               // identifikátor typu osoby, jejíž jménu upravujeme
            partyTypeCode: party.partyType.code,
            nameFormTypeId : name.nameFormType.nameFormTypeId,                      // identifikátor typu jména
            mainPart: name.mainPart,                                                // hlavní část jména
            otherPart: name.otherPart,                                              // doplňková část jména
            degreeBefore: name.degreeBefore,                                        // titul před jménem
            degreeAfter: name.degreeAfter,                                          // titul za jménem
            validFrom: {                                                            // datace od
                textDate: (name.validFrom != null ? name.validFrom.textDate : ""),    
                calendarTypeId: (name.validFrom != null ? name.validFrom.calendarTypeId : this.props.partyRegion.gregorianCalendarId)
            },
            validTo: {                                                              // datace do
                textDate: (name.validTo != null ? name.validTo.textDate : ""), 
                calendarTypeId: (name.validTo != null ? name.validTo.calendarTypeId : this.props.partyRegion.gregorianCalendarId)
            },
            complements: complements,
        }
        this.dispatch(modalDialogShow(this, name.mainPart , <PartyNameForm initData={data} onSave={this.updateName} />));
    }

    /**
     * UPDATE RELATION
     * *********************************************
     * Uložení změn ve jménu
     * @param data object - data vyplněná v formuláři
     */ 
    updateName(data){
        var party = this.props.partyRegion.selectedPartyData;                           // identifikátor osoby, které patří měněné jméno

        var complements = [];                                                           // nový (zatím prázdný) seznam doplnku jména
        for(let i = 0; i<data.complements.length; i++){                                 // projdeme data doplňků z formuláře
            complements[complements.length] = {                                         // a přidáme je do seznamu nových doplňků
                complement: data.complements[i].complement,                             // textová hodnota doplňky
                complementTypeId: data.complements[i].complementTypeId,                 // identifikátor typu doplňku
                partyNameComplementId: data.complements[i].partyNameComplementId        // identifikátor doplňku
            }
        }        

        var names = party.partyNames;                                                   // původní jména osoby
        for(let i = 0; i<names.length; i++){                                            // je potřeba ho najít mezi ostatními jmény
            if(names[i].partyNameId == data.partyNameId){                               // to je ono            
                party.partyNames[i].mainPart = data.mainPart;                           // hlavní část jména
                party.partyNames[i].otherPart = data.otherPart;                         // vedlejší část jména
                party.partyNames[i].degreeBefore = data.degreeBefore;
                party.partyNames[i].degreeAfter = data.degreeAfter;
                party.partyNames[i].validFrom = data.validFrom;                         // datace jména od
                party.partyNames[i].validTo = data.validTo;                             // datace jména do
                party.partyNames[i].partyNameComplements = complements;                 // seznamm entit ve vztahu
                party.partyNames[i].nameFormType.nameFormTypeId = data.nameFormTypeId;  // identifikátor typu jména
            }
            if(
                !party.partyNames[i].validFrom ||
                party.partyNames[i].validFrom.textDate == "" || 
                party.partyNames[i].validFrom.textDate == null || 
                party.partyNames[i].validFrom.textDate == undefined
            ){  
                party.partyNames[i].validFrom = null;                                            // pokud není zadaný textová část data, celý datum se ruší
            }
            if(
                !party.partyNames[i].validTo || 
                party.partyNames[i].validTo.textDate == "" || 
                party.partyNames[i].validTo.textDate == null || 
                party.partyNames[i].validTo.textDate == undefined
            ){  
                party.partyNames[i].validTo = null;                                              // pokud není zadaný textová část data, celý datum se ruší
            }
        }
        this.dispatch(updateParty(party));                                              // uložení změn a znovu načtení dat osoby              
    }

    /**
     * RENDER
     * *********************************************
     * Vykreslení bloku jmen
     */ 
    render() {
        const {canEdit, partyRegion:{selectedPartyData}} = this.props;

        const party = selectedPartyData;

        return  <div className="partyNames">
            <table>
                <tbody>
                    {party.partyNames.map(i => {
                        let cls = "name column";
                        if(i.prefferedName) {
                            cls += " text-bold";
                        }

                        const allowedDeleteOrSelect = !i.prefferedName && canEdit;
                        return <tr key={'partyName' + i.partyNameId} className="name">
                        <td className={cls}>{i.displayName}</td>
                            <td className="buttons">
                                {canEdit && <Button className="column" onClick={this.handleUpdateName.bind(this, i.partyNameId)}><Icon glyph="fa-pencil"/></Button>}
                                {allowedDeleteOrSelect && <Button className="column" onClick={this.handleDeleteName.bind(this, i.partyNameId)}><Icon glyph="fa-trash"/></Button>}
                                {allowedDeleteOrSelect && <Button className="column" onClick={this.handleSelectPrefferedName.bind(this, i.partyNameId)}><Icon glyph="fa-check"/></Button>}
                            </td>
                        <td className="description">{(i.preferred ? i18n('party.detail.name.preferred') : "" )}</td>
                    </tr>})}
                </tbody>
            </table>
            {canEdit && <Button className="column" onClick={this.handleAddName}><Icon glyph="fa-plus"/> { i18n('party.detail.name.new')}</Button>}
        </div>
    }
}

module.exports = connect()(PartyDetailNames);
