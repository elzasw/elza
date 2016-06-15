import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    fetched: false,
    fetching: false,
    filterText: '',
    filterState: { type: "all" },
    currentDataKey: '',
    users: [],
    usersCount: 0,
}

export default function user(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_SAVE:
                // fundDetail: fundDetail(state.fundDetail, action)
            return {
            }
        case types.STORE_LOAD:
            if (action.fundRegion) {
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
                }
                    // fundDetail: fundDetail(action.fundRegion.fundDetail, action),
            } else {
                return state
            }
        // case types.FUNDS_SELECT_FUND:
        // case types.FUNDS_FUND_DETAIL_REQUEST:
        // case types.FUNDS_FUND_DETAIL_RECEIVE:
        //     return {
        //         ...state,
        //         fundDetail: fundDetail(state.fundDetail, action),
        //     }
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

