import * as types from 'actions/constants/ActionTypes';

const initialState = {
    navigateTo: null,
    method: null,
};

export default function router(state = initialState, action) {
    switch (action.type) {
        case types.ROUTER_NAVIGATE:
            return {
                ...state,
                navigateTo: action.path,
                method: action.method,
            };
        case types.ROUTER_NAVIGATE_CLEAR:
            return {
                ...state,
                navigateTo: null,
                method: null,
            };
        default:
            return state;
    }
}
