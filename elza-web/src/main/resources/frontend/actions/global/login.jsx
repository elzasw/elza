import * as types from 'actions/constants/ActionTypes.js';
import {WebApi} from 'actions/index.jsx';
import {userDetailChange, userDetailClear} from 'actions/user/userDetail.jsx'

export function loginFail(callback) {
    return {
        type: types.LOGIN_FAIL,
        callback
    }
}

export function loginSuccess() {
    return dispatch => {
        dispatch({
            type: types.LOGIN_SUCCESS
        })
        WebApi.getUserDetail()
            .then(userDetail => {
                dispatch(userDetailChange(userDetail))
            })
    }
}

export function logout() {
    return dispatch => {
        return WebApi.logout()
            .then(() => {
                dispatch({
                    type: types.LOGOUT
                });
                dispatch(userDetailClear())
            }).catch(() => {
                dispatch({
                    type: types.LOGOUT
                });
                dispatch(userDetailClear())
            });
    }
}
