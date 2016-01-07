/**
 * Akce pro doplňující informace k záložce NODE.
 */

import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

function getNodeInfo(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node.nodeInfo;
    }

    return null;
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function faNodeInfoFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var nodeInfo = getNodeInfo(state, versionId, nodeKey);
        if (nodeInfo != null && !nodeInfo.fetched && !nodeInfo.isFetching) {
            return dispatch(faNodeInfoFetch(versionId, nodeId, nodeKey));
        }
    }
}

/**
 * Nové načtení dat.
 */
export function faNodeInfoFetch(versionId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(faNodeInfoRequest(versionId, nodeId, nodeKey))
        return WebApi.getFaNodeInfo(versionId, nodeId)
            .then(json => dispatch(faNodeInfoReceive(versionId, nodeId, nodeKey, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function faNodeInfoReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FA_NODE_INFO_RECEIVE,
        versionId,
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
export function faNodeInfoRequest(versionId, nodeId, nodeKey) {
    return {
        versionId,
        nodeId,
        nodeKey,
        type: types.FA_NODE_INFO_REQUEST
    }
}
