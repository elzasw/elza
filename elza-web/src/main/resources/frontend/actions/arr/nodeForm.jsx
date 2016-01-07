import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

function getNodeForm(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node.nodeForm;
    }

    return null;
}
export function faNodeFormFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var nodeForm = getNodeForm(state, versionId, nodeKey);

        if (nodeForm != null) {
            if (!nodeForm.fetched && !nodeForm.isFetching) {
                return dispatch(faNodeFormFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

export function faNodeFormFetch(versionId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(faNodeFormRequest(versionId, nodeId, nodeKey))
        return WebApi.getFaNodeForm(versionId, nodeId)
            .then(json => dispatch(faNodeFormReceive(versionId, nodeId, nodeKey, json)))
    };
}

export function faNodeFormReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FA_NODE_FORM_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        attrDesc: json.attrDesc,
        childNodes: json.childNodes,
        receivedAt: Date.now()
    }
}

export function faNodeFormRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_NODE_FORM_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}
