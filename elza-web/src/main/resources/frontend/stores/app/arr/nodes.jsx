import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'


const nodeInitialState = {
    id: null,
    name: null,
    selectedSubNodeId: null,
    subNodes: [],
}
function node(state = nodeInitialState, action) {
    switch (action.type) {
        case types.FA_FA_SELECT_SUBNODE:
            return Object.assign({}, state, {selectedSubNodeId: action.subNodeId});
        default:
            return state;
    }
}
function nodeInitState(node) {
    return {
        id: node.id,
        name: node.name,
        selectedSubNodeId: null,
        subNodes: [],
    }
}

const nodesInitialState = {
    activeIndex: null,
    nodes: []
}
export default function nodes(state = nodesInitialState, action) {
    switch (action.type) {
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
            if (action.openNewTab || state.nodes.length == 0) {   // otevře se vždy nová záložka, nebo není žádná
                // Založíme novou záložku a vybereme ji
                newState = {
                    ...state,
                    nodes: [
                        ...state.nodes,
                        nodeInitState(action.subNodeParentNode)
                    ],
                    activeIndex: state.nodes.length
                }
            } else {    // pokusí se použít aktuální, pokud je, jinak vytvoří novou
                var index = state.activeIndex;
                newState = {
                    ...state,
                    nodes: [
                        ...state.nodes.slice(0, index),
                        nodeInitState(action.subNodeParentNode),
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
