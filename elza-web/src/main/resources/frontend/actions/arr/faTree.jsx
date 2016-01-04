/**
 * Akce pro strom AP.
 * Vysvětlení pojmů:
 * uzel - JP
 */

import {WebApi} from 'actions'
import * as types from 'actions/constants/actionTypes';

/**
 * Rozbalení uzlu.
 * @param {Object} node uzel
 * @param {boolean} pokud je true, je pod rozbalovaný uzel přidán nový uzel s informací, že se načítají data
 */
export function _faTreeNodeExpand(node, addWaitingNode=false) {
    return {
        type: types.FA_FA_TREE_EXPAND_NODE,
        node,
        addWaitingNode,
    }
}

/**
 * Rozbalení uzlu.
 * @param {Object} node uzel
 */
export function faTreeNodeExpand(node) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var faTree = activeFa.faTree;

        dispatch(_faTreeNodeExpand(node, true))

        var faId = activeFa.id;
        var versionId = activeFa.versionId;
        var nodeId = node.id;
        var expandedIds = {...faTree.expandedIds, [nodeId]: true}
        return WebApi.getFaTree(faId, versionId, nodeId, expandedIds)
            .then(json => dispatch(faTreeReceive(faId, versionId, nodeId, expandedIds, json)));
    }
}

/**
 * Nastavení focusu pro uzel ve stromu.
 * @param {Object} node uzel
 */
export function faTreeFocusNode(node) {
    return {
        type: types.FA_FA_TREE_FOCUS_NODE,
        node,
    }
}

/**
 * Zabalení uzlu.
 * @param {Object} node uzel
 */
export function faTreeNodeCollapse(node) {
    return {
        type: types.FA_FA_TREE_COLLAPSE_NODE,
        node,
    }
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {faId} node finding aid id
 * @param {versionId} id verze
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeFetchIfNeeded(faId, versionId, expandedIds) {
    return (dispatch, getState) => {
        var state = getState();
        var faTree = state.arrRegion.fas[state.arrRegion.activeIndex].faTree;

        if (faTree.faId !== faId || faTree.versionId !== versionId || faTree.expandedIds !== expandedIds) {
            return dispatch(faTreeFetch(faId, versionId, null, expandedIds));
        } else if (!faTree.fetched && !faTree.isFetching) {
            return dispatch(faTreeFetch(faId, versionId, null, expandedIds));
        }
    }
}

/**
 * Nové načtení dat.
 * @param {faId} node finding aid id
 * @param {versionId} id verze
 * @param {nodeId} pokud je uvedeno, obsahuje id node, pro který se mají vracet data, pokud není uveden, vrací se celý strom
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeFetch(faId, versionId, nodeId, expandedIds) {
    return dispatch => {
        dispatch(faTreeRequest(faId, versionId, nodeId, expandedIds))
        return WebApi.getFaTree(faId, versionId, nodeId, expandedIds)
            .then(json => dispatch(faTreeReceive(faId, versionId, nodeId, expandedIds, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {faId} node finding aid id
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 * @param {Object} json objekt s daty
 */
export function faTreeReceive(faId, versionId, nodeId, expandedIds, json) {
    return {
        type: types.FA_FA_TREE_RECEIVE,
        faId,
        versionId,
        nodeId,
        expandedIds,
        nodes: json.nodes,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {faId} node finding aid id
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeRequest(faId, versionId, nodeId, expandedIds) {
    return {
        type: types.FA_FA_TREE_REQUEST,
        faId,
        versionId,
        nodeId,
        expandedIds,
    }
}
