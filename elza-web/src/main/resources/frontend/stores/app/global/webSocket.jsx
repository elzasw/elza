import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    connected: false,
    loading: true,
}

export default function webSocket(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_WEB_SOCKET_CONNECT:
            return Object.assign({}, state, {
                connected: true,
                loading: false,
            })
        case types.GLOBAL_WEB_SOCKET_DISCONNECT:
            return Object.assign({}, state, {
                connected: false,
            })
        default:
            return state
    }
}
