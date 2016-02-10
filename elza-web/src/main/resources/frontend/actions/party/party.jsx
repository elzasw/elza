/**
 * Web api pro komunikaci se serverem.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions'
import * as types from 'actions/constants/ActionTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog'
import {faSubNodeFormValueChangeParty, faSubNodeFormValueBlur} from 'actions/arr/subNodeForm'
import {routerNavigate} from 'actions/router'
import {i18n, AddPartyForm} from 'components';
import {getPartyTypeById} from 'actions/refTables/partyTypes';



/**
 * INSERT PARTY
 * *********************************************
 * Volání webového rozhraní pro vložení nové osoby
 * @param string partyType - typ osoby (ParPersonEditVO, ParDynastyEditVO, ...)
 * @param string filteredText - aktualni filtr nad seznamem osob - aby se uzivateli vratil, doplneny pripadne o novou osobu
 * @param int partyTypeId - identifikátor typu osoby (1 - ParPersonEditVO, 2 - ParDynastyEditVO, ...)
 * @param int nameFormTypeId - identifikator typu jmena (uredni, svetske, za svobodna, ...)  
 * @param string nameMain - hlavní jméno osoby
 * @param string nameOther - doplňující jméno osoby
 * @param string validRange - rozsah období, ve kterém je záznam platný/aktivní (1.1.2000 - 31.5.2003, 19. století, leden 1978, ...)
 * @param int calendarType - identifikator kalendáře, ve kterém je rozsah období uveden (Gregoriánský, Juliánský)
 * @param string degreeBefore - titul před jménem osoby
 * @param string degreeAfter - titul za jménem osoby
 * @param string scope - !! cosi co netuším k čemu je, ale nejde bez toho uložit korporace - uklada se aktualne vždycky prazdny řetězec !!
 */
export function insertParty(partyType, filterText, partyTypeId, nameFormTypeId, nameMain, nameOther, validFrom, valiedTo, calendarTypeId, degreeBefore, degreeAfter, scope) {
    return dispatch => {
        return WebApi.insertParty(partyType)
            .then((json) => { 
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(json.partyId));   // otevření detailu aktuálně vložené osoby
                dispatch(findPartyFetch(filterText));       // znovu načtení leveho panelu s vyfiltrovanými osobami (aby se tam pridala nová)

            });
    }
}

/**
 * UPDATE PARTY
 * *********************************************
 * Volání webového rozhraní pro upravu základních udaju ossob (inplace editace)
 * @param json object party - kompletni objekt osoby se všemi změnami
*/
export function updateParty(party){ 
    return dispatch => {
        return WebApi.updateParty(party)
            .then((json) => {
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(party.partyId));   // otevření detailu aktuálně vložené osoby
            });
    }
}

/**
 * DELETE PARTY
 * *********************************************
 * Volání webového rozhraní pro smazání osoby
 * @param string partyId - identifikátor osoby, kterou chceme smazat
 * @param string filteredText - aktualni filtr nad seznamem osob - aby se uzivateli vratil, už bez smazané osoby
 */
export function deleteParty(partyId, filterText) {
    return dispatch => {
        return WebApi.deleteParty(partyId)
            .then((json) => {
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(clearPartyDetail());
                dispatch(findPartyFetch(filterText));       // znovu načtení leveho panelu s vyfiltrovanými osobami (aby zmizela ta smazaná)
            });
    }
}


/**
 * FIND PARTY FETCH IF NEEDED
 * *********************************************
 * Volání webového rozhraní pro získání hledaný osob (pokud již není načten)
 * @param string filteredText - hledaná fráze/text/jméno
 */
export function findPartyFetchIfNeeded(filterText, versionId = null) {
    return (dispatch, getState) => {
        var state = getState();

        if (!state.partyRegion.isFetchingSearch && (state.partyRegion.dirty || state.partyRegion.filterText !== filterText)) {
            return dispatch(findPartyFetch(filterText, versionId));
        } else if (!state.partyRegion.fetchedSearch && !state.partyRegion.isFetchingSearch) {
            return dispatch(findPartyFetch(filterText, versionId));
        }
    }
}

/**
 * FIND PARTY FETCH
 * *********************************************
 * Volání webového rozhraní pro získání seznamu osob na základě hledané fráze
 * @param string filteredText - hledaná fráze/text/jméno
 */
