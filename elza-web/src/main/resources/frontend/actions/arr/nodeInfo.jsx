/**
 * Akce pro doplňující informace k záložce NODE.
 */

import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

function getNodeInfo(state, faId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, faId, nodeKey);
    if (r != null) {
        return r.node.nodeInfo;
    }

    return null;
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function faNodeInfoFetchIfNeeded(faId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var nodeInfo = getNodeInfo(state, faId, nodeKey);
        if (nodeInfo != null && !nodeInfo.fetched && !nodeInfo.isFetching) {
            return dispatch(faNodeInfoFetch(faId, nodeId, nodeKey));
        }
    }
}

/**
 * Nové načtení dat.
 */
export function faNodeInfoFetch(faId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(faNodeInfoRequest(faId, nodeId, nodeKey))
        return WebApi.getFaNodeInfo(faId, nodeId)
            .then(json => dispatch(faNodeInfoReceive(faId, nodeId, nodeKey, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function faNodeInfoReceive(faId, nodeId, nodeKey, json) {
    return {
        type: types.FA_NODE_INFO_RECEIVE,
        faId,
        nodeId,
        nodeKey,
        childNodes: json.childNodes,
        parentNodes: json.parentNodes,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function faNodeInfoRequest(faId, nodeId, nodeKey) {
    return {
        faId,
        nodeId,
        nodeKey,
        type: types.FA_NODE_INFO_REQUEST
    }
}
