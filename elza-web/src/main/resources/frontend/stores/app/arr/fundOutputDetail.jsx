import * as types from '../../../actions/constants/ActionTypes.js';
import subNodeForm from './subNodeForm.jsx'
import {outputFormActions} from '../../../actions/arr/subNodeForm.jsx'
import {consolidateState} from '../../../components/Utils.jsx'

const initialState = {
    id: null,
    fetched: false,
    fetching: false,
    currentDataKey: '',
    subNodeForm: subNodeForm()
};

export default function fundOutputDetail(state = initialState, action = {}) {
    if (outputFormActions.isSubNodeFormAction(action)) {
        const result = {
            ...state,
            subNodeForm: subNodeForm(state.subNodeForm, action),
        };
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.STORE_SAVE:{
            const {id} = state;
            return {
                id
            }
        }
        case types.STORE_LOAD:{
            return {
                ...state,
                fetched: false,
                fetching: false,
                currentDataKey: '',
                subNodeForm: subNodeForm()
            }
        }
        case types.OUTPUT_STATE_CHANGE:{
            if (state.fetched && action.outputId === state.outputDefinition.id) {
                return {
                    ...state,
                    currentDataKey: '',
                    outputDefinition: {
                        ...state.outputDefinition,
                        state: action.state
                    }
                }
            }
            return state
        }
        case types.OUTPUT_CHANGES:
        case types.OUTPUT_CHANGES_DETAIL:{
            if (action.outputIds.indexOf(state.id) !== -1) {
                return {
                    ...state,
                    subNodeForm: subNodeForm(state.subNodeForm, action),
                    currentDataKey: ''
                }
            }
            return state
        }
        case types.FUND_FUND_CHANGE_READ_MODE:{
            return {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
            }
        }
        case types.FUND_OUTPUT_SELECT_OUTPUT:{
            if (state.id !== action.id) {
                return {
                    ...state,
                    id: action.id,
                    currentDataKey: '',
                    fetched: false,
                    subNodeForm: subNodeForm()
                }
            }
            return state
        }
        case types.FUND_OUTPUT_DETAIL_REQUEST:{
            return {
                ...state,
                fetching: true,
                currentDataKey: action.dataKey
            }
        }
        case types.FUND_OUTPUT_DETAIL_RECEIVE:{
            return {
                ...state,
                ...action.data,
                fetching: false,
                fetched: true
            }
        }
        case types.CHANGE_OUTPUTS:
            if (action.outputDefinitionIds && action.outputDefinitionIds.indexOf(state.id) >= 0) {
                return {
                    ...state,
                    subNodeForm: subNodeForm(state.subNodeForm, action),
                }
            } else {
                return state;
            }
        case types.OUTPUT_INCREASE_VERSION:
            return {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action)
            }
        case types.FUND_OUTPUT_DETAIL_CLEAR:
            return initialState;        
        default:
            return state
    }
}

