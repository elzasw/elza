import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function faTreeFetchIfNeeded(faId, versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var faTreeData = state.arrangementRegion.faTreeData;
        if (faTreeData.faId !== faId || faTreeData.versionId !== versionId) {
            return dispatch(faTreeFetch(faId, versionId));
        } else if (!faTreeData.fetched && !faTreeData.isFetching) {
            return dispatch(faTreeFetch(faId, versionId));
        }
    }
}

export function faTreeFetch(faId, versionId) {
    return dispatch => {
        dispatch(faTreeRequest(faId, versionId))
        return WebApi.getFaTree(faId, versionId)
            .then(json => dispatch(faTreeReceive(faId, versionId, json)));
    }
}

export function faTreeReceive(faId, versionId, json) {
    return {
        type: types.FA_FA_TREE_RECEIVE,
        faId,
        versionId,
        nodes: json.nodes,
        nodeMap: json.nodeMap,
        receivedAt: Date.now()
    }
}

export function faTreeRequest(faId, versionId) {
    return {
        type: types.FA_FA_TREE_REQUEST,
        faId,
        versionId
    }
}
