import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'

import {consolidateState} from 'components/Utils'
import {isFundTreeAction} from 'actions/arr/fundTree'
import fundDetail from './fundDetail'

const initialState = {
    fetched: false,
    fetching: false,
    filterText: '',
    currentDataKey: '',
    funds: [],
    fundsCount: 0,
    fundDetail: fundDetail(),
}

export default function fundRegion(state = initialState, action = {}) {
    if (isFundTreeAction(action) && action.area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
        return {
            ...state,
            fundDetail: fundDetail(state.fundDetail, action),
        }
    }

    switch (action.type) {
        case types.FUNDS_SELECT_FUND:
        case types.FUNDS_FUND_DETAIL_REQUEST:
        case types.FUNDS_FUND_DETAIL_RECEIVE:
            return {
                ...state,
                fundDetail: fundDetail(state.fundDetail, action),
            }
        case types.FUNDS_SEARCH:
            return {
                ...state,
                filterText: typeof action.filterText !== 'undefined' ? action.filterText : '',
                currentDataKey: '',
            }
        case types.FUNDS_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            }
        case types.FUNDS_RECEIVE:
            return {
                ...state,
                fetching: false,
                fetched: true,
                funds: action.data.funds,
                fundsCount: action.data.fundsCount,
            }
        default:
            return state
    }
}

