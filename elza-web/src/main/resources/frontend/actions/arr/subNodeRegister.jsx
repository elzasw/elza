import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/actionTypes';

export function faSubNodeRegisterFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeRegister = getSubNodeRegister(state, versionId, nodeKey);
        if (subNodeRegister != null) {
            if ((!subNodeRegister.fetched || subNodeRegister.dirty) && !subNodeRegister.isFetching) {
                return dispatch(faSubNodeRegisterFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

export function faSubNodeRegisterFetch(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        dispatch(faSubNodeRegisterRequest(versionId, nodeId, nodeKey))
        return WebApi.getFaNodeRegister(versionId, nodeId)
                .then(json => dispatch(faSubNodeRegisterReceive(versionId, nodeId, nodeKey, json)))
    };
}

export function faSubNodeRegisterReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FA_SUB_NODE_REGISTER_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        data: json,
        receivedAt: Date.now()
    }
}

export function faSubNodeRegisterRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_REGISTER_REQUEST,
        versionId,
        nodeId,
        nodeKey,
    }
}

function getSubNodeRegister(state, versionId, nodeKey) {
    var node = getNode(state, versionId, nodeKey);
    if (node !== null) {
        return node.subNodeRegister;
    } else {
        return null;
    }
}

function getNode(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node;
    }

    return null;
}