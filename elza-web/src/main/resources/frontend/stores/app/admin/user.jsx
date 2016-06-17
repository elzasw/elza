import * as types from 'actions/constants/ActionTypes.js';
import userDetail from './userDetail.jsx'
import {isUserDetailAction} from 'actions/admin/user.jsx'

const initialState = {
    fetched: false,
    fetching: false,
    filterText: '',
    filterState: { type: "all" },
    currentDataKey: '',
    users: [],
    usersCount: 0,
    userDetail: userDetail(),
}

export default function user(state = initialState, action = {}) {
    if (isUserDetailAction(action)) {
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
                    fetching: false,
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
                fetching: true,
                currentDataKey: action.dataKey,
            }
        case types.USERS_RECEIVE:
            return {
                ...state,
                fetching: false,
                fetched: true,
                users: action.data.users,
                usersCount: action.data.usersCount,
            }
        default:
            return state
    }
}

