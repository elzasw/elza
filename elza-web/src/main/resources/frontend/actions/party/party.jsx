/**
 * Web api pro komunikaci se serverem.
 */

import {WebApi} from 'actions'
import * as types from 'actions/constants/actionTypes'
import {modalDialogHide} from 'actions/global/modalDialog'


/**
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
export function insertParty(partyType, filterText, partyTypeId, nameFormTypeId, nameMain, nameOther, validRange, calendarType, degreeBefore, degreeAfter, scope) {
    return dispatch => {
        return WebApi.insertParty(partyType, partyTypeId, nameFormTypeId, nameMain, nameOther, degreeBefore, degreeAfter, validRange, calendarType, scope)
            .then((json) => { 
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(findPartyFetch(filterText));       // znovu načtení leveho panelu s vyfiltrovanými osobami (aby se tam pridala nová)
                dispatch(partyDetailFetch(json.partyId));   // otevření detailu aktuálně vložené osoby
            });
    }
}

/**
 * Volání webového rozhraní pro vložení nové relace (vztahu)
 * @param int partyId - identifikator osoby, ktere se pridava vztah
 * @param string note - poznámka ke vztahu
 * @param string sources - zdroje informaci
 * @param date from - datum počátku vztahu
 * @param date to - datum konce vztahu    
 */
export function insertRelation(partyId, relationTypeId, note, source, from, to, entities) {
    return dispatch => {
        return WebApi.insertRelation(partyId, relationTypeId, note, source, from, to, entities)
            .then((json) => { 
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(json.partyId));   // otevření detailu aktuálně vložené osoby
            });
    }
}


/**
 * Volání webového rozhraní pro smazání osoby
 * @param string partyId - identifikátor osoby, kterou chceme smazat
 * @param string filteredText - aktualni filtr nad seznamem osob - aby se uzivateli vratil, už bez smazané osoby
 */
export function deleteParty(partyId, filterText) {
    return dispatch => {
        return WebApi.deleteParty(partyId)
            .then((json) => {
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(findPartyFetch(filterText));       // znovu načtení leveho panelu s vyfiltrovanými osobami (aby zmizela ta smazaná)
                dispatch(partyDeleted());                   // zavolání funkce co hlavně odstraní ze storu detail smazané osoby, tj dojde k přegenerování na výchozí detail (nejspíš osoba nenalezena)
            });
    }
}


/**
 * Volání webového rozhraní pro vložení nového jména osobě
 * @param int partyId - identifikator osoby, ktere se pridava jméno
 * @param int nameFormTypeId - identifikator typu jmena (uredni, svetske, za svobodna, ...)  
 * @param string nameMain - hlavní jméno osoby
 * @param string nameOther - doplňující jméno osoby
 * @param string validFrom - rozsah období, ve kterém je záznam platný/aktivní (1.1.2000 - 31.5.2003, 19. století, leden 1978, ...)
 * @param string validTo - rozsah období, ve kterém je záznam platný/aktivní (1.1.2000 - 31.5.2003, 19. století, leden 1978, ...)
 * @param int calendarType - identifikator kalendáře, ve kterém je rozsah období uveden (Gregoriánský, Juliánský)
 * @param string degreeBefore - titul před jménem osoby
 * @param string degreeAfter - titul za jménem osoby
 */
export function insertName(partyId, nameFormTypeId, nameMain, nameOther, validRange, calendarType, degreeBefore, degreeAfter){
    return dispatch => {
        return WebApi.insertRelation(partyId, relationTypeId, note, source, from, to, entities)
            .then((json) => { 
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(json.partyId));   // otevření detailu aktuálně vložené osoby
            });
    }
}

