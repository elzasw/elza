/**
 * Web api pro komunikaci se serverem.
 */

import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n, AddPartyForm} from 'components/index.jsx';
import {getPartyTypeById} from 'actions/refTables/partyTypes.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'

import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'

/**
 * Načtení seznamu osob dle filtru
 *
 * @param filter {Object} - objekt filtru
 * @param versionId int - versionId
 */
export function partyListFetchIfNeeded(filter, versionId = null) {
    return SimpleListActions.fetchIfNeeded(AREA_PARTY_LIST, versionId, () => WebApi.findParty(filter.text, versionId, filter.type))
}

/**
 * Filtr osob
 *
 * @param filter {Object} - objekt filtru
 */
export function partyListFilter(filter) {
    return SimpleListActions.filter(AREA_PARTY_LIST, filter);
}

/**
 * Invalidace seznamu osob
 */
export function partyListInvalidate() {
    return SimpleListActions.invalidate(AREA_PARTY_LIST, null);
}

export function partyDetailFetchIfNeeded(id) {
    return DetailActions.fetchIfNeeded(AREA_PARTY_DETAIL, id, () => WebApi.getParty(id));
}

export function partyDetailInvalidate() {
    return DetailActions.invalidate(AREA_PARTY_DETAIL, null)
}

export function partyDetailClear() {
    return partyDetailFetchIfNeeded(null);
}


export function partySettingsPinSave(settings) {
    return dispatch => {
        WebApi.setUserSettings(settings)
            .then(data => {
                dispatch(userDetailResponseSettings(data));
                dispatch(modalDialogHide());
            });
    }
}


export const AREA_PARTY_LIST = 'partyList';
export const AREA_PARTY_DETAIL = 'partyDetail';

/**
 * INSERT PARTY
 * *********************************************
 * Volání webového rozhraní pro vložení nové osoby
 * @param partyType string - typ osoby (ParPersonEditVO, ParDynastyEditVO, ...)
 * @param filterText string - aktualni filtr nad seznamem osob - aby se uzivateli vratil, doplneny pripadne o novou osobu
 * @param partyTypeId int - identifikátor typu osoby (1 - ParPersonEditVO, 2 - ParDynastyEditVO, ...)
 * @param nameFormTypeId int - identifikator typu jmena (uredni, svetske, za svobodna, ...)
 * @param nameMain string - hlavní jméno osoby
 * @param nameOther string - doplňující jméno osoby
 * @param validFrom string - OD - rozsah období, ve kterém je záznam platný/aktivní (1.1.2000 - 31.5.2003, 19. století, leden 1978, ...)
 * @param valiedTo string - DO - rozsah období, ve kterém je záznam platný/aktivní (1.1.2000 - 31.5.2003, 19. století, leden 1978, ...)
 * @param calendarTypeId int - identifikator kalendáře, ve kterém je rozsah období uveden (Gregoriánský, Juliánský)
 * @param degreeBefore string - titul před jménem osoby
 * @param degreeAfter string - titul za jménem osoby
 * @param scope string - !! cosi co netuším k čemu je, ale nejde bez toho uložit korporace - uklada se aktualne vždycky prazdny řetězec !!
 * @deprecated
 */
export function insertParty(partyType, filterText, partyTypeId, nameFormTypeId, nameMain, nameOther, validFrom, valiedTo, calendarTypeId, degreeBefore, degreeAfter, scope) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.insertParty(partyType))
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
 * @param party json - kompletni objekt osoby se všemi změnami
*/
export function updateParty(party){ 
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.updateParty(party))
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
 * @param partyId string - identifikátor osoby, kterou chceme smazat
 */
export function partyDelete(partyId) {
    return dispatch => {
        WebApi.deleteParty(partyId).then(() => {
            dispatch(partyDetailClear());
            dispatch(partyListInvalidate());
        })
    }
}

/**
 * INSERT RELATION
 * *********************************************
 * Volání webového rozhraní pro vložení nové relace
 * @param relation obj - objekt vztahu, který se má vytvořit
 * @param partyId int - identidikátor osoby, které se vztah zakládá
 */
