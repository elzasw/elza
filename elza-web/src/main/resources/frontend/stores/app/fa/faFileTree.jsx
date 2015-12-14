import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    items: []
}

export default function faFileTree(state = initialState, action) {
    switch (action.type) {
        case types.FA_REQUEST_FA_FILE_TREE:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.FA_RECEIVE_FA_FILE_TREE:
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
