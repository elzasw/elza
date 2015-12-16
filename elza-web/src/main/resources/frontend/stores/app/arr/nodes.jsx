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

function selectNodeTab(state, node, moveTabToBegin, openUnexistingNodeTab) {
    var index = indexById(state.nodes, node.id);
    if (index == null && openUnexistingNodeTab) {    // není zatím v seznamu, přidáme ho tam, jen pokud je to požadováno
        var nodeItem = nodeInitState(node);
        if (moveTabToBegin) {
            return {
                ...state,
                nodes: [
                    nodeItem,
                    ...state.nodes
                ],
                activeIndex: 0
            }
        } else {
            return {
                ...state,
                nodes: [
                    ...state.nodes,
                    nodeItem
                ],
                activeIndex: state.nodes.length
            }
        }
    } else if (index != null) { // existuje již v záložkách, vybereme ho
        if (moveTabToBegin) {
            return {
                ...state,
                nodes: [
                    state.nodes[index],
                    ...state.nodes.slice(0, index),
                    ...state.nodes.slice(index + 1)
                ],
                activeIndex: 0
            }
        } else {
            return {
                ...state,
                activeIndex: index
            }
        }
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
            // 1. Nejdříve výběr záložky a případně její vytvoření
            var index = indexById(state.nodes, action.subNodeParentNode.id);
            var newState;
            if (index == null) {    // Sub node parent node není jako záložka
                if (action.openUnexistingNodeTab) { // Chceme ho jako záložku vytvořit
                    // Založíme novou záložku a vybereme ji
                    newState = selectNodeTab(state, action.subNodeParentNode, action.moveTabToBegin, true);
                } else {    // nic neděláme, není jako záložka a nechceme je vytvořit, budeme akci výběru sub node ignorovat
                    return state;
                }
            } else {    // sub node parent node je jako záložka, pouze chceme přepnout aktuální záložku
                // Vybereme záložku
                newState = selectNodeTab(state, action.subNodeParentNode, action.moveTabToBegin, false);
            }

            // 2. Výběr subnode
            var index = indexById(newState.nodes, action.subNodeParentNode.id);
            return {
                ...newState,
                nodes: [
                    ...newState.nodes.slice(0, index),
                    node(newState.nodes[index], action),
                    ...newState.nodes.slice(index + 1)
                ],
            }
        case types.FA_FA_CLOSE_NODE_TAB:
            var index = indexById(state.nodes, action.node.id);
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
            return selectNodeTab(state, action.node, action.moveTabToBegin, true);
        default:
            return state
    }
}
