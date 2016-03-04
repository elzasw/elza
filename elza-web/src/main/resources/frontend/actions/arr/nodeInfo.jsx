/**
 * Akce pro doplňující informace k záložce NODE.
 */

import {WebApi} from 'actions'
import {indexById, findByNodeKeyInGlobalState} from 'stores/app/utils.jsx'
import {barrier} from 'components/Utils'
import {isFaRootId} from 'components/arr/ArrUtils'
import * as types from 'actions/constants/ActionTypes';

/**
 * Dohledání store node pro předané parametry.
 * @param {state} kořenový store
 * @param {int} versionId verze AP
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
export function faNodeInfoFetchIfNeeded(versionId, nodeId, nodeKey) {
    return (dispatch, getState) => {
        var state = getState();
        var node = getNode(state, versionId, nodeKey);
        if (node != null && (!node.nodeInfoFetched || node.nodeInfoDirty ) && !node.isNodeInfoFetching) {
            return dispatch(faNodeInfoFetch(versionId, nodeId, nodeKey));
        }
    }
}

/**
 * Nové načtení dat.
 */
export function faNodeInfoFetch(versionId, nodeId, nodeKey) {
    return dispatch => {
        dispatch(faNodeInfoRequest(versionId, nodeId, nodeKey))

        var isRoot = isFaRootId(nodeId);

        var getFaTree, getNodeParents;
        if (isRoot) {
            getNodeParents = new Promise(function (resolve, reject) {
                resolve([]);
            })
            getFaTree = WebApi.getFaTree(versionId, null)
                .then(json => {
                    return {nodes: [json.nodes[0]]}
                })
        } else {
            getNodeParents = WebApi.getNodeParents(versionId, nodeId);
            getFaTree = WebApi.getFaTree(versionId, nodeId);
        }

        return barrier(
            getFaTree,
            getNodeParents
        )
        .then(data => {
            return {
                childNodes: data[0].data.nodes,
                parentNodes: data[1].data
            }
        })
        .then(json => dispatch(faNodeInfoReceive(versionId, nodeId, nodeKey, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {Object} json objekt s daty
 */
export function faNodeInfoReceive(versionId, nodeId, nodeKey, json) {
    return {
        type: types.FA_NODE_INFO_RECEIVE,
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
export function faNodeInfoRequest(versionId, nodeId, nodeKey) {
    return {
        versionId,
        nodeId,
        nodeKey,
        type: types.FA_NODE_INFO_REQUEST
    }
}
