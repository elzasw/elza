import * as types from 'actions/constants/ActionTypes';

const initialState = {
    filterRecommended: true,
    data: [],
    isFetching: false,
    fetched: false,
    currentDataKey: '',
};

export default function fundOutputFunctions(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD: {
            return {
                ...state,
                selectedIds: [],
                data: null,
                isFetching: false,
                fetched: false,
                currentDataKey: '',
            };
        }
        case types.STORE_SAVE: {
            const {filterRecommended} = state;
            return {
                filterRecommended,
            };
        }
        case types.FUND_OUTPUT_FUNCTIONS_REQUEST: {
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey,
            };
        }
        case types.FUND_OUTPUT_FUNCTIONS_RECEIVE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                data: action.data,
            };
        }
        case types.FUND_OUTPUT_FUNCTIONS_FILTER: {
            return {
                ...state,
                filterRecommended: action.filterRecommended,
                currentDataKey: '',
            };
        }
        case types.OUTPUT_CHANGES_DETAIL:
        case types.OUTPUT_STATE_CHANGE:
        case types.CHANGE_FUND_ACTION: {
            return {
                ...state,
                fetched: false,
                currentDataKey: '',
            };
        }
        default:
            return state;
    }
}