export function findPartyFetch(filterText, versionId = null) {
    return dispatch => {
        dispatch(findPartyRequest(filterText))
        return WebApi.findParty(filterText, versionId)
            .then(json => dispatch(findPartyReceive(filterText, json)));
    }
}

/**
 * PARTY CLEAR SELECTED PARTY
 * *********************************************
 * Zruší zobrazeni detailu osoby - zobrazí defaultní stránku - pravděpodobně "nenalezeno"
 */
export function clearPartyDetail() {
    return {
        type: types.PARTY_DETAIL_CLEAR
    }
}

/**
 * FIND PARTY RECETVE
 * *********************************************
 * Seznam osob z webového rozhraní byl získán
 * @param string filteredText - hledaná fráze/text/jméno
 * @param obj json - objekt obsahující nalezené osoby
 */
export function findPartyReceive(filterText, json) {
    return {
        type: types.PARTY_FIND_PARTY_RECEIVE,
        items:json,
        filterText: filterText
    }
}

/**
 * FIND PARTY REQUEST
 * *********************************************
 * Byl odeslán požadavek na získání seznamu hledaných osob
 * @param string filteredText - hledaná fráze/text/jméno
 */
export function findPartyRequest(filterText) {
    return {
        type: types.PARTY_FIND_PARTY_REQUEST,
        filterText: filterText
    }
}

/**
 * PARTY DETAIL FETCH IF NEEDED
 * *********************************************
 * Volání webového rozhraní pro získání detailu osoby (pokud již není načten)
 * @param int selectedPartyID - identifikátor hledané osoby
 */
export function partyDetailFetchIfNeeded(selectedPartyID) {
    return (dispatch, getState) => {
        if(selectedPartyID == undefined){
            return;
        }

        var state = getState();
        if (!state.partyRegion.isFetchingDetail && (state.partyRegion.dirty || state.partyRegion.selectedPartyID !== selectedPartyID)) {
            return dispatch(partyDetailFetch(selectedPartyID));
        } else if (!state.partyRegion.fetchedDetail && !state.partyRegion.isFetchingDetail) {
            return dispatch(partyDetailFetch(selectedPartyID));
        }
    }
}

/**
 * PARTY DETAIL FETCH
 * *********************************************
 * Volání webového rozhraní pro získání detailu osoby
 * @param int selectedPartyID - identifikátor hledané osoby
 */
export function partyDetailFetch(selectedPartyID) {
    return dispatch => {
        dispatch(partyDetailRequest(selectedPartyID))
        
        return WebApi.getParty(selectedPartyID)
            .then(json => dispatch(partyDetailReceive(selectedPartyID, json)));
    }
}

/**
 * PARTY DETAIL RECIVE
 * *********************************************
 * Volání webového rozhraní pro získání detailu osoby
 * @param int selectedPartyID - identifikátor hledané osoby
 * @param obj selectedPartyData - získaná data osoby
 */
export function partyDetailReceive(selectedPartyID, selectedPartyData) {
    return {
        type: types.PARTY_DETAIL_RECEIVE,
        selectedPartyData: selectedPartyData,
        selectedPartyID: selectedPartyID
    }
}

/**
 * PARTY DETAIL REQUEST
 * *********************************************
 * Volání webového rozhraní pro získání detailu osoby
 * @param int selectedPartyID - identifikátor hledané osoby
 * @param obj selectedPartyData - získaná data osoby
 */
export function partyDetailRequest(selectedPartyID) {
    return {
        type: types.PARTY_DETAIL_REQUEST,
        selectedPartyID: selectedPartyID
    }
}


/**
 * INSERT RELATION
 * *********************************************
 * Volání webového rozhraní pro vložení nové relace
 * @param obj relation - objekt vztahu, který se má vytvořit
 * @param int partyId - identidikátor osoby, které se vztah zakládá
 */
export function insertRelation(relation, partyId) {
    return dispatch => {
        return WebApi.insertRelation(relation)
            .then((json) => { 
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(partyId));        // přenačtení detailu osoby
            });
    };
}

/**
 * UPDATE RELATION
 * *********************************************
 * Volání webového rozhraní pro uložení změny vztahu
 * @param obj relation - změněný objekt vztahu
 * @param int partyId - identidikátor osoby, které vztah patří
 */
export function updateRelation(relation, partyId) {
    return dispatch => {
        return WebApi.updateRelation(relation)
            .then((json) => { 
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(partyId));        // přenačtení detailu osoby
            });
    };
}


