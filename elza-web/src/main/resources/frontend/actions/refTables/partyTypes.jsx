/**
 * Akce pro seznam forem jmena osob - partyTypes.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refPartyTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.partyTypes.fetched || state.refTables.partyTypes.dirty) && !state.refTables.partyTypes.isFetching) {
            return dispatch(refPartyTypesFetch());
        }
    }
}

/**
 * Projde seznam typů osob a najde typ s daným id.
 * @param partyTypeId id typu
 * @param partyTypes seznam typů osob
 * @returns typ osoby
 */
export function getPartyTypeById(partyTypeId, partyTypes){
    var index;
    for(index = 0; index < partyTypes.length; ++index){
        if(partyTypes[index].partyTypeId === partyTypeId){
            return partyTypes[index];
        }
    }
}

/**
 * Nové načtení dat.
 */
export function refPartyTypesFetch() {
    return dispatch => {
        dispatch(refPartyTypesRequest())
        return WebApi.getPartyTypes()
            .then(json => dispatch(refPartyTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refPartyTypesReceive(json) {
    return {
        type: types.REF_PARTY_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refPartyTypesRequest() {
    return {
        type: types.REF_PARTY_TYPES_REQUEST
    }
}
