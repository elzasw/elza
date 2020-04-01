import * as types from 'actions/constants/ActionTypes.js';
import {indexById} from 'stores/app/utils.jsx';
import fundOutputDetail from './fundOutputDetail.jsx';
import fundOutputFiles from './fundOutputFiles.jsx';
import fundOutputFunctions from './fundOutputFunctions.jsx';
import {isFundOutputDetail} from 'actions/arr/fundOutput.jsx';
import {isFundOutputFilesAction} from 'actions/arr/fundOutputFiles.jsx';
import {isFundOutputFunctionsAction} from 'actions/arr/fundOutputFunctions.jsx';
import {consolidateState} from 'components/Utils.jsx';
import {outputFormActions} from 'actions/arr/subNodeForm.jsx';

const initialState = {
    fetched: false,
    fetching: false,
    currentDataKey: '',
    filterState: null,
    outputs: [],
    fundOutputDetail: fundOutputDetail(),
    fundOutputFiles: fundOutputFiles(),
    fundOutputFunctions: fundOutputFunctions(),
};

export default function fundOutput(state = initialState, action = {}) {
    if (isFundOutputDetail(action) || outputFormActions.isSubNodeFormAction(action)) {
        return {
            ...state,
            fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
        };
    }
    if (isFundOutputFilesAction(action)) {
        return {
            ...state,
            fundOutputFiles: fundOutputFiles(state.fundOutputFiles, action),
        };
    }
    if (isFundOutputFunctionsAction(action)) {
        return {
            ...state,
            fundOutputFunctions: fundOutputFunctions(state.fundOutputFunctions, action),
        };
    }

    switch (action.type) {
        case types.STORE_LOAD: {
            return {
                ...state,
                outputs: [],
                isFetching: false,
                fetched: false,
                currentDataKey: '',
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
                fundOutputFiles: fundOutputFiles(state.fundOutputFiles, action),
                fundOutputFunctions: fundOutputFunctions(state.fundOutputFunctions, action),
            };
        }
        case types.STORE_SAVE: {
            return {
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
                fundOutputFiles: fundOutputFiles(state.fundOutputFiles, action),
                fundOutputFunctions: fundOutputFunctions(state.fundOutputFunctions, action),
                filterState: state.filterState,
            };
        }
        case types.FUND_FUND_CHANGE_READ_MODE: {
            return {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
            };
        }
        case types.OUTPUT_CHANGES_DETAIL:
        case types.CHANGE_OUTPUTS: {
            let outputs = state.outputs;
            let currentDataKey = state.currentDataKey;
            if (action.type === types.OUTPUT_CHANGES_DETAIL) {
                for (let i = 0; i < action.outputIds.length; i++) {
                    if (indexById(outputs, action.outputIds[i]) != null) {
                        currentDataKey = '';
                        break;
                    }
                }
            }

            const result = {
                ...state,
                currentDataKey: currentDataKey,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
                fundOutputFunctions: fundOutputFunctions(state.fundOutputFunctions, action),
            };
            return consolidateState(state, result);
        }
        case types.OUTPUT_CHANGES: {
            const result = {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
            };
            const index = indexById(action.outputIds);
            if (index !== null) {
                result.currentDataKey = '';
            }
            return consolidateState(state, result);
        }
        case types.OUTPUT_STATE_CHANGE: {
            const result = {
                ...state,
                fundOutputDetail: fundOutputDetail(state.fundOutputDetail, action),
                fundOutputFiles: fundOutputFiles(state.fundOutputFiles, action),
            };
            for (const item of state.outputs) {
                if (item.id == action.outputId) {
                    result.currentDataKey = '';
                    break;
                }
            }
            return consolidateState(state, result);
        }
        case types.FUND_OUTPUT_REQUEST: {
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey,
            };
        }
        case types.FUND_OUTPUT_RECEIVE: {
            return {
                ...state,
                fetching: false,
                fetched: true,
                outputs: action.outputs,
            };
        }
        case types.FUND_OUTPUT_FILTER_STATE: {
            return {
                ...state,
                filterState: action.state,
            };
        }
        case types.CHANGE_FUND_ACTION: {
            return {
                ...state,
                fundOutputFunctions: fundOutputFunctions(state.fundOutputFunctions, action),
            };
        }

        case types.FUND_INVALID: {
            return {
                ...state,
                currentDataKey: '',
            };
        }

        default:
            return state;
    }
}
