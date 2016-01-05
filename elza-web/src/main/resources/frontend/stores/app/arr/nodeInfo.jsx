import * as types from 'actions/constants/actionTypes';

const nodeInfoInitialState = {
    isFetching: false,
    fetched: false,
    childNodes: [],
    parentNodes: [],
}

export default function nodeInfo(state = nodeInfoInitialState, action) {
    switch (action.type) {
        case types.FA_NODE_INFO_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_NODE_INFO_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                childNodes: action.childNodes,
                parentNodes: action.parentNodes,
                lastUpdated: action.receivedAt
            })
        default:
            return state;
    }
}
