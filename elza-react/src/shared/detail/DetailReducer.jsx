import * as types from 'actions/constants/ActionTypes';
import {INVALIDATE, REQUEST, RESET, RESPONSE, SELECT, UPDATE_VALUE} from './DetailActions';

/**
 * Implicitní funkce pro načtení data key.
 * @returns {*|null|number|string}
 */
function getDataKey() {
    return this.id;
}

const initialState = {
    data: null,
    id: null,
    isFetching: false,
    fetched: false,
    currentDataKey: '',
    getDataKey: getDataKey,
    reducer: detail,
};

export default function detail(state = initialState, action = {}, config = null) {
    // Konfigurace
    if (config) {
        state = {...state};
        if (config.reducer) {
            // metoda pro reducer
            state.reducer = config.reducer;
        }
    }
    switch (action.type) {
        case REQUEST:
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey,
            };
        case RESPONSE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                data: action.data,
            };
        }
        case SELECT: {
            return {
                ...state,
                id: action.id,
                data: state.id === action.id ? state.data : null,
                fetched: state.id === action.id ? state.fetched : false,
            };
        }
        case INVALIDATE: {
            if (action.id && action.id !== state.id) {
                return state;
            }
            return {
                ...state,
                currentDataKey: '',
            };
        }
        case UPDATE_VALUE: {
            return {
                ...state,
                id: action.id,
                data: action.data,
                fetched: true,
                isFetching: false,
            };
        }
        case types.STORE_SAVE: {
            return {
                id: state.id,
                data: !state.isFetching && state.fetched ? state.data : null,
            };
        }
        case types.STORE_LOAD: {
            return {
                ...state,
                id: action.id,
            };
        }
        case RESET:
            return initialState;
        default:
            return state;
    }
}
