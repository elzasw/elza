/**
 * Akce pro doplňující informace pro aktuálně vybraný formulář node.
 */

import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/ActionTypes';

export function isSubNodeInfoAction(action) {
    switch (action.type) {
        case types.FUND_SUB_NODE_INFO_REQUEST:
        case types.FUND_SUB_NODE_INFO_RECEIVE:
            return true
        default:
            return false
    }
}

/**
 * Načtení subNodeInfo store pro předaná data.
 * @param {Object} state globální store
 * @param {int} versionId verze AS
 * @param {int} nodeKey klíč záložky
 * @return subNodeInfo store
 */
function getSubNodeInfoStore(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node.subNodeInfo;
    }

    return null;
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 */
export function fundSubNodeInfoFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeInfo = getSubNodeInfoStore(state, versionId, nodeKey);

        if (subNodeInfo != null) {
            if (!subNodeInfo.fetched && !subNodeInfo.isFetching) {
                return dispatch(fundSubNodeInfoFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

/**
 * Nové načtení dat.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 */
export function fundSubNodeInfoFetch(versionId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(fundSubNodeInfoRequest(versionId, nodeId, nodeKey))
        return WebApi.getFundTree(versionId, nodeId)
            .then(json => dispatch(fundSubNodeInfoReceive(versionId, nodeId, nodeKey, json)))
    };
}

/**
 * Nová data byla načtena.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 * @param {Object} json objekt s daty
 */
export function fundSubNodeInfoReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FUND_SUB_NODE_INFO_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        childNodes: json.nodes,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} nodeKey klíč záložky
 */
export function fundSubNodeInfoRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FUND_SUB_NODE_INFO_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}
