import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    childNodes: [],
    attrDesc: null,
}

export default function nodeForm(state = initialState, action) {
    switch (action.type) {
        case types.FA_NODE_FORM_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_NODE_FORM_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                childNodes: action.childNodes,
                attrDesc: action.attrDesc
            })
        default:
            return state
    }
}

