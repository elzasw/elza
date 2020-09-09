import * as types from 'actions/constants/ActionTypes';

export function webSocketConnect() {
    return {
        type: types.GLOBAL_WEB_SOCKET_CONNECT,
    };
}

export function webSocketDisconnect(disconnectedOnError) {
    return {
        type: types.GLOBAL_WEB_SOCKET_DISCONNECT,
        disconnectedOnError,
    };
}
