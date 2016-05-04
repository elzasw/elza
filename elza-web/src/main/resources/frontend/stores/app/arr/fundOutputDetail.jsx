import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
}

export default function fundOutputDetail(state = initialState, action = {}) {
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
        case types.OUTPUT_CHANGES:
        case types.OUTPUT_CHANGES_DETAIL:
            if (action.outputIds.indexOf(state.id) !== -1) {
                return {
                    ...state,
                    currentDataKey: '',
                }
            } else {
                return state
            }
        case types.FUND_OUTPUT_SELECT_OUTPUT:
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
        case types.FUND_OUTPUT_DETAIL_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            }
        case types.FUND_OUTPUT_DETAIL_RECEIVE:
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

