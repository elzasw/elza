import * as types from 'actions/constants/actionTypes';

const initialState = {
    faId: null,
    versionId: null,
    isFetching: false,
    fetched: false,
    nodes: [],
    nodeMap: [],
}

/*
export default function faTreeData(state = initialState, action) {
    switch (action.type) {
        case types.FA_FA_TREE_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
                faId: action.faId,
                versionId: action.versionId,
            })
        case types.FA_FA_TREE_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                nodes: action.nodes,
                nodeMap: action.nodeMap,
                faId: action.faId,
                versionId: action.versionId,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
*/