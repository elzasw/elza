/**
 * Akce pro doplňující informace pro aktuálně vybraný formulář node.
 */

import {WebApi} from 'actions/index.jsx';
import {findByRoutingKeyInGlobalState, indexById} from 'stores/app/utils.jsx'

import * as types from 'actions/constants/ActionTypes.js';

export function isSubNodeInfoAction(action) {
    switch (action.type) {
        case types.FUND_SUB_NODE_INFO_REQUEST:
        case types.FUND_SUB_NODE_INFO_RECEIVE:
            return true
        default:
            return false
    }
}

/**
 * Načtení subNodeInfo store pro předaná data.
 * @param {Object} state globální store
 * @param {int} versionId verze AS
 * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 * @return subNodeInfo store
 */
function getSubNodeInfoStore(state, versionId, routingKey) {
    var r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
    if (r != null) {
        return r.node.subNodeInfo;
    }

    return null;
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function fundSubNodeInfoFetchIfNeeded(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        var state = getState();
        var subNodeInfo = getSubNodeInfoStore(state, versionId, routingKey);

        if (subNodeInfo != null) {
            if (!subNodeInfo.fetched && !subNodeInfo.isFetching) {
                return dispatch(fundSubNodeInfoFetch(versionId, nodeId, routingKey));
            }
        }
    }
}

/**
 * Nové načtení dat.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function fundSubNodeInfoFetch(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        const state = getState();
        const r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
        let node;
        if (r != null) {
            node = r.node;
        }
        dispatch(fundSubNodeInfoRequest(versionId, nodeId, routingKey));
        return WebApi.getNodeData(versionId, nodeId, false, false, true, null, null, node ? null : node.filterText)
            .then(json => dispatch(fundSubNodeInfoReceive(versionId, nodeId, routingKey, {nodes: json.children})))
    };
}

/**
 * Nová data byla načtena.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 * @param {Object} json objekt s daty
 */
export function fundSubNodeInfoReceive(versionId, nodeId, routingKey, json) {
    return {
        type: types.FUND_SUB_NODE_INFO_RECEIVE,
        versionId,
        nodeId,
        routingKey,
        childNodes: json.nodes,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {int} versionId verze AS
 * @param {int} nodeId id node záložky, které se to týká
 * @param {int} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
export function fundSubNodeInfoRequest(versionId, nodeId, routingKey) {
    return {
        type: types.FUND_SUB_NODE_INFO_REQUEST,
        versionId,
        nodeId,
        routingKey,
    }
}
