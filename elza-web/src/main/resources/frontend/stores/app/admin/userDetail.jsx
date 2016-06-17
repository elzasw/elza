import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
}

export default function userDetail(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_SAVE:
            const {id} = state
            return {
                id,
            }
        case types.STORE_LOAD:
            return {
                ...state,
                fetched: false,
                fetching: false,
                currentDataKey: '',
            }
        case types.USERS_SELECT_USER:
            if (state.id !== action.id) {
                return {
                    ...state,
                    id: action.id,
                    currentDataKey: '',
                    fetched: false,
                }
            } else {
                return state
            }
        case types.USERS_USER_DETAIL_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            }
        case types.USERS_USER_DETAIL_RECEIVE:
            return {
                ...state,
                ...action.data,
                fetching: false,
                fetched: true,
            }
        default:
            return state
    }
}

