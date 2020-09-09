import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetching: false,
    fetched: false,
    dirty: false,
    items: [],
    currentDataKey: null,
};

export default function outputTypes(state = initialState, action = {}) {
    switch (action.type) {
        case types.REF_OUTPUT_TYPES_REQUEST: {
            return {
                ...state,
                currentDataKey: action.dataKey,
                isFetching: true,
            };
        }
        case types.REF_OUTPUT_TYPES_RECEIVE: {
            if (action.dataKey !== state.currentDataKey) {
                return state;
            }
            return {
                ...state,
                isFetching: false,
                fetched: true,
                dirty: false,
                items: action.items,
                lastUpdated: Date.now(),
            };
        }
        case types.CHANGE_PACKAGE: {
            return {
                dirty: true,
            };
        }
        default:
            return state;
    }
}
