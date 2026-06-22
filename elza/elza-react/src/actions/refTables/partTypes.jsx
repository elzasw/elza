/**
 * Akce pro seznam partTypes.
 */

import {Api} from 'api/api';
import * as types from "../constants/ActionTypes";


/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refPartTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        const {
            refTables: {partTypes},
        } = getState();
        if ((!partTypes.fetched || partTypes.dirty) && !partTypes.isFetching) {
            return dispatch(refPartTypesFetch());
        }
    };
}

/**
 * Nové načtení dat.
 */
export function refPartTypesFetch() {
    return dispatch => {
        dispatch(refPartTypesRequest());
        return Api.rules.rulesListPartTypes().then(({data}) => dispatch(refPartTypesReceive(data)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refPartTypesReceive(json) {
    return {
        type: types.REF_PART_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refPartTypesRequest() {
    return {
        type: types.REF_PART_TYPES_REQUEST,
    };
}
