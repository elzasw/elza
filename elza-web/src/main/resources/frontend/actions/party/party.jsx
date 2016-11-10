import React from 'react';
import ReactDOM from 'react-dom';
import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes'
import {modalDialogShow, modalDialogHide} from 'actions/global/modalDialog.jsx'
import {i18n, AddPartyForm} from 'components/index.jsx';
import {getPartyTypeById} from 'actions/refTables/partyTypes.jsx';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {addToastrWarning} from 'components/shared/toastr/ToastrActions.jsx'
import {objectById} from 'stores/app/utils.jsx'

import {SimpleListActions} from 'shared/list'
import {DetailActions} from 'shared/detail'

export const AREA_PARTY_LIST = 'partyList';
export const AREA_PARTY_DETAIL = 'partyDetail';

export const PARTY_TYPE_CODES = {
    GROUP_PARTY: 'GROUP_PARTY',
    PERSON: 'PERSON',
    DYNASTY: 'DYNASTY',
    EVENT: 'EVENT',
};

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

export function partyDetail() {
    return DetailActions.invalidate(AREA_PARTY_DETAIL, null)
}

export function partyDetailClear() {
    return partyDetailFetchIfNeeded(null);
}

export function partyUpdate(party) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.updateParty(party)).then((json) => {
            dispatch(partyDetailInvalidate());
        });
    }
}


export function partyCreate(party) {
    return dispatch => {
        return savingApiWrapper(dispatch, WebApi.createParty(party))
            .then(newParty => {
                dispatch(modalDialogHide());
                dispatch(partyDetailFetchIfNeeded(newParty.id));
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
        const {refTables: {partyTypes}} = getState();
        const partyType = objectById(partyTypes.items, partyTypeId);

        let label;
        switch (partyType.code) {                                        // podle typu osoby bude různý nadpis
            case PARTY_TYPE_CODES.PERSON: label = i18n('party.addParty'); break;
            case PARTY_TYPE_CODES.DYNASTY: label = i18n('party.addPartyDynasty'); break;
            case PARTY_TYPE_CODES.GROUP_PARTY: label = i18n('party.addPartyGroup'); break;
            case PARTY_TYPE_CODES.EVENT: label = i18n('party.addPartyEvent'); break;
            default: label = i18n('party.addParty');
        }

        dispatch(modalDialogShow(this, label, <AddPartyForm partyType={partyType} showSubmitTypes={showSubmitTypes} versionId={versionId} onSubmitForm={partyAddSubmit.bind(null, callback, dispatch)} />));
    }
}

function partyAddSubmit(callback, dispatch, submitType, data) {
    let classType = '';                                     // typ osoby - je potreba uvest i jako specialni klivcove slovo
    switch (data.partyType.code) {
        case PARTY_TYPE_CODES.PERSON: classType = '.ParPersonVO'; break;          // typ osoby osoba
        case PARTY_TYPE_CODES.DYNASTY: classType = '.ParDynastyVO'; break;         // typ osoby rod
        case PARTY_TYPE_CODES.GROUP_PARTY: classType = '.ParPartyGroupVO'; break;      // typ osoby korporace
        case PARTY_TYPE_CODES.EVENT: classType = '.ParEventVO'; break;           // typ osoby docasna korporace - udalost
    }
    const {prefferedName, ...other} = data;
    const party = {
        '@type': classType,
        ...other,
        record: {
            '@class': "cz.tacr.elza.controller.vo.RegRecordVO",
            ...other.record
        },
        partyNames : [
            {
                ...prefferedName,
                prefferedName: true,
            }
        ]
    };

    savingApiWrapper(dispatch, WebApi.createParty(party)).then((json) => {
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