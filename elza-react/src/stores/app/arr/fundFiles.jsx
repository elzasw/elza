import * as types from 'actions/constants/ActionTypes';

const initialState = {
    filterText: '',
    from: 0,
    count: 0,
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
            const {filterText, from} = state;
            return {
                filterText,
                from,
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
                count: action.data.count,
                data: action.data,
            };
        }
        case types.FUND_FILES_FILTER: {
            return {
                ...state,
                filterText: action.filterText,
                from: action.from ?? 0,
                currentDataKey: '',
            };
        }
        default:
            return state;
    }
}
