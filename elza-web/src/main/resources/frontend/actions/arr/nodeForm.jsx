import {WebApi} from 'actions'

import * as types from 'actions/constants/actionTypes';

export function nodeFormFetchIfNeeded(nodeId, versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var nodeForm = state.arrangementRegion.nodeForm;

        if (nodeForm.nodeId !== nodeId || nodeForm.versionId !== versionId) {
            return dispatch(nodeFormFetch(nodeId, versionId));
        } else if (!nodeForm.fetched && !nodeForm.isFetching) {
            return dispatch(nodeFormFetch(nodeId, versionId));
        }
    }
}

export function nodeFormFetch(nodeId, versionId) {
    return dispatch => {
        dispatch(nodeFormRequest(nodeId, versionId))
        return WebApi.getNodeForm(nodeId, versionId)
            .then(json => dispatch(nodeFormReceive(nodeId, versionId, json)));
    }
}

export function nodeFormReceive(nodeId, versionId, json) {
    return {
        type: types.FA_NODE_FORM_RECEIVE,
        nodeId,
        versionId,
        parentNodes: json.parents,
        childNodes: json.children,
        receivedAt: Date.now()
    }
}

export function nodeFormRequest(nodeId, versionId) {
    return {
        type: types.FA_NODE_FORM_REQUEST,
        nodeId,
        versionId,
    }
}
