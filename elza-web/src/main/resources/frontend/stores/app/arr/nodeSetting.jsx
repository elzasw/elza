import * as types from 'actions/constants/ActionTypes.js';

import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    nodes: []
}

const createNodeState = (nodeId, descItemTypeCopyIds = [], descItemTypeLockIds = [], copyAll = false) => ({
    id: nodeId,
    descItemTypeCopyIds: descItemTypeCopyIds,
    descItemTypeLockIds: descItemTypeLockIds,
    copyAll: copyAll,
});

export default function nodeSetting(state = initialState, action) {
    switch (action.type) {

        case types.NODE_DESC_ITEM_TYPE_LOCK:

            var nodeIndex = indexById(state.nodes, action.nodeId);

            if (nodeIndex !== null) {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, nodeIndex),
                        Object.assign({}, state.nodes[nodeIndex], {descItemTypeLockIds: [
                            ...state.nodes[nodeIndex].descItemTypeLockIds,
                            action.descItemTypeId
                        ]}),
                        ...state.nodes.slice(nodeIndex + 1)
                    ]
                };
            } else {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes,
                        createNodeState(action.nodeId, undefined, action.descItemTypeId)
                    ]
                };
            }

        case types.NODE_DESC_ITEM_TYPE_UNLOCK:
            var nodeIndex = indexById(state.nodes, action.nodeId);
            if (nodeIndex !== null) {
                var typeIndex = state.nodes[nodeIndex].descItemTypeLockIds.indexOf(action.descItemTypeId);

                return {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, nodeIndex),
                        Object.assign({}, state.nodes[nodeIndex], {descItemTypeLockIds: [
                            ...state.nodes[nodeIndex].descItemTypeLockIds.slice(0, typeIndex),
                            ...state.nodes[nodeIndex].descItemTypeLockIds.slice(typeIndex + 1)
                        ]}),
                        ...state.nodes.slice(nodeIndex + 1)
                    ]
                };
            }
            return state;

        case types.NODE_DESC_ITEM_TYPE_UNLOCK_ALL:
            var nodeIndex = indexById(state.nodes, action.nodeId);
            if (nodeIndex !== null) {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, nodeIndex),
                        Object.assign({}, state.nodes[nodeIndex], {descItemTypeLockIds: []}),
                        ...state.nodes.slice(nodeIndex + 1)
                    ]
                };
            }
            return state;

        case types.NODE_DESC_ITEM_TYPE_COPY_ALL:
            var nodeIndex = indexById(state.nodes, action.nodeId);
            if (nodeIndex !== null) {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, nodeIndex),
                        Object.assign({}, state.nodes[nodeIndex], {copyAll: !state.nodes[nodeIndex].copyAll}),
                        ...state.nodes.slice(nodeIndex + 1)
                    ]
                };
            } else {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes,
                        createNodeState(action.nodeId, undefined, undefined, true)
                    ]
                };
            }
            return state;


        case types.NODE_DESC_ITEM_TYPE_COPY:

            var nodeIndex = indexById(state.nodes, action.nodeId);

            if (nodeIndex !== null) {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, nodeIndex),
                        Object.assign({}, state.nodes[nodeIndex], {descItemTypeCopyIds: [
                            ...state.nodes[nodeIndex].descItemTypeCopyIds,
                            action.descItemTypeId
                        ]}),
                        ...state.nodes.slice(nodeIndex + 1)
                    ]
                };
            } else {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes,
                        createNodeState(action.nodeId, action.descItemTypeId)
                    ]
                };
            }

        case types.NODE_DESC_ITEM_TYPE_NOCOPY:
            var nodeIndex = indexById(state.nodes, action.nodeId);
            if (nodeIndex !== null) {
                var typeIndex = state.nodes[nodeIndex].descItemTypeCopyIds.indexOf(action.descItemTypeId);

                return {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, nodeIndex),
                        Object.assign({}, state.nodes[nodeIndex], {descItemTypeCopyIds: [
                            ...state.nodes[nodeIndex].descItemTypeCopyIds.slice(0, typeIndex),
                            ...state.nodes[nodeIndex].descItemTypeCopyIds.slice(typeIndex + 1)
                        ]}),
                        ...state.nodes.slice(nodeIndex + 1)
                    ]
                };
            }
            return state;

        default:
            return state
    }
}

