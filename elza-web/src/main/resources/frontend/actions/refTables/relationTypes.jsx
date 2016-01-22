/**
 * Akce pro číselníky typů relací osob
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function relationTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.refTables.relationTypes.fetched && !state.refTables.relationTypes.isFetching) {
            return dispatch(relationTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function relationTypesFetch() {
    return dispatch => {
        dispatch(relationTypesRequest())
        return WebApi.getRelationTypes()
            .then(json => dispatch(relationTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function relationTypesReceive(json) {
    return {
        type: types.REF_RELATION_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function relationTypesRequest() {
    return {
        type: types.REF_RELATION_TYPES_REQUEST
    }
}
