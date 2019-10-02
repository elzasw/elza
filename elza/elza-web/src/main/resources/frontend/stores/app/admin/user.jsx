import * as types from 'actions/constants/ActionTypes.js';
import userDetail from './userDetail.jsx'
import {isUserDetailAction} from 'actions/admin/user.jsx'
import {isPermissionAction} from 'actions/admin/permission.jsx'

const initialState = {
    fetched: false,
    isFetching: false,
    filterText: '',
    filterState: { type: "all" },
    currentDataKey: '',
    users: [],
    usersCount: 0,
    userDetail: userDetail(),
}

export default function user(state = initialState, action = {}) {
    if (isUserDetailAction(action) || isPermissionAction(action)) {
        return {
            ...state,
            userDetail: userDetail(state.userDetail, action)
        }
    }

    switch (action.type) {
        case types.STORE_SAVE:
            const {filterText, filterState} = state

            return {
                filterText,
                filterState,
                userDetail: userDetail(state.userDetail, action),
            }
        case types.STORE_LOAD:
            if (action.adminRegion) {
                return {
                    ...state,
                    fetched: false,
                    isFetching: false,
                    filterText: '',
                    filterState: { type: "all" },
                    currentDataKey: '',
                    users: [],
                    usersCount: 0,
                    ...action.adminRegion.user,
                    userDetail: userDetail(action.adminRegion.user.userDetail, action),
                }
            } else {
                return state
            }
        case types.USERS_SEARCH:
            return {
                ...state,
                filterText: typeof action.filterText !== 'undefined' ? action.filterText : '',
                filterState: action.filterState,
                currentDataKey: '',
            }
        case types.USERS_REQUEST:
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey,
            }
        case types.USERS_RECEIVE:
            return {
                ...state,
                isFetching: false,
                fetched: true,
                users: action.data.users,
                usersCount: action.data.usersCount,
            }
        case types.CHANGE_USER:
            return {
                ...state,
                userDetail: userDetail(state.userDetail, action),
                currentDataKey:''
            }
        default:
            return state
    }
}

