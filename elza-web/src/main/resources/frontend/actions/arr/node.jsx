import {WebApi} from 'actions';
import * as types from 'actions/constants/actionTypes';
import {faSelectSubNodeInt,faSelectSubNode} from 'actions/arr/nodes';
import {indexById} from 'stores/app/utils.jsx'

export function nodesFetchIfNeeded(versionId) {
    return (dispatch, getState) => {
        var state = getState();
        var index = indexById(state.arrRegion.fas, versionId, "versionId");
        if (index !== null) {
            var fa = state.arrRegion.fas[index]
            var nodeIds = [];
            fa.nodes.nodes.forEach(node => {
                if (node.dirty && !node.isFetching) {
                    nodeIds.push(node.id);
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

export function faNodeSubNodeFulltextResult(versionId, nodeId, nodeKey, nodeIds) {
    return {
        type: types.FA_FA_SUBNODES_FULLTEXT_RESULT,
        versionId,
        nodeId,
        nodeKey,
        nodeIds
    }
}

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
            dispatch(faNodeChange(versionId, {newNode: json, indexNode: indexNode, parentNode: parentNode, direction: direction, action: "ADD"}));
            dispatch(faSelectSubNodeInt(json.id,parentNode));
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
            dispatch(faNodeChange(versionId, {node: node, parentNode: parentNode, action: "DELETE"}));
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
