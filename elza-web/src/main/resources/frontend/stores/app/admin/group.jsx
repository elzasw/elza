import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    fetched: false,
    fetching: false,
    filterText: '',
    currentDataKey: '',
    groups: [],
    groupsCount: 0,
}

export default function group(state = initialState, action = {}) {
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
                    currentDataKey: '',
                    groups: [],
                    groupsCount: 0,
                    ...action.adminRegion.group,
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
        case types.GROUPS_SEARCH:
            return {
                ...state,
                filterText: typeof action.filterText !== 'undefined' ? action.filterText : '',
                filterState: action.filterState,
                currentDataKey: '',
            }
        case types.GROUPS_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            }
        case types.GROUPS_RECEIVE:
            return {
                ...state,
                fetching: false,
                fetched: true,
                groups: action.data.groups,
                groupsCount: action.data.groupsCount,
            }
        default:
            return state
    }
}

