import { combineReducers, createStore } from 'redux'
import {SELECT_NODE, SELECT_FA, CLOSE_NODE, CLOSE_FA, GET_OBJECT_INFO, AppActions} from './AppActions.jsx';

var {faActions, ObjectInfo} = AppActions;

function indexById(arr, id) {
    if (arr == null) {
        return null;
    }

    for (var a=0; a<arr.length; a++) {
        if (arr[a].id === id) {
            return a;
        }
    }
    return null;
}

function selectedAfterClose(arr, index) {
    if (index >= arr.length - 1) {
        if (index - 1 > 0) {
            return index - 1;
        } else {
            return null;
        }
    } else {
        return index;
    }
}

function nodes(state = {activeIndex: null, items: []}, action) {
    switch (action.type) {
        case GET_OBJECT_INFO:
            state.items.forEach(node => {
                action.objectInfo.addNode(node);
            });
            return state
        case CLOSE_NODE:
            var index = indexById(state.items, action.node.id);
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.items, index);
            }
            return {
                ...state,
                items: [
                    ...state.items.slice(0, index),
                    ...state.items.slice(index + 1)
                ],
                activeIndex: newActiveIndex
            }
        case SELECT_NODE:
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

function fas(state = {activeIndex: null, items: []}, action) {
    console.log("[fas]", "STATE", state, "ACTION", action);
    switch (action.type) {
        case GET_OBJECT_INFO:
            state.items.forEach(fa => {
                action.objectInfo.addFa(fa);
                nodes(fa.nodes, action);
            });
            return state
        case CLOSE_NODE:
        case SELECT_NODE:
            return {
                ...state,
                items: [
                    ...state.items.slice(0, state.activeIndex),
                    Object.assign({}, state.items[state.activeIndex], {nodes: nodes(state.items[state.activeIndex].nodes, action)}),
                    ...state.items.slice(state.activeIndex + 1)
                ]
            }
        case CLOSE_FA:
            var index = indexById(state.items, action.fa.id);
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                newActiveIndex = selectedAfterClose(state.items, index);
            }
            return {
                ...state,
                items: [
                    ...state.items.slice(0, index),
                    ...state.items.slice(index + 1)
                ],
                activeIndex: newActiveIndex
            }
        case SELECT_FA:
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

let reducer = combineReducers({ fas });
let store = createStore(reducer);

var test1 = function() {
    console.log('---------');
    console.log('STORE: ', store.getState());
    store.dispatch(faActions.selectFa({id:'fa1', name:'nazev fa1'}));
    store.dispatch(faActions.selectFa({id:'fa2', name:'nazev fa2'}));
    store.dispatch(faActions.selectFa({id:'fa3', name:'nazev fa3'}));
    store.dispatch(faActions.selectFa({id:'fa2'}, false));
    console.log('STORE: ', store.getState());
    store.dispatch(faActions.closeFa({id:'fa2'}));
    console.log('STORE: ', store.getState());
}
var test2 = function() {
    console.log('---------');
    console.log('STORE: ', store.getState());
    store.dispatch(faActions.selectFa({id:'fa1', name:'nazev fa1'}));
    store.dispatch(faActions.selectNode({id:'node1', name:'nazev node1'}));
    store.dispatch(faActions.selectNode({id:'node2', name:'nazev node2'}));
    console.log('STORE: ', store.getState());

    var objectInfo = new ObjectInfo();
    store.dispatch(faActions.getObjectInfo(objectInfo));
    console.log(objectInfo);
}
module.exports = {
        test: function() {
            test2();
        },
        store
}

/*



*/