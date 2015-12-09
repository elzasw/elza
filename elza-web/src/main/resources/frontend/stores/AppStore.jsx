import { combineReducers, createStore } from 'redux'
import {SELECT_NODE, SELECT_FA, ADD_NODE, CLOSE_NODE, FIRST_FA_SELECT, CLOSE_FA, ADD_FA, AppActions} from './AppActions.jsx';

var {faActions} = AppActions;

function nodes(state = {activeIndex: null, items: []}, action) {
    switch (action.type) {
        case SELECT_NODE:
            var activeIndex;
            for (var a=0; a<state.items.length; a++) {
                if (state.items[a].id === action.id) {
                    activeIndex = a;
                    break;
                }
            }
            return {
                ...state,
                activeIndex: activeIndex
            }
        default:
            return state
    }
}

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

function fas(state = {activeIndex: null, items: []}, action) {
    console.log("fas::", "STATE", state, "ACTION", action);
    switch (action.type) {
        case ADD_NODE:
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
        case ADD_FA:
            return {
                ...state,
                items: [
                    ...state.items,
                    Object.assign({}, action.fa)
                ],
                activeIndex: activeIndex != null ? activeIndex : 0
            }
        case CLOSE_FA:
            var index = indexById(state.items, action.faId);
            var newActiveIndex = state.activeIndex;
            if (state.activeIndex == index) {   // byl vybrán, budeme řešit novou vybranou záložku
                if (index >= state.items.length - 1) {
                    if (index - 1 > 0) {
                        newActiveIndex = index - 1;
                    } else {
                        newActiveIndex = null;
                    }
                }
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
            var activeIndex = indexById(state.items, action.faId);
            return {
                ...state,
                activeIndex: activeIndex
            }
        case FIRST_FA_SELECT:
            var index = indexById(state.items, action.faId);
            return {
                ...state,
                items: [
                    state.items[index],
                    ...state.items.slice(0, index),
                    ...state.items.slice(index + 1)
                ],
                activeIndex: 0
            }
        default:
            return state
    }
}

let reducer = combineReducers({ fas });
let store = createStore(reducer);

var test = function() {
    console.log('---------');
    console.log('STORE: ', store.getState());
    store.dispatch(faActions.addFa({id:'fa1', name:'nazev fa1'}));
    store.dispatch(faActions.addFa({id:'fa2', name:'nazev fa2'}));
    store.dispatch(faActions.addFa({id:'fa3', name:'nazev fa3'}));
    store.dispatch(faActions.firstFaSelect('fa3'));
    console.log('STORE: ', store.getState());
}
module.exports = {
        test: function() {
            test();
        }
}