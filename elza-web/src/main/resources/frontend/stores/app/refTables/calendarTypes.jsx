import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: []
}

export default function calendarTypes(state = initialState, action) {
    switch (action.type) {
        case types.REF_CALENDAR_TYPES_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.REF_CALENDAR_TYPES_RECEIVE:
            return Object.assign({}, state, {
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: action.receivedAt
            })
        default:
            return state
    }
}
