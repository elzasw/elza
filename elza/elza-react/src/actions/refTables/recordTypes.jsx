/**
 * Akce pro seznam typu záznamů - recordTypes.
 */

import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refRecordTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (
            (!state.refTables.recordTypes.fetched || state.refTables.recordTypes.dirty) &&
            !state.refTables.recordTypes.isFetching
        ) {
            return dispatch(refRecordTypesFetch());
        }
    };
}

/**
 * Nové načtení dat.
 */
export function refRecordTypesFetch() {
    return dispatch => {
        dispatch(refRecordTypesRequest());
        return WebApi.getRecordTypes().then(json => {
            dispatch(refRecordTypesReceive(json));
        });
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refRecordTypesReceive(json) {
    return {
        type: types.REF_RECORD_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refRecordTypesRequest() {
    return {
        type: types.REF_RECORD_TYPES_REQUEST,
    };
}
