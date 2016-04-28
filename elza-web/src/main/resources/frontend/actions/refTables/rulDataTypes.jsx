/**
 * Akce pro číselníky typů atributů - RulDataType.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refRulDataTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.rulDataTypes.fetched || state.refTables.rulDataTypes.dirty) && !state.refTables.rulDataTypes.isFetching) {
            return dispatch(refRulDataTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function refRulDataTypesFetch() {
    return dispatch => {
        dispatch(refRulDataTypesRequest())
        return WebApi.getRulDataTypes()
            .then(json => dispatch(refRulDataTypesReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refRulDataTypesReceive(json) {
    return {
        type: types.REF_RUL_DATA_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refRulDataTypesRequest() {
    return {
        type: types.REF_RUL_DATA_TYPES_REQUEST
    }
}
