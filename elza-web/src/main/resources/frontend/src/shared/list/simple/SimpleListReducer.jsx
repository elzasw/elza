import * as types from 'actions/constants/ActionTypes.js';
import {REQUEST, RESPONSE, FILTER, SELECT_PARENT, INVALIDATE, RESET} from './SimpleListActions'

const simpleFilter = (rows, filter) => rows;

function getDataKey() {
    return "" + (typeof this.parent === "object" ? JSON.stringify(this.parent) : this.parent)  + "_" + JSON.stringify(this.filter);
}

const initialState = {
    sourceRows: [], // zdrojové řádky
    filteredRows: [], // filtrované řádky
    count: 0,
    rows: [],   // vyfiltrované řádky a případně jinak upravené řádky = určené pro konkrétní zobrazení
    isFetching: false,
    fetched: false,
    filter: {},
    filterRows: simpleFilter,
    currentDataKey: "",
    getDataKey,
    reducer: list,
    parent: null,
};

export default function list(state = initialState, action = {}, config = null) {
    // Konfigurace
    if (config) {
        state = {...state};
        if (config.filterRows) {    // metoda pro filtr
            state.filterRows = config.filterRows
        }
        if (config.getDataKey) {    // metoda pro datakey
            state.getDataKey = config.getDataKey
        }
        if (config.filter) {    // metoda pro filter
            state.filter = config.filter
        }
        if (config.reducer) {    // metoda pro reducer
            state.reducer = config.reducer
        }
    }

    switch (action.type) {
        case REQUEST:
            return {
                ...state,
                isFetching: true,
                currentDataKey: action.dataKey,
            };
        case SELECT_PARENT:
            return {
                ...state,
                parent: action.parent,
                fetched: false,
                isFetching: false,
                sourceRows: [],
                filteredRows: [],
                rows: [],
                count: 0
            };
        case INVALIDATE: {
            return {
                ...state,
                currentDataKey: initialState.currentDataKey
            }
        }
        case RESPONSE: {
            let sourceRows = action.rows;
            let filteredRows = state.filterRows(sourceRows, state.filter);
            let rows = filteredRows;

            return {
                ...state,
                isFetching: false,
                fetched: true,
                sourceRows,
                filteredRows,
                rows,
                count: action.count,
            }
        }
        case FILTER: {
            let filteredRows = state.filterRows(state.sourceRows, action.filter);
            let rows = filteredRows;

            return {
                ...state,
                filter: action.filter,
                filteredRows,
                rows,
            }
        }
        case types.STORE_SAVE: {
            return {
                filter: state.filter,
            }
        }
        case types.STORE_LOAD: {
            return {
                ...state,
                filter: action.filter
            }
        }
        case RESET:
            return initialState;
        default:
            return state
    }
}
