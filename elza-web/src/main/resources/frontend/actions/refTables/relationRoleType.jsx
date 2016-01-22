/**
 * Akce pro číselníky typů rolí relací osob
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function relationRoleTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.refTables.relationRoleTypes.fetched && !state.refTables.relationRoleTypes.isFetching) {
            return dispatch(relationRoleTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function relationRoleTypesFetch() {
    return dispatch => {
        dispatch(relationRoleTypesRequest())
        return WebApi.getRelationRoleTypes()
            .then(json => dispatch(relationRoleTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function relationRoleTypesReceive(json) {
    return {
        type: types.REF_RELATION_ROLE_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function relationRoleTypesRequest() {
    return {
        type: types.REF_RELATION_ROLE_TYPES_REQUEST
    }
}
