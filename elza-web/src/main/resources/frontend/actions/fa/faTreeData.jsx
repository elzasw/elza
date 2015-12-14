import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function fetchFaTreeIfNeeded(faId, versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var faTreeData = state.arrangementRegion.faTreeData;
        if (faTreeData.faId !== faId || faTreeData.versionId !== versionId) {
            return dispatch(fetchFaTree(faId, versionId));
        } else if (!faTreeData.fetched && !faTreeData.isFetching) {
            return dispatch(fetchFaTree(faId, versionId));
        }
    }
}

export function fetchFaTree(faId, versionId) {
    return dispatch => {
        dispatch(requestFaTree(faId, versionId))
        return WebApi.getFaTree(faId, versionId)
            .then(json => dispatch(receiveFaTree(faId, versionId, json)));
    }
}

export function receiveFaTree(faId, versionId, json) {
    return {
        type: types.FA_RECEIVE_FA_TREE,
        faId,
        versionId,
        nodes: json.nodes,
        nodeMap: json.nodeMap,
        receivedAt: Date.now()
    }
}

export function requestFaTree(faId, versionId) {
    return {
        type: types.FA_REQUEST_FA_TREE,
        faId,
        versionId
    }
}
