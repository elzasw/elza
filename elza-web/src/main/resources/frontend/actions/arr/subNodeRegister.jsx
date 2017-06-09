import {WebApi} from 'actions/index.jsx';
import {indexById, findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/ActionTypes.js';
import {savingApiWrapper} from 'actions/global/status.jsx';
import {increaseNodeVersion} from 'actions/arr/node.jsx';

export function isSubNodeRegisterAction(action) {
    switch (action.type) {
        case types.FUND_SUB_NODE_REGISTER_REQUEST:
        case types.FUND_SUB_NODE_REGISTER_RECEIVE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_CREATE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_UPDATE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_DELETE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_SAVING:
        case types.FUND_SUB_NODE_REGISTER_VALUE_DELETE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_ADD:
        case types.FUND_SUB_NODE_REGISTER_VALUE_CHANGE:
        case types.FUND_SUB_NODE_REGISTER_VALUE_FOCUS:
        case types.FUND_SUB_NODE_REGISTER_VALUE_BLUR:
            return true;
        default:
            return false
    }
}


/**
 * Načtení dat pokud je potřeba.
 *
 * @param {number} versionId - id verze
 * @param {number|null} nodeId - data key
 * @param {number|string} routingKey - routing key
 */
export function fundSubNodeRegisterFetchIfNeeded(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        // Spočtení data key - se správným id
        const store = getSubNodeRegister(getState(), versionId, routingKey);

        if (!store) {
            return;
        }

        const dataKey = nodeId;
        if (store.currentDataKey !== dataKey) { // pokus se data key neschoduje, provedeme fetch
            dispatch(fundSubNodeRegisterRequest(versionId, nodeId, routingKey));

            if (nodeId !== null) {  // pokud chceme reálně načíst objekt, provedeme fetch
                return WebApi.getFundNodeRegister(versionId, nodeId)
                    .then(json => {
                        const newStore = getSubNodeRegister(getState(), versionId, routingKey);
                        const newDataKey = newStore.currentDataKey;
                        if (newDataKey === dataKey) {   // jen pokud příchozí objekt odpovídá dtům, které chceme ve store
                            dispatch(fundSubNodeRegisterReceive(versionId, nodeId, routingKey, json))
                        }
                    })
            } else {
                // Response s prázdným objektem
                dispatch(fundSubNodeRegisterReceive(versionId, nodeId, routingKey, null))
            }
        }
    }
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
        dataKey: nodeId,
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
        const register = subNodeRegister.data[index];
        const node = subNodeRegister.node;

        dispatch(increaseNodeVersion(versionId, nodeId, node.version));
        if (register.id !== null) {
            dispatch(fundSubNodeRegisterValueSaving(versionId, nodeId, routingKey, index));
            dispatch(fundSubNodeRegisterDelete(versionId, nodeId, {...register, node: subNodeRegister.node}, routingKey, index));
        } else {
            dispatch({
                type: types.FUND_SUB_NODE_REGISTER_VALUE_DELETE,
                versionId,
                nodeId,
                routingKey,
                index,
            });
        }
    }
}

const OperationType = {
    CREATE:'CREATE',
    DELETE:'DELETE',
    UPDATE:'UPDATE',
};

function fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, data, operationType) {
    let type = null;
    switch (operationType) {
        case OperationType.CREATE:
            type = types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_CREATE;
            break;
        case OperationType.DELETE:
            type = types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_DELETE;
            break;
        case OperationType.UPDATE:
            type = types.FUND_SUB_NODE_REGISTER_VALUE_RESPONSE_UPDATE;
            break;
        default:
            throw "Invalid operation type '" + operationType + "'!!";
    }

    return {
        type,
        versionId,
        nodeId,
        routingKey,
        index,
        data
    }
}

export function fundSubNodeRegisterDelete(versionId, nodeId, data, routingKey, index) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.deleteFundNodeRegister(versionId, nodeId, data)).then(json => {
            dispatch(fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, json, OperationType.DELETE));
        })
    }
}

export function fundSubNodeRegisterCreate(versionId, nodeId, data, routingKey, index) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.createFundNodeRegister(versionId, nodeId, data)).then(json => {
            dispatch(fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, json, OperationType.CREATE));
        })
    }
}

export function fundSubNodeRegisterUpdate(versionId, nodeId, data, routingKey, index) {
    return (dispatch, getState) => {
        savingApiWrapper(dispatch, WebApi.updateFundNodeRegister(versionId, nodeId, data)).then(json => {
            dispatch(fundSubNodeRegisterResponse(versionId, nodeId, routingKey, index, json, OperationType.UPDATE));
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


/**
 * Označí registr jako aktulně ukládaný
 *
 * @param versionId
 * @param nodeId
 * @param routingKey
 * @param index
 * @returns {{type: *, versionId: *, nodeId: *, routingKey: *, index: *}}
 */
function fundSubNodeRegisterValueSaving(versionId, nodeId, routingKey, index) {
    return {
        type: types.FUND_SUB_NODE_REGISTER_VALUE_SAVING,
        versionId,
        nodeId,
        routingKey,
        index,
    }
}


/**
 * On blur - vytvření / update
 * @param versionId
 * @param nodeId
 * @param routingKey
 * @param index
 * @returns {function(*, *)}
 */
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
        const register = subNodeRegister.data[index];
        const node = subNodeRegister.node;
        if (register && !register.hasError && register.touched) {
            dispatch(fundSubNodeRegisterValueSaving(versionId, nodeId, routingKey, index));
            dispatch(increaseNodeVersion(versionId, nodeId, node.version));

            if (register.id === null) { // Jen pokud je ještě není vytvořená
                dispatch(fundSubNodeRegisterCreate(versionId, nodeId, {...register, node: subNodeRegister.node}, routingKey, index));
            } else if (register.id && register.value != register.prevValue) { // pokud se hodnota změnila
                dispatch(fundSubNodeRegisterUpdate(versionId, nodeId, {...register, node: subNodeRegister.node}, routingKey, index))
            } else {
                console.warn('Unknown');
            }
        } else if (!register) {
            console.warn('Sub Node Register - fundSubNodeRegisterValueBlur - Register with index ' + index + ' does not exist!');
        }
    }
}