import {WebApi} from 'actions/index.jsx';
import * as types from 'actions/constants/ActionTypes.js';

export function fundNodesPolicyFetchIfNeeded(fundVersionId) {
    return (dispatch, getState) => {
        const state = getState();

        const funds = state.arrRegion.funds;
        let fund = null;
        funds.forEach(item => {
            if (item.versionId === fundVersionId) {
                fund = item;
            }
        });

        if (fund != null) {
            const fundNodesPolicy = fund.fundNodesPolicy;
            if ((!fundNodesPolicy.fetched || fundNodesPolicy.dirty) && !fundNodesPolicy.isFetching) {
                return dispatch(fundNodesPolicyFetch(fundVersionId));
            }
        }
    };
}

/**
 * Nové načtení dat.
 */
export function fundNodesPolicyFetch(fundVersionId) {
    return dispatch => {
        dispatch(fundNodesPolicyRequest(fundVersionId));
        return WebApi.getFundPolicy(fundVersionId).then(json => dispatch(fundNodesPolicyReceive(fundVersionId, json)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function fundNodesPolicyReceive(fundVersionId, json) {
    return {
        type: types.FUND_FUND_NODES_POLICY_RECEIVE,
        items: json,
        versionId: fundVersionId,
        receivedAt: Date.now(),
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function fundNodesPolicyRequest(fundVersionId) {
    return {
        type: types.FUND_FUND_NODES_POLICY_REQUEST,
        versionId: fundVersionId,
    };
}
