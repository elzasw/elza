/**
 * Akce pro doplňující informace k záložce NODE.
 */

import {WebApi} from 'actions/index.jsx';
import {findByRoutingKeyInGlobalState} from 'stores/app/utils.jsx';
import {isFundRootId} from 'components/arr/ArrUtils.jsx';
import * as types from 'actions/constants/ActionTypes.js';

export function isNodeInfoAction(action) {
    switch (action.type) {
        case types.FUND_NODE_INFO_REQUEST:
        case types.FUND_NODE_INFO_RECEIVE:
        case types.FUND_NODE_INFO_INVALIDATE:
            return true;
        default:
            return false;
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
export function fundNodeInfoFetchIfNeeded(versionId, nodeId, routingKey, showParents) {
    return (dispatch, getState) => {
        const state = getState();
        const node = getNode(state, versionId, routingKey);
        //console.log("FETCH_NODE",node);
        if (node != null && (!node.nodeInfoFetched || node.nodeInfoDirty) && !node.isNodeInfoFetching) {
            //console.log("FETCHING_NODE_INFO");
            return dispatch(fundNodeInfoFetch(versionId, nodeId, routingKey, showParents));
        }
    };
}

/**
 * Nové načtení dat.
 */
export function fundNodeInfoFetch(versionId, nodeId, routingKey, showParents) {
    return dispatch => {
        dispatch(fundNodeInfoRequest(versionId, nodeId, routingKey));

        const isRoot = isFundRootId(nodeId);
        let request;
        if (isRoot) {
            request = WebApi.getNodeData(versionId, null, false, false, true);
        } else {
            request = WebApi.getNodeData(versionId, nodeId, false, true, true);
        }
        request
            .then(data => {
                return {
                    childNodes: data.children ? data.children : [],
                    parentNodes: data.parents ? data.parents : [],
                };
            })
            .then(json => dispatch(fundNodeInfoReceive(versionId, nodeId, routingKey, json)));
    };
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function fundNodeInfoReceive(versionId, nodeId, routingKey, json, viewStartIndexInvalidate = false) {
    return {
        type: types.FUND_NODE_INFO_RECEIVE,
        versionId,
        nodeId,
        routingKey,
        childNodes: json.childNodes,
        nodeIndex: json.nodeIndex,
        nodeCount: json.nodeCount,
        parentNodes: json.parentNodes,
        receivedAt: Date.now(),
        viewStartIndexInvalidate,
    };
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function fundNodeInfoRequest(versionId, nodeId, routingKey) {
    return {
        versionId,
        nodeId,
        routingKey,
        type: types.FUND_NODE_INFO_REQUEST,
    };
}

/**
 * Marks node info as invalid to force new data request.
 */
export function fundNodeInfoInvalidate(versionId, nodeId, routingKey) {
    return {
        versionId,
        nodeId,
        routingKey,
        type: types.FUND_NODE_INFO_INVALIDATE,
    };
}
