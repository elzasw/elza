import * as types from 'actions/constants/ActionTypes.js';
import {REQUEST, RESPONSE, SELECT, INVALIDATE, UPDATE_VALUE} from './DetailActions'

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
    currentDataKey: "",
    getDataKey: getDataKey,
    reducer: detail,
}

export default function detail(state = initialState, action = {}) {
    switch (action.type) {
        case REQUEST:
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey,
            }
        case RESPONSE: {
            return {
                ...state,
                isFetching: false,
                fetched: true,
                data: action.data,
            }
        }
        case SELECT: {
            return {
                ...state,
                id: action.id,
                data: state.id === action.id ? state.data : null,
                fetched: state.id === action.id ? state.fetched : false,
            }
        }
        case INVALIDATE: {
            return {
                ...state,
                currentDataKey: "",
            }
        }
        case UPDATE_VALUE: {
            return {
                ...state,
                data: action.data
            }
        }
        case types.STORE_SAVE: {
            return {
                id: state.id,
            }
        }
        case types.STORE_LOAD: {
            return {
                ...initialState,
                id: action.id,
            }
        }
        default:
            return state
    }
}
