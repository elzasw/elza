import * as types from 'actions/constants/actionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    items: []
}

export default function packetTypes(state = initialState, action) {
    switch (action.type) {
        case types.REF_PACKET_TYPES_REQUEST:
            return Object.assign({}, state, {
                isFetching: true,
            })
        case types.REF_PACKET_TYPES_RECEIVE:
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
