import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    versionId: null,
    nodeId: null,
    attrDesc: null,
}

export default function subNodeForm(state = initialState, action) {
    switch (action.type) {
        case types.FA_SUB_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_SUB_NODE_FORM_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                versionId: action.versionId,
                nodeId: action.nodeId,
                data: action.data,
            })
        default:
            return state
    }
}

