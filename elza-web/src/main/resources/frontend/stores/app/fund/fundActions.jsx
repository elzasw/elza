import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'

import {consolidateState} from 'components/Utils.jsx'
import {isFundTreeAction} from 'actions/arr/fundTree.jsx'
import fundDetail from './fundDetail.jsx'

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

export default function fundActions(state = initialState, action = {}) {
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
        default:
            return state;
    }
}