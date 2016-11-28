import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';
import {userDetailChange, userDetailClear} from 'actions/user/userDetail.jsx'
import {routerNavigate} from "actions/router.jsx"
import {stompDisconnect, stompConnect} from "websocket"

export function loginFail(callback) {
    return {
        type: types.LOGIN_FAIL,
        callback
    }
}

export function loginSuccess() {
    return (dispatch, getState) => {
        // Reconnect websocketu - jinak by házelo AccessDenied, protože websocket byl inicializován s jiným přihlášením
        stompDisconnect();
        stompConnect();

        // ---
        let state = getState();
        WebApi.getUserDetail()
            .then(userDetail => {
                let action = {
                    type: types.LOGIN_SUCCESS,
                    reset: false,
                }
                if (state.userDetail.id != userDetail.id) {
                    dispatch(routerNavigate('/'));
                    action.reset = true;
                }
                dispatch(action)
                dispatch(userDetailChange(userDetail))
            })
    }
}

export function logout() {
    return dispatch => {
        dispatch(routerNavigate('/'));
        return WebApi.logout()
            .then(() => {
                dispatch({
                    type: types.LOGOUT,
                    reset: true,
                });
                //dispatch(userDetailClear())
            }).catch(() => {
                dispatch({
                    type: types.LOGOUT,
                    reset: true,
                });
                //dispatch(userDetailClear())
            });
    }
}
