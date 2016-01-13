/**
 * Akce pro strom AP, verzí a případně fondů.
 */

import {WebApi} from 'actions'
import * as types from 'actions/constants/actionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function faFileTreeFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.faFileTree.fetched && !state.faFileTree.isFetching) {
            return dispatch(faFileTreeFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function faFileTreeFetch() {
    return dispatch => {
        dispatch(faFileTreeRequest())
        return WebApi.getFaFileTree()
            .then(json => dispatch(faFileTreeReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function faFileTreeReceive(json) {
    return {
        type: types.FA_FA_FILE_TREE_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function faFileTreeRequest() {
    return {
        type: types.FA_FA_FILE_TREE_REQUEST
    }
}
