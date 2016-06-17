import * as types from 'actions/constants/ActionTypes.js';
import groupDetail from './groupDetail.jsx'
import {isGroupDetailAction} from 'actions/admin/group.jsx'

const initialState = {
    fetched: false,
    fetching: false,
    filterText: '',
    currentDataKey: '',
    groups: [],
    groupsCount: 0,
    groupDetail: groupDetail(),
}

export default function group(state = initialState, action = {}) {
    if (isGroupDetailAction(action)) {
        return {
            ...state,
            groupDetail: groupDetail(state.groupDetail, action)
        }
    }
    
    switch (action.type) {
        case types.STORE_SAVE:
            const {filterText} = state
            return {
                filterText,
                groupDetail: groupDetail(state.groupDetail, action),
            }
        case types.STORE_LOAD:
            if (action.adminRegion) {
                return {
                    ...state,
                    fetched: false,
                    fetching: false,
                    filterText: '',
                    currentDataKey: '',
                    groups: [],
                    groupsCount: 0,
                    ...action.adminRegion.group,
                    groupDetail: groupDetail(action.adminRegion.group.groupDetail, action),
                }
            } else {
                return state
            }
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

