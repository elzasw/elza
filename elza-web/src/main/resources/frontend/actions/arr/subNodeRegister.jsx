import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/ActionTypes';

export function isSubNodeRegisterAction(action) {
    switch (action.type) {
        case types.FUND_SUB_NODE_REGISTER_REQUEST:
        case types.FUND_SUB_NODE_REGISTER_RECEIVE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_DELETE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_ADD:
        case types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_FOCUS:
        case types.FUND_SUB_NODE_REGISTER_VALUE_BLUR:
            return true
        default:
            return false
    }
}

export function fundSubNodeRegisterFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeRegister = getSubNodeRegister(state, versionId, nodeKey);
        if (subNodeRegister != null) {
            if ((!subNodeRegister.fetched || subNodeRegister.dirty) && !subNodeRegister.isFetching) {
                return dispatch(fundSubNodeRegisterFetch(versionId, nodeId, nodeKey));
            }
        }
    }
}

export function fundSubNodeRegisterFetch(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        dispatch(fundSubNodeRegisterRequest(versionId, nodeId, nodeKey))
        return WebApi.getFundNodeRegister(versionId, nodeId)
                .then(json => dispatch(fundSubNodeRegisterReceive(versionId, nodeId, nodeKey, json)))
    };
}

export function fundSubNodeRegisterReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        data: json,
        receivedAt: Date.now()
    }
}

export function fundSubNodeRegisterRequest(versionId, nodeId, nodeKey) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_REQUEST,
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

export function fundSubNodeRegisterValueAdd(versionId, nodeId, nodeKey) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_ADD,
        versionId,
        nodeId,
        nodeKey
    }
}

export function fundSubNodeRegisterValueChange(versionId, nodeId, nodeKey, index, value) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE,
            versionId,
            nodeId,
            nodeKey,
            index,
            value,
            dispatch
        })
    }
}

export function fundSubNodeRegisterValueDelete(versionId, nodeId, nodeKey, index) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeRegister = getSubNodeRegister(state, versionId, nodeKey);
        var loc = subNodeRegister.getLoc(subNodeRegister, index);

        dispatch({
            type: types.FUND_SUB_NODE_REGISTER_VALUE_DELETE,
            versionId,
            nodeId,
            nodeKey,
            index,
        })

        if (typeof loc.link.id !== 'undefined') {
            fundSubNodeRegisterDelete(versionId, nodeId, loc.link)
                    .then(json => {
                        dispatch(fundSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, json, 'DELETE'));
                    })
        }
    }
}

export function fundSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, data, operationType) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE,
        versionId,
        nodeId,
        nodeKey,
        index,
        operationType,
        data: data
    }
}

export function fundSubNodeRegisterDelete(versionId, nodeId, data) {
    return WebApi.deleteFundNodeRegister(versionId, nodeId, data);
}

export function fundSubNodeRegisterCreate(versionId, nodeId, data) {
    return WebApi.createFundNodeRegister(versionId, nodeId, data);
}

export function fundSubNodeRegisterUpdate(versionId, nodeId, data) {
    return WebApi.updateFundNodeRegister(versionId, nodeId, data);
}

export function fundSubNodeRegisterValueFocus(versionId, nodeId, nodeKey, index) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_FOCUS,
        versionId,
        nodeId,
        nodeKey,
        index,
    }
}

export function fundSubNodeRegisterValueBlur(versionId, nodeId, nodeKey, index) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FUND_SUB_NODE_REGISTER_VALUE_BLUR,
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
                    fundSubNodeRegisterUpdate(versionId, nodeId, loc.link)
                        .then(json => {
                            dispatch(fundSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, json, 'UPDATE'));
                        })
                }
            } else {
                fundSubNodeRegisterCreate(versionId, nodeId, loc.link)
                    .then(json => {
                        dispatch(fundSubNodeRegisterResponse(versionId, nodeId, nodeKey, index, json, 'CREATE'));
                    })
            }
        }
    }
}