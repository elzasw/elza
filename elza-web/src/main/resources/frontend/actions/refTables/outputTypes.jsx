/**
 * Akce pro číselníky typů obalů.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function outputTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.outputTypes.fetched || state.refTables.outputTypes.dirty) && !state.refTables.outputTypes.isFetching) {
            return dispatch(outputTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function outputTypesFetch() {
    return dispatch => {
        dispatch(outputTypesRequest())
        return WebApi.getOutputTypes()
            .then(json => dispatch(outputTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function outputTypesReceive(json) {
    return {
        type: types.REF_OUTPUT_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function outputTypesRequest() {
    return {
        type: types.REF_OUTPUT_TYPES_REQUEST
    }
}
