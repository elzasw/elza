import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx'
import fundOutputDetail from './fundOutputDetail.jsx'
import fundOutputFiles from './fundOutputFiles.jsx'
import {isFundOutputDetail} from 'actions/arr/fundOutput.jsx'
import {isFundOutputFilesAction} from 'actions/arr/fundOutputFiles.jsx'
import {consolidateState} from 'components/Utils.jsx'
import {outputFormActions} from 'actions/arr/subNodeForm.jsx'

const initialState = {
    fetched: false,
    fetching: false,
    currentDataKey: '',
    outputs: [],
    fundOutputDetail: fundOutputDetail(),
}

export default function fundOutput(state = initialState, action = {}) {
    if (isFundOutputDetail(action) || outputFormActions.isSubNodeFormAction(action, "OUTPUT")) {
        return {
            ...state,
            fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action)
        }
    }
    if (isFundOutputFilesAction(action)) {
        return {
            ...state,
            fundOutputFiles: fundOutputFiles(state.fundOutputFiles, action)
        }
    }
    
    switch (action.type) {
        case types.STORE_LOAD:{
            return {
                ...state,
                outputs: [],
                isFetching: false,
                fetched: false,
                currentDataKey: '',
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action)
            }
        }
        case types.STORE_SAVE:{
            return {
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action)
            }
        }
        case types.OUTPUT_CHANGES_DETAIL:{
            const result = {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action)
            };
            return consolidateState(state, result)
        }
        case types.OUTPUT_CHANGES:{
            const result = {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action)
            };
            const index = indexById(action.outputIds);
            if (index !== null) {
                result.currentDataKey = ''
            }
            return consolidateState(state, result)
        }
        case types.GENERATED_OUTPUT:{
            const result = {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
                fundOutputFiles: fundOutputFiles(state.fundOutputFiles, action)
            };
            const index = indexById(action.outputIds);
            if (index !== null) {
                result.currentDataKey = ''
            }
            return consolidateState(state, result)
        }
        case types.FUND_OUTPUT_REQUEST:{
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey
            }
        }
        case types.FUND_OUTPUT_RECEIVE:{
            return {
                ...state,
                fetching: false,
                fetched: true,
                outputs: action.outputs
            };
        }
        default:
            return state
    }
}
