/**
 * Načtení seznamu všech osob
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refPartyListFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.partyList.fetched || state.refTables.partyList.dirty) && !state.refTables.partyList.isFetching) {
            return dispatch(refPartyListFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function refPartyListFetch() {
    return dispatch => {
        dispatch(refPartyListRequest())
        return WebApi.findParty('')
            .then(json => dispatch(refPartyListReceive(json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refPartyListReceive(json) {
    return {
        type: types.REF_PARTY_LIST_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refPartyListRequest() {
    return {
        type: types.REF_PARTY_LIST_REQUEST
    }
}
