const SELECT_FA = 'SELECT_FA'
const CLOSE_FA = 'CLOSE_FA'
const SELECT_NODE = 'SELECT_NODE'
const CLOSE_NODE = 'CLOSE_NODE'

function selectFa(faId) {
    return {
        type: SELECT_FA,
        id: faId
    }
}

function selectNode(nodeId) {
    return {
        type: SELECT_NODE,
        id: nodeId
    }
}

function closeFa(faId) {
    return {
        type: CLOSE_FA,
        id: faId
    }
}

import { combineReducers, createStore } from 'redux'

function nodes(state = {active: null, activeIndex: null, items: [{id:1}, {id:2}]}, action) {
console.log("nodes state: ", state, "action: ", action);
    switch (action.type) {
        case SELECT_NODE:
            var active;
            var activeIndex;
            for (var a=0; a<state.items.length; a++) {
                if (state.items[a].id === action.id) {
                    active = state.items[a].id;
                    activeIndex = a;
                    break;
                }
            }
            return {
                ...state,
                activeIndex: activeIndex,
                active: active
            }
        default:
            return state
    }
}

function fas(
    state = {
    active: null,
    activeIndex: null,
    items: [
        {
            id: 'fa1',
            nodes: nodes({active: null, activeIndex: null, items: [{id:'node1'}, {id:'node2'}]}, action)
        }
    ]
    },
    action
) {
    switch (action.type) {
        case SELECT_NODE:
            return {
                ...state,
                items: [
                    ...state.items.slice(0, state.activeIndex),
                    Object.assign({}, state.items[state.activeIndex], {nodes: nodes(state.items[state.activeIndex].nodes, action)}),
                    ...state.items.slice(state.activeIndex + 1)
                ]
            }
        case SELECT_FA:
            var active;
            var activeIndex;
            for (var a=0; a<state.items.length; a++) {
                if (state.items[a].id === action.id) {
                    active = state.items[a].id;
                    activeIndex = a;
                    break;
                }
            }
            return {
                ...state,
                activeIndex: activeIndex,
                active: active
            }
        default:
            return state
    }
}
/*
let reducer = combineReducers({ fas });
let sss = createStore(reducer);
console.log("state", sss.getState());
console.log("state active fa", sss.getState().fas.active);
console.log("before select fa");
sss.dispatch(selectFa('fa1'));
console.log("state", sss.getState());
console.log("state active fa id", sss.getState().fas.active);
console.log("state active fa index", sss.getState().fas.activeIndex);
console.log("state active fa", sss.getState().fas.items[sss.getState().fas.activeIndex]);
console.log("state active node", sss.getState().fas.items[sss.getState().fas.activeIndex].nodes.active);
console.log("before select node");
sss.dispatch(selectNode('node1'));
console.log("state", sss.getState());
console.log("state active fa", sss.getState().fas.items[sss.getState().fas.activeIndex]);
console.log("state active node", sss.getState().fas.items[sss.getState().fas.activeIndex].nodes.active);
*/