import * as types from 'actions/constants/ActionTypes';
import {indexById} from 'stores/app/utils.jsx'

import {consolidateState} from 'components/Utils'
import fundTree from './../arr/fundTree'
import {isFundTreeAction} from 'actions/arr/fundTree'

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    fundTree: fundTree(undefined, {type: ''}),
}

export default function fundDetail(state = initialState, action = {}) {
    if (isFundTreeAction(action) && action.area === types.FUND_TREE_AREA_FUNDS_FUND_DETAIL) {
        return {
            ...state,
            fundTree: fundTree(state.fundTree, action)
        }
    }

    switch (action.type) {
        case types.FUNDS_SELECT_FUND:
            if (state.id !== action.id) {
                return {
                    ...state,
                    id: action.id,
                    currentDataKey: '',
                    fetched: false,
                    fundTree: fundTree(undefined, {type: ''}),
                }
            } else {
                return state
            }
        case types.FUNDS_FUND_DETAIL_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            }
        case types.FUNDS_FUND_DETAIL_RECEIVE:
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

