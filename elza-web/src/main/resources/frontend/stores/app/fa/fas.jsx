import * as types from 'actions/constants/actionTypes';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

import nodes from './nodes'

const initialState = {
    activeIndex: null,
    items: []
}

export default function fas(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_GET_OBJECT_INFO:
            state.items.forEach(fa => {
                action.objectInfo.addFa(fa);
                nodes(fa.nodes, action);
            });
            return state
        case types.FA_CLOSE_NODE:
        case types.FA_SELECT_NODE:
            return {
                ...state,
                items: [
                    ...state.items.slice(0, state.activeIndex),
                    Object.assign({}, state.items[state.activeIndex], {nodes: nodes(state.items[state.activeIndex].nodes, action)}),
                    ...state.items.slice(state.activeIndex + 1)
                ]
            }
        case types.FA_CLOSE_FA:
            var index = indexById(state.items, action.fa.id);
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
        case types.FA_SELECT_FA:
            var index = indexById(state.items, action.fa.id);
            if (index == null) {    // není zatím v seznamu, přidáme jí tam
                if (action.moveToBegin) {
                    return {
                        ...state,
                        items: [
                            Object.assign({}, action.fa),
                            ...state.items
                        ],
                        activeIndex: 0
                    }
                } else {
                    return {
                        ...state,
                        items: [
                            ...state.items,
                            Object.assign({}, action.fa)
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

