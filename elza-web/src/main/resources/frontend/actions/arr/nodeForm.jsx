import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

function getNodeForm(state, faId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, faId, nodeKey);
    if (r != null) {
        return r.node.nodeForm;
    }

    return null;
}
export function faNodeFormFetchIfNeeded(faId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var nodeForm = getNodeForm(state, faId, nodeKey);

        if (nodeForm != null) {
            if (!nodeForm.fetched && !nodeForm.isFetching) {
                return dispatch(faNodeFormFetch(faId, nodeId, nodeKey));
            }
        }
    }
}

export function faNodeFormFetch(faId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(faNodeFormRequest(faId, nodeId, nodeKey))
        return WebApi.getFaNodeForm(faId, nodeId)
            .then(json => dispatch(faNodeFormReceive(faId, nodeId, nodeKey, json)))
    };
}

export function faNodeFormReceive(faId, nodeId, nodeKey, json) {
    return {
        type: types.FA_NODE_FORM_RECEIVE,
        faId,
        nodeId,
        nodeKey,
        node: json.node,
        attrDesc: json.attrDesc,
        childNodes: json.childNodes,
        receivedAt: Date.now()
    }
}

export function faNodeFormRequest(faId, nodeId, nodeKey) {
    return {
        type: types.FA_NODE_FORM_REQUEST,
        faId,
        nodeId,
        nodeKey,
    }
}
