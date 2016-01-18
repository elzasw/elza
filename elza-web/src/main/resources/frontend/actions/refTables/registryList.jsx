/**
 * Akce pro seznam forem jmena osob - partyTypes.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refFetchRegistryIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.refTables.partyTypes.fetched && !state.refTables.partyTypes.isFetching) {
            return dispatch(refPartyTypesFetch());
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
