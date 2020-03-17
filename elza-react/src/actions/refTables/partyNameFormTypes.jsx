/**
 * Akce pro seznam forem jmena osob - partyNameFormTypes.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refPartyNameFormTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (
            (!state.refTables.partyNameFormTypes.fetched || state.refTables.partyNameFormTypes.dirty) &&
            !state.refTables.partyNameFormTypes.isFetching
        ) {
            return dispatch(refPartyNameFormTypesFetch());
        }
    };
}

/**
 * Nové načtení dat.
 */
export function refPartyNameFormTypesFetch() {
    return dispatch => {
        dispatch(refPartyNameFormTypesRequest());
        return WebApi.getPartyNameFormTypes().then(json => dispatch(refPartyNameFormTypesReceive(json)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refPartyNameFormTypesReceive(json) {
    return {
        type: types.REF_PARTY_NAME_FORM_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refPartyNameFormTypesRequest() {
    return {
        type: types.REF_PARTY_NAME_FORM_TYPES_REQUEST,
    };
}
