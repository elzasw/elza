import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'

import {consolidateState} from 'components/Utils.jsx'

const initialState = {
    list: {
        isFetching: false,
        fetched: false,
        currentDataKey: null,
        data: null
    },
    detail: {
        isFetching: false,
        fetched: false,
        currentDataKey: null,
        data: null
    }
};

export default function fundAction(state = initialState, action = {}) {
    switch (action) {
        case types.FUND_ACTIONS_ACTION_SELECT: {
            return {
                ...state,
                detail: {
                    ...state.detail,
                    currentDataKey: action.dataKey
                }
            }
        }
        case types.FUND_ACTIONS_ACTION_DETAIL_REQUEST: {
            return {
                ...state,
                detail: {
                    ...state.detail,
                    isFetching: true
                }
            }
        }
        case types.FUND_ACTIONS_ACTION_DETAIL_RECEIVE: {
            if (state.detail.currentDataKey !== action.dataKey) {
                return state;
            }
            return {
                ...state,
                detail: {
                    ...state.detail,
                    isFetching: false,
                    fetch: true,
                    data: action.data
                }
            }
        }
        case types.FUND_ACTIONS_FUND_SELECT: {
            return {
                ...state,
                detail: {
                    ...state.detail,
                    currentDataKey: action.dataKey
                }
            }
        }
        case types.FUND_ACTIONS_LIST_REQUEST: {
            return {
                ...state,
                list: {
                    ...state.list,
                    isFetching: true
                }
            }
        }
        case types.FUND_ACTIONS_LIST_RECEIVE: {
            if (state.list.currentDataKey !== action.dataKey) {
                return state;
            }
            return {
                ...state,
                list: {
                    ...state.list,
                    isFetching: false,
                    fetch: true,
                    data: action.data
                }
            }
        }
        default:
            return state;
    }
}