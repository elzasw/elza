import * as types from 'actions/constants/actionTypes';

const initialState = {
    selectedId: null,
    expandedIds: {},
    searchedIds: null
}

export default function faTree(state = initialState, action) {
    switch (action.type) {
        case types.FA_EXPAND_FA_TREE:
            return Object.assign({}, state, {expandedIds: {...state.expandedIds, [action.node.id]: true}});
        case types.FA_COLLAPSE_FA_TREE:
            var expandedIds = {...state.expandedIds};
            delete expandedIds[action.node.id];
            return Object.assign({}, state, {expandedIds: expandedIds});
        default:
            return state
    }
}
