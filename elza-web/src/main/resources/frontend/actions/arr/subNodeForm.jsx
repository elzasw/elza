import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

function getSubNodeForm(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node.subNodeForm;
    }

    return null;
}
export function faSubNodeFormFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeForm = getSubNodeForm(state, versionId, nodeKey);

        if (subNodeForm != null) {
            if (!subNodeForm.fetched && !subNodeForm.isFetching) {
                return dispatch(faSubNodeFormFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

export function faSubNodeFormFetch(versionId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(faSubNodeFormRequest(versionId, nodeId, nodeKey))
        return WebApi.getFaNodeForm(versionId, nodeId)
            .then(json => dispatch(faSubNodeFormReceive(versionId, nodeId, nodeKey, json)))
    };
}

export function faSubNodeFormReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FA_SUB_NODE_FORM_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        data: json,
        receivedAt: Date.now()
    }
}

export function faSubNodeFormRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_FORM_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}
