import {WebApi} from 'actions/index.jsx';
import {indexById, findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/ActionTypes.js';
import {savingApiWrapper} from 'actions/global/status.jsx';
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

export function fundSubNodeRegisterFetchIfNeeded(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        const subNodeRegister = getSubNodeRegister(getState(), versionId, routingKey);
        if (subNodeRegister != null) {
            if ((!subNodeRegister.fetched || subNodeRegister.dirty) && !subNodeRegister.isFetching) {
                return dispatch(fundSubNodeRegisterFetch(versionId, nodeId, routingKey));
            }
        }
    }
}

export function fundSubNodeRegisterFetch(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        dispatch(fundSubNodeRegisterRequest(versionId, nodeId, routingKey))
        return WebApi.getFundNodeRegister(versionId, nodeId)
                .then(json => dispatch(fundSubNodeRegisterReceive(versionId, nodeId, routingKey, json)))
    };
}

export function fundSubNodeRegisterReceive(versionId, nodeId, routingKey, json) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_RECEIVE,
        versionId,
        nodeId,
        routingKey,
        data: json,
        receivedAt: Date.now()
    }
}

export function fundSubNodeRegisterRequest(versionId, nodeId, routingKey) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_REQUEST,
        versionId,
        nodeId,
        routingKey,
    }
}

function getSubNodeRegister(state, versionId, routingKey) {
    const node = getNode(state, versionId, routingKey);
    if (node !== null) {
        return node.subNodeRegister;
    } else {
        return null;
    }
}

function getNode(state, versionId, routingKey) {
    const r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
    if (r != null) {
        return r.node;
    }

    return null;
}

export function fundSubNodeRegisterValueAdd(versionId, nodeId, routingKey) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_ADD,
        versionId,
        nodeId,
        routingKey
    }
}

export function fundSubNodeRegisterValueChange(versionId, nodeId, routingKey, index, record) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE,
        versionId,
        nodeId,
        routingKey,
        index,
        record
    }
}

export function fundSubNodeRegisterValueDelete(versionId, nodeId, routingKey, index) {
    return (dispatch, getState) => {
        const subNodeRegister = getSubNodeRegister(getState(), versionId, routingKey);
        const loc = subNodeRegister.getLoc(subNodeRegister, index);

        dispatch({
            type: types.FUND_SUB_NODE_REGISTER_VALUE_DELETE,
            versionId,
            nodeId,
            routingKey,
            index,
        })
        console.log("q", loc);
        if (typeof loc.link.id !== 'undefined') {
            dispatch(fundSubNodeRegisterDelete(versionId, nodeId, loc.link, routingKey, index));
        }
    }
}

export function fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, data, operationType) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE,
        versionId,
        nodeId,
        routingKey,
        index,
        operationType,
        data: data
    }
}

export function fundSubNodeRegisterDelete(versionId, nodeId, data, routingKey, index) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.deleteFundNodeRegister(versionId, nodeId, data)).then(json => {
            dispatch(fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, json, 'DELETE'));
        })
    }
}

export function fundSubNodeRegisterCreate(versionId, nodeId, data, routingKey, index) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.createFundNodeRegister(versionId, nodeId, data)).then(json => {
            dispatch(fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, json, 'CREATE'));
        })
    }
}

export function fundSubNodeRegisterUpdate(versionId, nodeId, data, routingKey, index) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.updateFundNodeRegister(versionId, nodeId, data)).then(json => {
            dispatch(fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, json, 'UPDATE'));
        });
    }
}

export function fundSubNodeRegisterValueFocus(versionId, nodeId, routingKey, index) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_FOCUS,
        versionId,
        nodeId,
        routingKey,
        index,
    }
}

export function fundSubNodeRegisterValueBlur(versionId, nodeId, routingKey, index) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FUND_SUB_NODE_REGISTER_VALUE_BLUR,
            versionId,
            nodeId,
            routingKey,
            index,
            receivedAt: Date.now()
        });

        const subNodeRegister = getSubNodeRegister(getState(), versionId, routingKey);
        const loc = subNodeRegister.getLoc(subNodeRegister, index);
        if (!loc.link.error.hasError && loc.link.touched) {
            // Jen pokud je již vytvořená a pokud se hodnota nebo specifikace změnila
            if (typeof loc.link.id !== 'undefined' && loc.link.value != loc.link.prevValue) {
                dispatch(fundSubNodeRegisterUpdate(versionId, nodeId, loc.link, routingKey, index))
            } else {
                dispatch(fundSubNodeRegisterCreate(versionId, nodeId, loc.link, routingKey, index));
            }
        }
    }
}