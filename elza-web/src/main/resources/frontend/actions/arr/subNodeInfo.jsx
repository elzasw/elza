import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

function getSubNodeInfo(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node.subNodeInfo;
    }

    return null;
}
export function faSubNodeInfoFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeInfo = getSubNodeInfo(state, versionId, nodeKey);

        if (subNodeInfo != null) {
            if (!subNodeInfo.fetched && !subNodeInfo.isFetching) {
                return dispatch(faSubNodeInfoFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

export function faSubNodeInfoFetch(versionId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(faSubNodeInfoRequest(versionId, nodeId, nodeKey))
        return WebApi.getFaTree(versionId, nodeId)
            .then(json => dispatch(faSubNodeInfoReceive(versionId, nodeId, nodeKey, json)))
    };
}

export function faSubNodeInfoReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FA_SUB_NODE_INFO_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        childNodes: json.nodes,
        receivedAt: Date.now()
    }
}

export function faSubNodeInfoRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_INFO_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}
