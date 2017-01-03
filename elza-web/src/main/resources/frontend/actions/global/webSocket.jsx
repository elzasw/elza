import * as types from 'actions/constants/ActionTypes.js';

export function webSocketConnect() {
    return {
        type: types.GLOBAL_WEB_SOCKET_CONNECT
    }
}

export function webSocketDisconnect(disconnectedOnError, disconnectedErrorMessage) {
    return {
        type: types.GLOBAL_WEB_SOCKET_DISCONNECT,
        disconnectedOnError,
        disconnectedErrorMessage
    }
}
