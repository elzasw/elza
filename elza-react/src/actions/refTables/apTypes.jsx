/**
 * Akce pro seznam typu pristupovych bodu - apTypes.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from "../constants/ActionTypes";

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refApTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        const state = getState();
        if ((!state.refTables.apTypes.fetched || state.refTables.apTypes.dirty) && !state.refTables.apTypes.isFetching) {
            return dispatch(refApTypesFetch());
        }
    };
}

/**
 * Nové načtení dat.
 */
export function refApTypesFetch() {
    return dispatch => {
        dispatch(refApTypesRequest());
        return WebApi.getApTypes().then(json => {
            dispatch(refApTypesReceive(json));
        });
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refApTypesReceive(json) {
    return {
        type: types.REF_AP_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refApTypesRequest() {
    return {
        type: types.REF_AP_TYPES_REQUEST,
    };
}
