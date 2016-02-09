import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/ActionTypes';

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

export function faSubNodeRegisterValueAdd(versionId, nodeId, nodeKey) {
    return {
        type: types.FA_SUB_NODE_REGISTER_VALUE_ADD,
        versionId,
        nodeId,
        nodeKey
    }
}

export function faSubNodeRegisterValueChange(versionId, nodeId, nodeKey, index, value) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FA_SUB_NODE_REGISTER_VALUE_CHANGE,
            versionId,
            nodeId,
            nodeKey,
            index,
            value,
            dispatch
        })
    }
}

export function faSubNodeRegisterValueDelete(versionId, nodeId, nodeKey, index) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeRegister = getSubNodeRegister(state, versionId, nodeKey);
        var loc = subNodeRegister.getLoc(subNodeRegister, index);

        dispatch({
            type: types.FA_SUB_NODE_REGISTER_VALUE_DELETE,
            versionId,
            nodeId,
            nodeKey,
            index,
        })

        if (typeof loc.link.id !== 'undefined') {
            faSubNodeRegisterDelete(versionId, nodeId, loc.link)
                    .then(json => {
                        dispatch(faSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, json, 'DELETE'));
                    })
        }
    }
}

export function faSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, data, operationType) {
    return {
        type: types.FA_SUB_NODE_REGISTER_VALUE_RESPONSE,
        versionId,
        nodeId,
        nodeKey,
        index,
        operationType,
        data: data
    }
}

export function faSubNodeRegisterDelete(versionId, nodeId, data) {
    return WebApi.deleteFaNodeRegister(versionId, nodeId, data);
}

export function faSubNodeRegisterCreate(versionId, nodeId, data) {
    return WebApi.createFaNodeRegister(versionId, nodeId, data);
}

export function faSubNodeRegisterUpdate(versionId, nodeId, data) {
    return WebApi.updateFaNodeRegister(versionId, nodeId, data);
}

export function faSubNodeRegisterValueFocus(versionId, nodeId, nodeKey, index) {
    return {
        type: types.FA_SUB_NODE_REGISTER_VALUE_FOCUS,
        versionId,
        nodeId,
        nodeKey,
        index,
    }
}

export function faSubNodeRegisterValueBlur(versionId, nodeId, nodeKey, index) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FA_SUB_NODE_REGISTER_VALUE_BLUR,
            versionId,
            nodeId,
            nodeKey,
            index,
            receivedAt: Date.now()
        });

        var state = getState();
        var subNodeRegister = getSubNodeRegister(state, versionId, nodeKey);
        var loc = subNodeRegister.getLoc(subNodeRegister, index);

        if (!loc.link.error.hasError && loc.link.touched) {
            if (typeof loc.link.id !== 'undefined') {
                // Jen pokud se hodnota nebo specifikace změnila
                var needUpdate = false;

                if (loc.link.value != loc.link.prevValue) {
                    needUpdate = true;
                }

                if (needUpdate) {
                    faSubNodeRegisterUpdate(versionId, nodeId, loc.link)
                        .then(json => {
                            dispatch(faSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, json, 'UPDATE'));
                        })
                }
            } else {
                faSubNodeRegisterCreate(versionId, nodeId, loc.link)
                    .then(json => {
                        dispatch(faSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, json, 'CREATE'));
                    })
            }
        }
    }
}