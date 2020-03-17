import * as types from 'actions/constants/ActionTypes.js';

const initialState = {
    filterText: '',
    data: null,
    isFetching: false,
    fetched: false,
    currentDataKey: '',
};

export default function fundOutputFiles(state = initialState, action = {}) {
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
            const {filterText} = state;
            return {
                filterText,
            };
        }
        case types.OUTPUT_STATE_CHANGE: {
            return {
                ...state,
                currentDataKey: '',
            };
        }
        case types.FUND_OUTPUT_FILES_REQUEST: {
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey,
            };
        }
        case types.FUND_OUTPUT_FILES_RECEIVE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                data: action.data,
            };
        }
        case types.FUND_OUTPUT_FILES_FILTER: {
            return {
                ...state,
                filterText: action.filterText,
                currentDataKey: '',
            };
        }
        default:
            return state;
    }
}
