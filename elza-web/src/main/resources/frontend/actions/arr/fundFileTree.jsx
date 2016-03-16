/**
 * Akce pro strom AP, verzí a případně fondů.
 */

import {WebApi} from 'actions'
import * as types from 'actions/constants/ActionTypes';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function fundFileTreeFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (!state.fundFileTree.fetched && !state.fundFileTree.isFetching) {
            return dispatch(fundFileTreeFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function fundFileTreeFetch() {
    return dispatch => {
        dispatch(fundFileTreeRequest())
        return WebApi.getFundFileTree()
            .then(json => dispatch(fundFileTreeReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function fundFileTreeReceive(json) {
    return {
        type: types.FUND_FUND_FILE_TREE_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function fundFileTreeRequest() {
    return {
        type: types.FUND_FUND_FILE_TREE_REQUEST
    }
}
