import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import fundDetail from './fundOutputDetail.jsx'
import {isFundOutputDetail} from 'actions/arr/fundOutput.jsx'

const initialState = {
    fetched: false,
    fetching: false,
    currentDataKey: '',
    outputs: [],
    fundDetail: fundDetail(),
}

export default function fundPackets(state = initialState, action = {}) {
    if (isFundOutputDetail(action)) {
        return {
            ...state,
            fundDetail: fundDetail(state.fundDetail, action),
        }
    }
    
    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                outputs: [],
                isFetching: false,
                fetched: false,
                currentDataKey: '',
                fundDetail: fundDetail(state.fundDetail, action),
            }
            break
        case types.STORE_SAVE:
            // const {filterText, filterState} = state;
            return {
                fundDetail: fundDetail(state.fundDetail, action),
            }
            break
        case types.FUND_OUTPUT_REQUEST:
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            }            
        case types.FUND_OUTPUT_RECEIVE:
            return {
                ...state,
                fetching: false,
                fetched: true,
                outputs: action.outputs,
            }            
        default:
            return state
    }
}
