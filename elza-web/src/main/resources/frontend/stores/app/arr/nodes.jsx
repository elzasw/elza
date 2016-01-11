import * as types from 'actions/constants/actionTypes';
import {indexById, findByNodeKeyInNodes, selectedAfterClose} from 'stores/app/utils.jsx'
import {node, nodeInitState} from './node.jsx'

const nodesInitialState = {
    activeIndex: null,
    nodes: []
}
export default function nodes(state = nodesInitialState, action) {
    switch (action.type) {
        case types.FA_NODE_INFO_REQUEST:
        case types.FA_NODE_INFO_RECEIVE:
        case types.FA_SUB_NODE_FORM_REQUEST:
        case types.FA_SUB_NODE_FORM_RECEIVE:
        case types.FA_SUB_NODE_INFO_REQUEST:
        case types.FA_SUB_NODE_INFO_RECEIVE:
            var r = findByNodeKeyInNodes(state, action.versionId, action.nodeKey);
            if (r) {
                return {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, r.nodeIndex),
                        node(state.nodes[r.nodeIndex], action),
                        ...state.nodes.slice(r.nodeIndex + 1)
                    ]
                }            
            } else {
                return state;
            }
        case types.GET_OBJECT_INFO:
            state.nodes.forEach(node => {
                action.objectInfo.addNode(node);
            });
            return state
        case types.FA_FA_SELECT_SUBNODE:
            var newState;

            if (action.subNodeParentNode == null) { // jen nulování
                return state;
            }

            // 1. Záložka
            if (action.openNewTab || state.nodes.length == 0) {   // otevře se vždy nová záložka, pokud není žádná
                // Založíme novou záložku a vybereme ji
                newState = {
                    ...state,
                    nodes: [
                        ...state.nodes,
                        nodeInitState(action.subNodeParentNode)
                    ],
                    activeIndex: state.nodes.length
                }
            } else {    // pokusí se použít aktuální
                var index = state.activeIndex;
                newState = {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, index),
                        nodeInitState(action.subNodeParentNode, state.nodes[index]),
                        ...state.nodes.slice(index + 1)
                    ],
                }
            }

            // 2. Výběr subnode
            var index = newState.activeIndex;
            return {
                ...newState,
                nodes: [
                    ...newState.nodes.slice(0, index),
                    node(newState.nodes[index], action),
                    ...newState.nodes.slice(index + 1)
                ],
            }
        case types.FA_FA_CLOSE_NODE_TAB:
            var index = action.index;
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.nodes, index);
            } else if (index < state.activeIndex) {
                newActiveIndex--;
            }
            return {
                ...state,
                nodes: [
                    ...state.nodes.slice(0, index),
                    ...state.nodes.slice(index + 1)
                ],
                activeIndex: newActiveIndex
            }
        case types.FA_FA_SELECT_NODE_TAB:
            return {
                ...state,
                activeIndex: action.index
            }            
        default:
            return state
    }
}