/**
 * Volání webového rozhraní pro vložení nového jména osobě
 * @param int partyId - identifikator osoby, ktere se pridava jméno
 * @param int nameFormTypeId - identifikator typu jmena (uredni, svetske, za svobodna, ...)  
 * @param string nameMain - hlavní jméno osoby
 * @param string nameOther - doplňující jméno osoby
 * @param string validFrom - rozsah období, ve kterém je záznam platný/aktivní (1.1.2000 - 31.5.2003, 19. století, leden 1978, ...)
 * @param string validTo - rozsah období, ve kterém je záznam platný/aktivní (1.1.2000 - 31.5.2003, 19. století, leden 1978, ...)
 * @param int calendarType - identifikator kalendáře, ve kterém je rozsah období uveden (Gregoriánský, Juliánský)
 * @param string degreeBefore - titul před jménem osoby
 * @param string degreeAfter - titul za jménem osoby
 */
export function insertName(partyId, nameFormTypeId, nameMain, nameOther, validRange, calendarType, degreeBefore, degreeAfter){
    return dispatch => {
        return WebApi.insertRelation(partyId, relationTypeId, note, source, from, to, entities)
            .then((json) => { 
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(json.partyId));   // otevření detailu aktuálně vložené osoby
            });
    }
}

/**
 * Volání webového rozhraní pro upraveni osoby
 * @param object party
 */
export function updateParty(party) {
    return dispatch => {
        return WebApi.updateParty(party)
            .then((json) => {
                dispatch(modalDialogHide());                // zavření aktualně otevřeného dialogu
                dispatch(partyDetailFetch(partyId));        // otevření detailu aktuálně vložené osoby
            });
    }
}

/**
 * Volání webového rozhraní pro získání hledaný osob (pokud již není načten)
 * @param string filteredText - hledaná fráze/text/jméno
 */
export function findPartyFetchIfNeeded(filterText) {
    return (dispatch, getState) => {
        var state = getState();
        console.log(state.partyRegion);
        if (state.partyRegion.filterText !== filterText) {
            return dispatch(findPartyFetch(filterText));
        } else if (!state.partyRegion.fetchedSearch && !state.partyRegion.isFetchingSearh) {
            return dispatch(findPartyFetch(filterText));
        }
    }
}

/**
 * Volání webového rozhraní pro získání hledaný osob 
 * @param string filteredText - hledaná fráze/text/jméno
 */
export function findPartyFetch(filterText) {
    return dispatch => {
        dispatch(findPartyRequest(filterText))
        return WebApi.findParty(filterText)
            .then(json => dispatch(findPartyReceive(filterText, json)));
    }
}

export function findPartyReceive(filterText, json) {
    return {
        type: types.PARTY_FIND_PARTY_RECEIVE,
        items:json,
        filterText: filterText
    }
}

export function findPartyRequest(filterText) {
    return {
        type: types.PARTY_FIND_PARTY_REQUEST,
        filterText: filterText
    }
}

export function partyDetailFetchIfNeeded(selectedPartyID) {
    return (dispatch, getState) => {
        var state = getState();
        if (state.partyRegion.selectedPartyID !== selectedPartyID) {
            return dispatch(partyDetailFetch(selectedPartyID));
        } else if (!state.partyRegion.fetchedDetail && !state.partyRegion.isFetchingDetail) {
            return dispatch(partyDetailFetch(selectedPartyID));
        }
    }
}

export function partyDetailFetch(selectedPartyID) {
    return dispatch => {
        dispatch(partyDetailRequest(selectedPartyID))
        return WebApi.getParty(selectedPartyID)
            .then(json => dispatch(partyDetailReceive(selectedPartyID, json)));
    }
}

export function partyDetailReceive(selectedPartyID, selectedPartyData) {
    return {
        type: types.PARTY_DETAIL_RECEIVE,
        selectedPartyData: selectedPartyData,
        selectedPartyID: selectedPartyID
    }
}

export function partyDetailRequest(selectedPartyID) {
    return {
        type: types.PARTY_DETAIL_REQUEST,
        selectedPartyID: selectedPartyID
    }
}

export function partyDeleted() {
    return {
        type: types.PARTY_DELETED
    }
}


