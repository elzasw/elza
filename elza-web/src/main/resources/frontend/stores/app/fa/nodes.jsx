import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

const initialState = {
    activeIndex: null,
    items: []
}

export default function nodes(state = initialState, action) {
    switch (action.type) {
        case types.GET_OBJECT_INFO:
            state.items.forEach(node => {
                action.objectInfo.addNode(node);
            });
            return state
        case types.FA_CLOSE_NODE:
            var index = indexById(state.items, action.node.id);
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.items, index);
            } else if (index < state.activeIndex) {
                newActiveIndex--;
            }
            return {
                ...state,
                items: [
                    ...state.items.slice(0, index),
                    ...state.items.slice(index + 1)
                ],
                activeIndex: newActiveIndex
            }
        case types.FA_SELECT_NODE:
            var index = indexById(state.items, action.node.id);
            if (index == null) {    // není zatím v seznamu, přidáme ho tam
                if (action.moveToBegin) {
                    return {
                        ...state,
                        items: [
                            Object.assign({}, action.node),
                            ...state.items
                        ],
                        activeIndex: 0
                    }
                } else {
                    return {
                        ...state,
                        items: [
                            ...state.items,
                            Object.assign({}, action.node)
                        ],
                        activeIndex: state.items.length
                    }
                }
            } else {
                if (action.moveToBegin) {
                    return {
                        ...state,
                        items: [
                            state.items[index],
                            ...state.items.slice(0, index),
                            ...state.items.slice(index + 1)
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