/**
 * DELETE RELATION
 * *********************************************
 * Volání webového rozhraní pro smazání relace
 * @param int relationID - identifikator mazané relace
 * @param int partyId - identifikátor osoby, kterou chceme smazat
 */
export function deleteRelation(relationId, partyId) {
    return dispatch => {
        return WebApi.deleteRelation(relationId)
            .then((json) => {
                dispatch(partyDetailFetch(partyId));        // přenačtení detailu osoby
            });
    }
}


export function partyAdd(partyTypeId, versionId, callback) {
    return (dispatch, getState) => {
        var state = getState();
        var partyTypeCode = getPartyTypeById(partyTypeId, state.refTables.partyTypes.items).code;

        var data = {                        // data předávaná do formuláře osoby
            partyTypeId: partyTypeId,       // identifikátor typu osoby (osoba, rod, událost, ..)
            partyTypeCode: partyTypeCode,
            from: {
                textDate: "",
                calendarTypeId: state.partyRegion.gregorianCalendarId
            },
            to: {
                textDate: "",
                calendarTypeId: state.partyRegion.gregorianCalendarId
            },
            complements: []
        }
        var label;
        switch(partyTypeCode){                                        // podle typu osoby bude různý nadpis
            case "PERSON": label = i18n('party.addParty'); break;   // rod
            case "DYNASTY": label = i18n('party.addPartyDynasty'); break;     // korporace
            case "GROUP_PARTY": label = i18n('party.addPartyGroup'); break;     // událost
            case "EVENT": label = i18n('party.addPartyEvent'); break;     // událost
            default: label = i18n('party.addParty');
        }

        dispatch(modalDialogShow(this, label, <AddPartyForm initData={data} versionId={versionId} onSave={partyAddSubmit.bind(null, callback, dispatch)} />));
    }
}

function partyAddSubmit(callback, dispatch, data) {

    var partyType = '';                                     // typ osoby - je potreba uvest i jako specialni klivcove slovo
    switch(data.partyTypeId){
        case 1: partyType = '.ParPersonVO'; break;          // typ osoby osoba
        case 2: partyType = '.ParDynastyVO'; break;         // typ osoby rod
        case 3: partyType = '.ParPartyGroupVO'; break;      // typ osoby korporace
        case 4: partyType = '.ParEventVO'; break;           // typ osoby docasna korporace - udalost
    }
    var party = {                                           // objekt osoby
        '@type': partyType,                                 // typ osoby - speciální klíčové slovo
        partyType: {                                        // typ osoby
            partyTypeId: data.partyTypeId                   // identikátor typu osoby
        },
        genealogy: data.mainPart,                           // název rodu pro soby typu rod
        scope: data.scopeId,                                          // cosi, co tu musí být
        record: {                                           // záznam patřící k ossobě
            registerTypeId: data.recordTypeId,              // identifikátor typu záznamu
            scopeId: data.scopeId                           // identifikátor tridy rejstriku
        },
        from: data.from,                                    // datace od
        to: data.to,                                        // datace do
        partyNames : [{                                     // jména osoby
            nameFormType: {                                 // typ formy jména
                nameFormTypeId: data.nameFormTypeId         // identifikátor typu jména osoby
            },
            displayName: data.mainPart,
            mainPart: data.mainPart,                        // hlavní část jména
            otherPart: data.otherPart,                      // vedlejší část jména
            degreeBefore: data.degreeBefore,                // titul před jménem
            degreeAfter: data.degreeAfter,                  // titul za jménem
            prefferedName: true,                            // hlavní jmno osoby
            from: data.from,                                // datace od
            to: data.to,                                    // datace do
            partyNameComplements: data.complements          // doplnky jména
        }]
    }
    if(party.from.textDate == "" || party.from.textDate == null || party.from.textDate == undefined){
        party.from = null;                                  // pokud není zadaný textová část data, celý fatum se ruší
    }
    if(party.to.textDate == "" || party.to.textDate == null || party.to.textDate == undefined){
        party.to = null;                                    // pokud není zadaný textová část data, celý fatum se ruší
    }

    WebApi.insertParty(party).then((json) => {
        dispatch(modalDialogHide());
        callback && callback(json);
    });
}

export function partySelect(partyId, fa = null) {
    return {
        partyId: partyId,
        fa: fa,
        type: types.PARTY_SELECT
    }
}

export function partyArrReset() {
    return {
        type: types.PARTY_ARR_RESET
    }

}