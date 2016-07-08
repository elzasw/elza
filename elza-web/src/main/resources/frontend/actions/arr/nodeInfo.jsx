/**
 * Akce pro doplňující informace k záložce NODE.
 */

import {WebApi} from 'actions/index.jsx';
import {indexById, findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx'
import {barrier} from 'components/Utils.jsx'
import {isFundRootId} from 'components/arr/ArrUtils.jsx'
import * as types from 'actions/constants/ActionTypes.js';

export function isNodeInfoAction(action) {
    switch (action.type) {
        case types.FUND_NODE_INFO_REQUEST:
        case types.FUND_NODE_INFO_RECEIVE:
            return true
        default:
            return false
    }
}

/**
 * Dohledání store node pro předané parametry.
 * @param {state} kořenový store
 * @param {int} versionId verze AS
 * @param {string} routingKey klíč určující umístění, např. u pořádání se jedná o identifikaci záložky NODE, ve které je formulář
 */
function getNode(state, versionId, routingKey) {
    var r = findByRoutingKeyInGlobalState(state, versionId, routingKey);
    if (r != null) {
        return r.node;
    }

    return null;
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function fundNodeInfoFetchIfNeeded(versionId, nodeId, routingKey) {
    return (dispatch, getState) => {
        var state = getState();
        var node = getNode(state, versionId, routingKey);
        if (node != null && (!node.nodeInfoFetched || node.nodeInfoDirty ) && !node.isNodeInfoFetching) {
            return dispatch(fundNodeInfoFetch(versionId, nodeId, routingKey));
        }
    }
}

/**
 * Nové načtení dat.
 */
export function fundNodeInfoFetch(versionId, nodeId, routingKey) {
    return dispatch => {
        dispatch(fundNodeInfoRequest(versionId, nodeId, routingKey))

        var isRoot = isFundRootId(nodeId);

        var getFundTree, getNodeParents;
        if (isRoot) {
            getNodeParents = new Promise(function (resolve, reject) {
                resolve([]);
            })
            getFundTree = WebApi.getFundTree(versionId, null)
                .then(json => {
                    return {nodes: [json.nodes[0]]}
                })
        } else {
            getNodeParents = WebApi.getNodeParents(versionId, nodeId);
            getFundTree = WebApi.getFundTree(versionId, nodeId);
        }

        return barrier(
            getFundTree,
            getNodeParents
        )
        .then(data => {
            return {
                childNodes: data[0].data.nodes,
                parentNodes: data[1].data
            }
        })
        .then(json => dispatch(fundNodeInfoReceive(versionId, nodeId, routingKey, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function fundNodeInfoReceive(versionId, nodeId, routingKey, json) {
    return {
        type: types.FUND_NODE_INFO_RECEIVE,
        versionId,
        nodeId,
        routingKey,
        childNodes: json.childNodes,
        parentNodes: json.parentNodes,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function fundNodeInfoRequest(versionId, nodeId, routingKey) {
    return {
        versionId,
        nodeId,
        routingKey,
        type: types.FUND_NODE_INFO_REQUEST
    }
}
