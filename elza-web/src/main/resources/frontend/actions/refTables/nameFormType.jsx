/**
 * Akce pro seznam forem jmena osob - nameFormTypeId.
 */

import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refNameFormTypeFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.refTables.nameFormType.fetched && !state.refTables.nameFormType.isFetching) {
            return dispatch(refNameFormTypeFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function refNameFormTypeFetch() {
    return dispatch => {
        dispatch(refNameFormTypeRequest())
        return WebApi.getNameFormTypes()
            .then(json => dispatch(refNameFormTypeReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refNameFormTypeReceive(json) {
    return {
        type: types.REF_NAME_FORM_TYPE_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refNameFormTypeRequest() {
    return {
        type: types.REF_NAME_FORM_TYPE_REQUEST
    }
}
