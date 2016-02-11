import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: []
}

export default function descItemTypes(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_DESC_ITEM_TYPES_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.REF_DESC_ITEM_TYPES_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: action.receivedAt
            })
        case types.CHANGE_PACKAGE:
            return Object.assign({}, state, {
                dirty: true
            })
        default:
            return state
    }
}
