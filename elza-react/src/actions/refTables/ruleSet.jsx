/**
 * Akce pro seznam sad pravidel - RulRuleSet.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function refRuleSetFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if (
            (!state.refTables.ruleSet.fetched || state.refTables.ruleSet.dirty) &&
            !state.refTables.ruleSet.isFetching
        ) {
            return dispatch(refRuleSetFetch());
        }
    };
}

/**
 * Nové načtení dat.
 */
export function refRuleSetFetch() {
    return dispatch => {
        dispatch(refRuleSetRequest());
        return WebApi.getRuleSets().then(json => dispatch(refRuleSetReceive(json)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function refRuleSetReceive(json) {
    return {
        type: types.REF_RULE_SET_RECEIVE,
        items: json,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function refRuleSetRequest() {
    return {
        type: types.REF_RULE_SET_REQUEST,
    };
}
