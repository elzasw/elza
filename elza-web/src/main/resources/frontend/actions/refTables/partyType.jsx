/**
 * Akce pro seznam typu osob.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function partyTypeFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.refTables.partyType.fetched && !state.refTables.partyType.isFetching) {
            return dispatch(refPartyTypeFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function refPartyTypeFetch() {
    return dispatch => {
        dispatch(refPartyTypeRequest())
        return WebApi.getPartyType()
            .then(json => dispatch(refPartyTypeReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refPartyTypeReceive(json) {
    return {
        type: types.REF_PARTY_TYPE_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refPartyTypeRequest() {
    return {
        type: types.REF_PARTY_TYPE_REQUEST
    }
}
