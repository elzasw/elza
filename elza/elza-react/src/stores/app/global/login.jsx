import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    logged: false,
};

export default function login(state = initialState, action = {}) {
    switch (action.type) {
        case types.LOGIN_SUCCESS:
            return {
                ...state,
                logged: true,
            };
        case types.LOGIN_FAIL:
            return {
                ...state,
                logged: false,
            };
        case types.LOGOUT:
            return {
                ...state,
                logged: false,
            };
        default:
            return state;
    }
}
