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
        const {
            refTables: {partyTypes},
        } = getState();
        if ((!partyTypes.fetched || partyTypes.dirty) && !partyTypes.isFetching) {
            return dispatch(refPartyTypesFetch());
        }
    };
}

/**
 * Nové načtení dat.
 */
export function refPartyTypesFetch() {
    return dispatch => {
        dispatch(refPartyTypesRequest());
        return WebApi.getPartyTypes().then(json => dispatch(refPartyTypesReceive(json)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refPartyTypesReceive(json) {
    return {
        type: types.REF_PARTY_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refPartyTypesRequest() {
    return {
        type: types.REF_PARTY_TYPES_REQUEST,
    };
}
