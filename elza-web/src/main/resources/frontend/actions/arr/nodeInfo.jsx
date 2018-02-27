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
        case types.FUND_NODE_INFO_INVALIDATE:
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
export function fundNodeInfoFetchIfNeeded(versionId, nodeId, routingKey, showParents) {
    return (dispatch, getState) => {
        var state = getState();
        var node = getNode(state, versionId, routingKey);
        //console.log("FETCH_NODE",node);
        if (node != null && (!node.nodeInfoFetched || node.nodeInfoDirty ) && !node.isNodeInfoFetching) {
            //console.log("FETCHING_NODE_INFO");
            return dispatch(fundNodeInfoFetch(versionId, nodeId, routingKey, showParents));
        }
    }
}

/**
 * Nové načtení dat.
 */
export function fundNodeInfoFetch(versionId, nodeId, routingKey, showParents) {
    return dispatch => {
        dispatch(fundNodeInfoRequest(versionId, nodeId, routingKey));

        const isRoot = isFundRootId(nodeId);
        let getFundTree;

        if (isRoot) {
            // sends nodeId as null, because root doesn't have valid id
            getFundTree = WebApi.getFundTree(versionId, null)
                .then(json => {
                    return {nodes: [json.nodes[0]]};
                });
        } else {
            getFundTree = WebApi.getFundTree(versionId, nodeId);
        }

        let getNodeParents;

        if (showParents && !isRoot) {
            getNodeParents = WebApi.getNodeParents(versionId, nodeId);
        } else {
            getNodeParents = new Promise(function (resolve, reject) {
                // empty response when parents are not needed
                resolve([]); 
            });
        }

        return barrier(
            getFundTree,
            getNodeParents
        )
        .then(data => {
            return {
                childNodes: data[0].data.nodes,
                parentNodes: data[1].data
            };
        })
        .then(json => dispatch(fundNodeInfoReceive(versionId, nodeId, routingKey, json)));
    };
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
/**
 * Marks node info as invalid to force new data request.
 */
export function fundNodeInfoInvalidate(versionId, nodeId, routingKey) {
    return {
        versionId,
        nodeId,
        routingKey,
        type: types.FUND_NODE_INFO_INVALIDATE
    }
}
