import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    connected: false,
    loading: true,
    disconnectedOnError: false, // v případě, že byl proveden disconnect na základě chyby, je zde true a je vyplněna zpráva - disconnectedErrorMessage
    disconnectedErrorMessage: null,
}

export default function webSocket(state = initialState, action) {
    switch (action.type) {
        case types.GLOBAL_WEB_SOCKET_CONNECT:
            return Object.assign({}, state, {
                connected: true,
                loading: false,
            })
        case types.GLOBAL_WEB_SOCKET_DISCONNECT:
            return {
                ...state,
                connected: false,
                disconnectedOnError: action.disconnectedOnError,
                disconnectedErrorMessage: action.disconnectedErrorMessage
            }
        default:
            return state
    }
}
