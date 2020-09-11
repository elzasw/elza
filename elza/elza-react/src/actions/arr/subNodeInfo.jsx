/**
 * Akce pro doplňující informace pro aktuálně vybraný formulář node.
 */

import {WebApi} from 'actions/index.jsx';
import {findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx';

import * as types from 'actions/constants/ActionTypes';

export function isSubNodeInfoAction(action) {
    switch (action.type) {
        case types.FUND_SUB_NODE_INFO_REQUEST:
        case types.FUND_SUB_NODE_INFO_RECEIVE:
            return true;
        default:
            return false;
    }
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
        receivedAt: Date.now(),
    };
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
    };
}
