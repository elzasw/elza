import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: []
}

export default function ruleSet(state = initialState, action) {
    switch (action.type) {
        case types.REF_RULE_SET_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.REF_RULE_SET_RECEIVE:
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
