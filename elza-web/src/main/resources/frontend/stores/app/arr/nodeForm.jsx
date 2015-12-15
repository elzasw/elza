import * as types from 'actions/constants/actionTypes';

const initialState = {
    nodeId: null,
    versionId: null,
    isFetching: false,
    fetched: false,
    parentNodes: [],
    childNodes: [],
}

export default function nodeForm(state = initialState, action) {
    switch (action.type) {
        case types.FA_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                nodeId: action.nodeId,
                versionId: action.versionId,
                isFetching: true,
            })
        case types.FA_NODE_FORM_RECEIVE:
            return Object.assign({}, state, {
                nodeId: action.nodeId,
                versionId: action.versionId,
                isFetching: false,
                fetched: true,
                parentNodes: action.parentNodes,
                childNodes: action.childNodes,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}

