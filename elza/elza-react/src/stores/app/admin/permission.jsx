import * as types from 'actions/constants/ActionTypes';

const initialState = {
    permissions: [],
};

export default function user(state = initialState, action = {}) {
    switch (action.type) {
        case types.PERMISSIONS_PERMISSION_CHANGE:
            return {
                ...state,
                permissions: [
                    ...state.permissions.slice(0, action.index),
                    action.value,
                    ...state.permissions.slice(action.index + 1),
                ],
            };
        case types.PERMISSIONS_PERMISSION_ADD:
            return {
                ...state,
                permissions: [...state.permissions, {permission: ''}],
            };
        case types.PERMISSIONS_PERMISSION_REMOVE:
            return {
                ...state,
                permissions: [
                    ...state.permissions.slice(0, action.index),
                    ...state.permissions.slice(action.index + 1),
                ],
            };
        case types.PERMISSIONS_PERMISSION_RECEIVE:
            return {
                ...state,
                permissions: action.permissions,
            };
        default:
            return state;
    }
}
