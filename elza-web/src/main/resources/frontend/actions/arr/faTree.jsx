/**
 * Akce pro strom AP.
 * Vysvětlení pojmů:
 * uzel - JP
 */

import {WebApi} from 'actions'
import * as types from 'actions/constants/actionTypes';
import {indexById} from 'stores/app/utils.jsx'

/**
 * Rozbalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 * @param {boolean} pokud je true, je pod rozbalovaný uzel přidán nový uzel s informací, že se načítají data
 */
export function _faTreeNodeExpand(area, node, addWaitingNode=false) {
    return {
        type: types.FA_FA_TREE_EXPAND_NODE,
        area,
        node,
        addWaitingNode,
    }
}

/**
 * Rozbalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function faTreeNodeExpand(area, node) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var faTree = activeFa.faTree;

        dispatch(_faTreeNodeExpand(area, node, true))

        var versionId = activeFa.versionId;
        var nodeId = node.id;
        var expandedIds = {...faTree.expandedIds, [nodeId]: true}
        return WebApi.getFaTree(versionId, nodeId, expandedIds)
            .then(json => dispatch(faTreeReceive(area, versionId, nodeId, expandedIds, [], json)));
    }
}

/**
 * Nastavení focusu pro uzel ve stromu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function faTreeFocusNode(area, node) {
    return {
        type: types.FA_FA_TREE_FOCUS_NODE,
        area,
        node,
    }
}

/**
 * Zabalení uzlu.
 * @param {String} area oblast stromu
 * @param {Object} node uzel
 */
export function faTreeNodeCollapse(area, node) {
    return {
        type: types.FA_FA_TREE_COLLAPSE_NODE,
        area,
        node,
    }
}

/**
 * Vyžádání dat - aby byla ve store k dispozici.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeFetchIfNeeded(area, versionId, expandedIds, selectedId) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var faTree = activeFa.faTree;

        var fetch = false;

        var includeIds = [];
        if (selectedId != null && typeof selectedId !== 'undefined') {
            includeIds.push(selectedId);

            var isInView = indexById(faTree.nodes, selectedId);
            if (isInView == null) {
                if (!faTree.fetchingIncludeIds[selectedId]) {
                    fetch = true;
                }
            }
        }

        if (!faTree.fetched && !faTree.isFetching) {
            fetch = true;
        }

        if (fetch) {
            return dispatch(faTreeFetch(area, versionId, null, expandedIds, includeIds));
        }
    }
}

/**
 * Nové načtení dat.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} pokud je uvedeno, obsahuje id node, pro který se mají vracet data, pokud není uveden, vrací se celý strom
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeFetch(area, versionId, nodeId, expandedIds, includeIds=[]) {
    return dispatch => {
        dispatch(faTreeRequest(area, versionId, nodeId, expandedIds, includeIds))
        return WebApi.getFaTree(versionId, nodeId, expandedIds, includeIds)
            .then(json => dispatch(faTreeReceive(area, versionId, nodeId, expandedIds, includeIds, json)));
    }
}

/**
 * Nová data byla načtena.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 * @param {Object} json objekt s daty
 */
export function faTreeReceive(area, versionId, nodeId, expandedIds, includeIds, json) {
    return {
        type: types.FA_FA_TREE_RECEIVE,
        area,
        versionId,
        nodeId,
        expandedIds,
        includeIds,
        nodes: json.nodes,
        expandedIdsExtension: json.expandedIdsExtension,
        receivedAt: Date.now()
    }
}

/**
 * Bylo zahájeno nové načítání dat.
 * @param {String} area oblast stromu
 * @param {versionId} id verze
 * @param {nodeId} node id, pokud bylo požadováno
 * @param {expandedIds} seznam rozbalených uzlů
 */
export function faTreeRequest(area, versionId, nodeId, expandedIds, includeIds) {
    return {
        type: types.FA_FA_TREE_REQUEST,
        area,
        versionId,
        nodeId,
        expandedIds,
        includeIds,
    }
}
