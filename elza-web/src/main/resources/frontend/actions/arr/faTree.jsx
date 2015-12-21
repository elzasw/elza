import {WebApi} from 'actions'
import * as types from 'actions/constants/actionTypes';

export function faTreeNodeExpandInt(node, addWaitingNode=false) {
    return {
        type: types.FA_FA_TREE_EXPAND_NODE,
        node,
        addWaitingNode,
    }
}

export function faTreeNodeExpand(node) {
    return (dispatch, getState) => {
        var state = getState();
        var activeFa = state.arrRegion.fas[state.arrRegion.activeIndex];
        var faTree = activeFa.faTree;

        dispatch(faTreeNodeExpandInt(node, true))

        var faId = activeFa.id;
        var versionId = activeFa.versionId;
        var nodeId = node.id;
        var expandedIds = {...faTree.expandedIds, ['n_' + nodeId]: true}
        return WebApi.getFaTree(faId, versionId, nodeId, expandedIds)
            .then(json => dispatch(faTreeReceive(faId, versionId, nodeId, expandedIds, json)));
    }
}

export function faTreeNodeCollapse(node) {
    return {
        type: types.FA_FA_TREE_COLLAPSE_NODE,
        node,
    }
}

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

export function faTreeFetch(faId, versionId, nodeId, expandedIds) {
    return dispatch => {
        dispatch(faTreeRequest(faId, versionId, nodeId, expandedIds))
        return WebApi.getFaTree(faId, versionId, nodeId, expandedIds)
            .then(json => dispatch(faTreeReceive(faId, versionId, nodeId, expandedIds, json)));
    }
}

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

export function faTreeRequest(faId, versionId, nodeId, expandedIds) {
    return {
        type: types.FA_FA_TREE_REQUEST,
        faId,
        versionId,
        nodeId,
        expandedIds,
    }
}
