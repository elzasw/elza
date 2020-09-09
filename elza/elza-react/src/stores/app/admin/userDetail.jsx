import * as types from 'actions/constants/ActionTypes';
import {isPermissionAction} from 'actions/admin/permission.jsx';
import permission from './permission.jsx';

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    permission: permission(),
};

export default function userDetail(state = initialState, action = {}) {
    if (isPermissionAction(action)) {
        return {
            ...state,
            permission: permission(state.permission, action),
        };
    }

    switch (action.type) {
        case types.STORE_SAVE:
            const {id} = state;
            return {};
        case types.STORE_LOAD:
            return {
                ...state,
                fetched: false,
                fetching: false,
                currentDataKey: '',
                permission: permission(),
            };
        case types.USERS_SELECT_USER:
            if (state.id !== action.id) {
                return {
                    ...state,
                    id: action.id,
                    currentDataKey: '',
                    fetched: false,
                };
            } else {
                return state;
            }
        case types.USERS_USER_DETAIL_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            };
        case types.USERS_USER_DETAIL_RECEIVE:
            const {permissions, ...mainData} = action.data;
            return {
                ...state,
                ...mainData,
                permissions,
                fetching: false,
                fetched: true,
            };
        case types.CHANGE_USER: {
            if (state.id !== initialState.id && action.userIds.indexOf(state.id) !== -1) {
                return {
                    ...state,
                    currentDataKey: '',
                };
            }
            return state;
        }
        default:
            return state;
    }
}
