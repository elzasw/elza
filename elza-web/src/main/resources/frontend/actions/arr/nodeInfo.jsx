/**
 * Akce pro doplňující informace k záložce NODE.
 */

import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'
import {barrier} from 'components/Utils'
import {isFundRootId} from 'components/arr/ArrUtils'
import * as types from 'actions/constants/ActionTypes';

/**
 * Dohledání store node pro předané parametry.
 * @param {state} kořenový store
 * @param {int} versionId verze AS
 * @param {string} nodeKey klíč záložky NODE
 */
function getNode(state, versionId, nodeKey) {
    var r = findByNodeKeyInGlobalState(state, versionId, nodeKey);
    if (r != null) {
        return r.node;
    }

    return null;
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 */
export function fundNodeInfoFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var node = getNode(state, versionId, nodeKey);
        if (node != null && (!node.nodeInfoFetched || node.nodeInfoDirty ) && !node.isNodeInfoFetching) {
            return dispatch(fundNodeInfoFetch(versionId, nodeId, nodeKey));
        }
    }
}

/**
 * Nové načtení dat.
 */
export function fundNodeInfoFetch(versionId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(fundNodeInfoRequest(versionId, nodeId, nodeKey))

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
        .then(json => dispatch(fundNodeInfoReceive(versionId, nodeId, nodeKey, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function fundNodeInfoReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FUND_NODE_INFO_RECEIVE,
        versionId,
        nodeId,
        nodeKey,
        childNodes: json.childNodes,
        parentNodes: json.parentNodes,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 */
export function fundNodeInfoRequest(versionId, nodeId, nodeKey) {
    return {
        versionId,
        nodeId,
        nodeKey,
        type: types.FUND_NODE_INFO_REQUEST
    }
}