export function insertRelation(relation, partyId) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.insertRelation(relation))
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
 * @param relation obj - změněný objekt vztahu
 * @param partyId int - identidikátor osoby, které vztah patří
 */
export function updateRelation(relation, partyId) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.updateRelation(relation))
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
 * @param relationId int - identifikator mazané relace
 * @param partyId int - identifikátor osoby, kterou chceme smazat
 */
export function deleteRelation(relationId, partyId) {
    return dispatch => {
        return WebApi.deleteRelation(relationId)
            .then((json) => {
                dispatch(partyDetailFetch(partyId));        // přenačtení detailu osoby
            });
    }
}


export function partyAdd(partyTypeId, versionId, callback, showSubmitTypes = false) {
    return (dispatch, getState) => {
        const state = getState();
        const partyTypeCode = getPartyTypeById(partyTypeId, state.refTables.partyTypes.items).code;

        let label;
        switch(partyTypeCode){                                        // podle typu osoby bude různý nadpis
            case "PERSON": label = i18n('party.addParty'); break;   // rod
            case "DYNASTY": label = i18n('party.addPartyDynasty'); break;     // korporace
            case "GROUP_PARTY": label = i18n('party.addPartyGroup'); break;     // událost
            case "EVENT": label = i18n('party.addPartyEvent'); break;     // událost
            default: label = i18n('party.addParty');
        }

        dispatch(modalDialogShow(this, label, <AddPartyForm partyTypeCode={partyTypeCode} partyTypeId={partyTypeId} showSubmitTypes={showSubmitTypes} versionId={versionId} onSubmitForm={partyAddSubmit.bind(null, callback, dispatch)} />));
    }
}

function partyAddSubmit(callback, dispatch, submitType, data) {
    let partyType = '';                                     // typ osoby - je potreba uvest i jako specialni klivcove slovo
    switch(data.partyTypeId){
        case 1: partyType = '.ParPersonVO'; break;          // typ osoby osoba
        case 2: partyType = '.ParDynastyVO'; break;         // typ osoby rod
        case 3: partyType = '.ParPartyGroupVO'; break;      // typ osoby korporace
        case 4: partyType = '.ParEventVO'; break;           // typ osoby docasna korporace - udalost
    }
    const party = {                                           // objekt osoby
        '@type': partyType,                                 // typ osoby - speciální klíčové slovo
        partyType: {                                        // typ osoby
            partyTypeId: data.partyTypeId                   // identikátor typu osoby
        },
        genealogy: data.mainPart,                           // název rodu pro soby typu rod
        scope: data.scopeId,                                          // cosi, co tu musí být
        record: {                                           // záznam patřící k ossobě
            '@class': "cz.tacr.elza.controller.vo.RegRecordVO",
            registerTypeId: data.recordTypeId,              // identifikátor typu záznamu
            scopeId: data.scopeId                           // identifikátor tridy rejstriku
        },
        from: data.from,                                    // datace od
        to: data.to,                                        // datace do
        partyNames : [{                                     // jména osoby
            nameFormType: {                                 // typ formy jména
                nameFormTypeId: parseInt(data.nameFormTypeId) // identifikátor typu jména osoby
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
    };
    if(party.from && (party.from.textDate == "" || party.from.textDate == null || party.from.textDate == undefined)){
        party.from = null;                                  // pokud není zadaný textová část data, celý datum se ruší
    }
    if(party.to && (party.to.textDate == "" || party.to.textDate == null || party.to.textDate == undefined)){
        party.to = null;                                    // pokud není zadaný textová část data, celý datum se ruší
    }

    savingApiWrapper(dispatch, WebApi.insertParty(party)).then((json) => {
        dispatch(modalDialogHide());
        callback && callback(json, submitType);
    });
}

export function partySelect(partyId, fund = null) {
    return {
        partyId: partyId,
        fund,
        type: types.PARTY_SELECT
    }
}

export function partyArrReset() {
    return {
        type: types.PARTY_ARR_RESET
    }

}