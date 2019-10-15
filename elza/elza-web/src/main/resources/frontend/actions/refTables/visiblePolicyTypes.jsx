/**
 * Akce pro číselníky typů visible policy.
 */

import {WebApi} from 'actions/index.jsx';

import * as types from 'actions/constants/ActionTypes.js';

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function visiblePolicyTypesFetchIfNeeded() {
    return (dispatch, getState) => {
        var state = getState();
        if ((!state.refTables.visiblePolicyTypes.fetched || state.refTables.visiblePolicyTypes.dirty) && !state.refTables.visiblePolicyTypes.isFetching) {
            return dispatch(visiblePolicyTypesFetch());
        }
    }
}

/**
 * Nové načtení dat.
 */
export function visiblePolicyTypesFetch() {
    return dispatch => {
        dispatch(visiblePolicyTypesRequest())
        return WebApi.getVisiblePolicyTypes()
            .then(json => {
                var data = {};
                json.forEach((item) => {
                    data[item.id] = {
                        code: item.code,
                        name: item.name,
                        ruleSetId: item.ruleSetId
                    }
                });
                dispatch(visiblePolicyTypesReceive(data))
            });
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function visiblePolicyTypesReceive(json) {
    return {
        type: types.REF_VISIBLE_POLICY_TYPES_RECEIVE,
        items: json,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function visiblePolicyTypesRequest() {
    return {
        type: types.REF_VISIBLE_POLICY_TYPES_REQUEST
    }
}
