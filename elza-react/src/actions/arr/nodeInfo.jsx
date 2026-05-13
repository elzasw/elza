/**
 * Akce pro doplňující informace k záložce NODE.
 */

import * as types from 'actions/constants/ActionTypes';

export function isNodeInfoAction(action) {
    switch (action.type) {
        case types.FUND_NODE_INFO_REQUEST:
        case types.FUND_NODE_INFO_RECEIVE:
        case types.FUND_NODE_INFO_INVALIDATE:
            return true;
        default:
            return false;
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function fundNodeInfoReceive(versionId, nodeId, routingKey, json, viewStartIndexInvalidate = false) {
    return {
        type: types.FUND_NODE_INFO_RECEIVE,
        versionId,
        nodeId,
        routingKey,
        childNodes: json.childNodes,
        nodeIndex: json.nodeIndex,
        nodeCount: json.nodeCount,
        parentNodes: json.parentNodes,
        receivedAt: Date.now(),
        viewStartIndexInvalidate,
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function fundNodeInfoRequest(versionId, nodeId, routingKey) {
    return {
        versionId,
        nodeId,
        routingKey,
        type: types.FUND_NODE_INFO_REQUEST,
    };
}

/**
 * Marks node info as invalid to force new data request.
 */
export function fundNodeInfoInvalidate(versionId, nodeId, routingKey) {
    return {
        versionId,
        nodeId,
        routingKey,
        type: types.FUND_NODE_INFO_INVALIDATE,
    };
}
