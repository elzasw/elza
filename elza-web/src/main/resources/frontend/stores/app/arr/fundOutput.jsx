import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import fundOutputDetail from './fundOutputDetail.jsx'
import {isFundOutputDetail} from 'actions/arr/fundOutput.jsx'
import {consolidateState} from 'components/Utils.jsx'

const initialState = {
    fetched: false,
    fetching: false,
    currentDataKey: '',
    outputs: [],
    fundOutputDetail: fundOutputDetail(),
}

export default function fundPackets(state = initialState, action = {}) {
    if (isFundOutputDetail(action)) {
        return {
            ...state,
            fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
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
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
            }
            break
        case types.STORE_SAVE:
            // const {filterText, filterState} = state;
            return {
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
            }
            break
        case types.OUTPUT_CHANGES_DETAIL:
            var result = {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
            }
            return consolidateState(state, result)
        case types.OUTPUT_CHANGES:
            var result = {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
            }
            var index = indexById(action.outputIds)
            if (index !== null) {
                result.currentDataKey = ''
            }
            return consolidateState(state, result)
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
