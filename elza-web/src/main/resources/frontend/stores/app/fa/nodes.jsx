import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

const initialState = {
    activeIndex: null,
    nodes: []
}

export default function nodes(state = initialState, action) {
    switch (action.type) {
        case types.GET_OBJECT_INFO:
            state.nodes.forEach(node => {
                action.objectInfo.addNode(node);
            });
            return state
        case types.FA_CLOSE_NODE:
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
        case types.FA_SELECT_NODE:
            var index = indexById(state.nodes, action.node.id);
            if (index == null) {    // není zatím v seznamu, přidáme ho tam
                if (action.moveToBegin) {
                    return {
                        ...state,
                        nodes: [
                            Object.assign({}, action.node),
                            ...state.nodes
                        ],
                        activeIndex: 0
                    }
                } else {
                    return {
                        ...state,
                        nodes: [
                            ...state.nodes,
                            Object.assign({}, action.node)
                        ],
                        activeIndex: state.nodes.length
                    }
                }
            } else {
                if (action.moveToBegin) {
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
        default:
            return state
    }
}
