/**
 * Akce pro vybranou záložku NODE pod konkrétní vybranou záložkou AP.
 */

import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {faSelectSubNode} from 'actions/arr/nodes';
import {indexById} from 'stores/app/utils.jsx'
import {isFaRootId} from 'components/arr/ArrUtils'

/**
 * Fetch dat pro otevřené záložky NODE, pokud je potřeba.
 * {int} versionId verze AP
 */
export function nodesFetchIfNeeded(versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var index = indexById(state.arrRegion.fas, versionId, "versionId");
        if (index !== null) {
            var fa = state.arrRegion.fas[index]
            var nodeIds = [];
            fa.nodes.nodes.forEach(node => {
                if (node.dirty && !node.isFetching) {
                    if (isFaRootId(node.id)) {  // virtuální představující AP, je nutné aktualizovat z AP
                        // bude se aktualizovat až s načtením a obnovením AP
                    } else {
                        nodeIds.push(node.id);
                    }
                }
            })

            if (nodeIds.length > 0) {
                dispatch(nodesRequest(versionId, nodeIds));

                WebApi.getNodes(versionId, nodeIds)
                    .then(json => {
                        dispatch(nodesReceive(versionId, json));
                    })
            }
        }
    }
}

/**
 * Akce vyžádání dat - informace do store.
 * {int} versionId verze AP
 * {Array} nodeIds seznam id node, pro které byla data vyžádána
 */
export function nodesRequest(versionId, nodeIds) {
    var nodeMap = {}
    nodeIds.forEach(id => {
        nodeMap[id] = true
    })

    return {
        type: types.FA_NODES_REQUEST,
        versionId,
        nodeMap
    }
}

/**
 * Akce přijetí dat - promítnutí do store.
 * {int} versionId verze AP
 * {Array} nodes seznam node
 */
export function nodesReceive(versionId, nodes) {
    var nodeMap = {}
    nodes.forEach(node => {
        nodeMap[node.id] = node
    })

    return {
        type: types.FA_NODES_RECEIVE,
        versionId,
        nodes,
        nodeMap
    }
}

/**
 * Výsledek hledání v seznamu sourozenců - Accordion.
 * {int} versionId verze AP
 * {int} nodeId id node dané záložky NODE
 * {string} nodeKey klíč dané záložky
 * {Array} nodeIds seznam vyfiltrovaných node
 */
export function faNodeSubNodeFulltextResult(versionId, nodeId, nodeKey, nodeIds) {
    return {
        type: types.FA_FA_SUBNODES_FULLTEXT_RESULT,
        versionId,
        nodeId,
        nodeKey,
        nodeIds
    }
}

/**
 * Hledání v seznamu sourozenců - Accordion pro konkrétní zobrazení.
 * {string} filterText podle jakého řetězce se má vyhledávat
 */
export function faNodeSubNodeFulltextSearch(filterText) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var activeNode = activeFa.nodes.nodes[activeFa.nodes.activeIndex];

        dispatch({
            type: types.FA_FA_SUBNODES_FULLTEXT_SEARCH,
            filterText
        })
        if (filterText !== '') {
            WebApi.findInFaTree(activeFa.versionId, activeNode.id, filterText, 'ONE_LEVEL')
                .then(json => {
                    dispatch(faNodeSubNodeFulltextResult(activeFa.versionId, activeNode.id, activeNode.nodeKey, json));
                    if (json.length > 0) {
                        dispatch(faSelectSubNode(json[0].nodeId, json[0].parent, false, null, true));
                    }
                })
        } else {
            dispatch(faNodeSubNodeFulltextResult(activeFa.versionId, activeNode.id, activeNode.nodeKey, []));
        }
    }
}

export function addJPBefore(node) {
    return {
        type: types.NODE_JP_ADD_BEFORE,
        data: {node}
    }
}

export function addJPAfter(node) {
    return {
        type: types.NODE_ADD_AFTER,
        data: {node}
    }
}

export function addNode(indexNode, parentNode, versionId, direction, descItemCopyTypes = null, scenarioName = null) {
    return (dispatch) => {
        parentNode = {
            id: parentNode.id,
            lastUpdate: parentNode.lastUpdate,
            version: parentNode.version
        };
        indexNode = {
            id: indexNode.id,
            lastUpdate: indexNode.lastUpdate,
            version: indexNode.version
        };
        return WebApi.addNode(indexNode, parentNode, versionId, direction, descItemCopyTypes, scenarioName).then((json) => {
            dispatch(faNodeChange(versionId, {newNode: json.node, indexNode: indexNode, parentNode: json.parentNode, direction: direction, action: "ADD"}));
            dispatch(faSelectSubNode(json.node.id, json.parentNode));
        });
    }
}

export function deleteNode(node, parentNode, versionId) {
    return (dispatch) => {
        parentNode = {
            id: parentNode.id,
            lastUpdate: parentNode.lastUpdate,
            version: parentNode.version
        };
        node = {
            id: node.id,
            lastUpdate: node.lastUpdate,
            version: node.version
        };
        return WebApi.deleteNode(node, parentNode, versionId).then((json) => {
            dispatch(faNodeChange(versionId, {node: json.node, parentNode: json.parentNode, action: "DELETE"}));
        });
    }
}

export function faNodeChange(versionId, data) {
    return {
        ...data,
        type: types.FA_NODE_CHANGE,
        versionId
    }
}
