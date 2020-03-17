/**
 * Akce pro seznam institucí - ParInstitution.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refInstitutionsFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (
            (!state.refTables.institutions.fetched || state.refTables.institutions.dirty) &&
            !state.refTables.institutions.isFetching
        ) {
            return dispatch(refInstitutionsFetch());
        }
    };
}

/**
 * Nové načtení dat.
 */
export function refInstitutionsFetch() {
    return dispatch => {
        dispatch(refInstitutionsRequest());
        return WebApi.getInstitutions().then(json => dispatch(refInstitutionsReceive(json)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refInstitutionsReceive(json) {
    return {
        type: types.REF_INSTITUTIONS_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refInstitutionsRequest() {
    return {
        type: types.REF_INSTITUTIONS_REQUEST,
    };
}
