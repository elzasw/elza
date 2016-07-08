/**
 * Načtení seznamu všech rejstříků
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refRegistryListFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.registryRegionList.fetched || state.refTables.registryRegionList.dirty) && !state.refTables.registryRegionList.isFetching) {
            return dispatch(refRegistryListFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function refRegistryListFetch() {
    return dispatch => {
        dispatch(refRegistryListRequest())
        return WebApi.findRegistry('')
            .then(json => dispatch(refRegistryListReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refRegistryListReceive(json) {
    return {
        type: types.REF_REGISTRY_LIST_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refRegistryListRequest() {
    return {
        type: types.REF_REGISTRY_LIST_REQUEST
    }
}
