import * as types from 'actions/constants/ActionTypes';

const initialState = {
    logged: true,
    callback: null
}

export default function login(state = initialState, action) {
    switch (action.type) {
        case types.LOGIN_SUCCESS:
            return Object.assign({}, state, {
                logged: true,
                callback: null
            })
        case types.LOGIN_FAIL:
            return Object.assign({}, state, {
                logged: false,
                callback: action.callback
            })
        case types.LOGOUT:
            return Object.assign({}, state, {
                logged: false,
                callback: null
            })
        default:
            return state
    }
}
