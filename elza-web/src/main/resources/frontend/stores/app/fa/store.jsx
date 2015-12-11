import {faActions, FA_REQUEST_FA_FILE_TREE, FA_RECEIVE_FA_FILE_TREE, FA_SELECT_NODE, FA_SELECT_FA, FA_CLOSE_NODE, FA_CLOSE_FA} from './actions.jsx';
import {GLOBAL_GET_OBJECT_INFO, ObjectInfo} from 'stores/app/global/actions.jsx';
import {indexById, selectedAfterClose} from 'stores/app/utils.jsx'

function nodes(state = {activeIndex: null, items: []}, action) {
    switch (action.type) {
        case GET_OBJECT_INFO:
            state.items.forEach(node => {
                action.objectInfo.addNode(node);
            });
            return state
        case FA_CLOSE_NODE:
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
        case FA_SELECT_NODE:
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
exports.nodes = nodes;

function fas(state = {activeIndex: null, items: []}, action) {
    //console.log("[fas]", "STATE", state, "ACTION", action);
    switch (action.type) {
        case GLOBAL_GET_OBJECT_INFO:
            state.items.forEach(fa => {
                action.objectInfo.addFa(fa);
                nodes(fa.nodes, action);
            });
            return state
        case FA_CLOSE_NODE:
        case FA_SELECT_NODE:
            return {
                ...state,
                items: [
                    ...state.items.slice(0, state.activeIndex),
                    Object.assign({}, state.items[state.activeIndex], {nodes: nodes(state.items[state.activeIndex].nodes, action)}),
                    ...state.items.slice(state.activeIndex + 1)
                ]
            }
        case FA_CLOSE_FA:
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
        case FA_SELECT_FA:
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
exports.fas = fas;

function faFileTree(state = {isFetching: false, fetched: false, items: []}, action) {
    //console.log("[faFileTree]", "STATE", state, "ACTION", action);
    switch (action.type) {
        case FA_REQUEST_FA_FILE_TREE:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case FA_RECEIVE_FA_FILE_TREE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                items: action.items,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
exports.faFileTree = faFileTree;
