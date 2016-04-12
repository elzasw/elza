import * as types from 'actions/constants/ActionTypes';
import {WebApi} from 'actions'

export function loginFail(callback) {
    return {
        type: types.LOGIN_FAIL,
        callback
    }
}

export function loginSuccess() {
    return {
        type: types.LOGIN_SUCCESS
    }
}

export function logout() {
    return dispatch => {
        return WebApi.logout()
            .then(() => {
                dispatch({
                    type: types.LOGOUT
                });
            }).catch(() => {
                dispatch({
                    type: types.LOGOUT
                });
            });
    }
}
