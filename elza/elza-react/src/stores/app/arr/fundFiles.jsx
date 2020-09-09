import * as types from 'actions/constants/ActionTypes';

const initialState = {
    filterText: '',
    data: null,
    isFetching: false,
    fetched: false,
    currentDataKey: '',
};

export default function fundFiles(state = initialState, action = {}) {
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
        case types.CHANGE_FILES: {
            return {
                ...state,
                currentDataKey: '',
            };
        }
        case types.FUND_FILES_REQUEST: {
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey,
            };
        }
        case types.FUND_FILES_RECEIVE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                data: action.data,
            };
        }
        case types.FUND_FILES_FILTER: {
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
