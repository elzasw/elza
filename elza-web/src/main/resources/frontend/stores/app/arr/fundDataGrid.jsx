import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetchingFilter: false,
    fetchedFilter: false,
    isFetchingData: false,
    fetchedData: false,
    pageSize: 10,   // aktuální velikost stránky
    pageIndex: 0,   // aktuální stránka
    dirty: false,
    items: [],
    itemsCount: 0,
    filter: {}, // mapa id desc item type na filter data
    visibleColumns: {1: true, 2: true, 3: true},   // seznam mapa id na boolean viditelných sloupečků   // "4", "5", "8", "9", "11", "14", "17", "38", "42", "44", "50", "53"
    columnsOrder: [],   // seznam id desc item type - pořadí zobrazování sloupečků
    columnInfos: {},    // mapa id desc item type na informace o sloupečku, např. jeho šířce atp.
    selectedIds: [],
    currentDataKey: '',
}
for (var a=1; a<10; a++) {
initialState.visibleColumns[a] = true
}

export default function fundDataGrid(state = initialState, action = {}) {
    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                isFetchingFilter: false,
                fetchedFilter: false,
                isFetchingData: false,
                fetchedData: false,
                dirty: false,
                items: [],
                itemsCount: 0,
                selectedIds: [],
                currentDataKey: '',
            }
        case types.STORE_SAVE:
            const {pageSize, pageIndex, filter, visibleColumns, columnsOrder, columnInfos} = state;

            return {
                pageSize,
                pageIndex,
                filter,
                visibleColumns,
                columnsOrder,
                columnInfos,
            }
        case types.FUND_FUND_DATA_GRID_FILTER_CLEAR_ALL:
            return {
                ...state,
                filter: {},
                fetchedFilter: false,
            }
        case types.FUND_FUND_DATA_GRID_PAGE_SIZE:
            return {
                ...state,
                pageSize: action.pageSize,
                pageIndex: 0,
                dirty: true,
            }
        case types.FUND_FUND_DATA_GRID_COLUMNS_SETTINGS:
            return {
                ...state,
                visibleColumns: action.visibleColumns,
                columnsOrder: action.columnsOrder,
            }
        case types.FUND_FUND_DATA_GRID_COLUMN_SIZE:
            var columnInfos = {...state.columnInfos}
            var info = columnInfos[action.columnId] || {}
            info.width = action.width
            columnInfos[action.columnId] = info
            return {
                ...state,
                columnInfos: columnInfos,
            }
        case types.FUND_FUND_DATA_GRID_SELECTION:
            return {
                ...state,
                selectedIds: [...action.ids],
            }
        case types.FUND_FUND_DATA_GRID_PAGE_INDEX:
            return {
                ...state,
                pageIndex: action.pageIndex,
                dirty: true,
            }
        case types.FUND_FUND_DATA_GRID_FILTER_CHANGE:
            var filter = {...state.filter}

            if (action.descItemTypeId !== null) {   // null je pro případ, kdy jen chceme aktualizovat data
                if (action.filter) {
                    filter[action.descItemTypeId] = action.filter
                } else {
                    delete filter[action.descItemTypeId]
                }
            }

            return {
                ...state,
                filter: filter,
                fetchedFilter: false,
            }
        case types.FUND_FUND_DATA_GRID_FILTER_REQUEST:
            return {
                ...state,
                isFetchingFilter: true,
            }
        case types.FUND_FUND_DATA_GRID_FILTER_RECEIVE:
            return {
                ...state,
                isFetchingFilter: false,
                fetchedFilter: true,
                itemsCount: action.itemsCount,
                pageIndex: 0,
                currentDataKey: '', // vynucení načtení dat!!!
            }
        case types.FUND_FUND_DATA_GRID_DATA_REQUEST:
            return {
                ...state,
                isFetchingData: true,
                currentDataKey: action.dataKey,
            }
        case types.FUND_FUND_DATA_GRID_DATA_RECEIVE:
            return {
                ...state,
                isFetchingData: false,
                fetchedData: true,
                items: action.items,
            }
        default:
            return state
    }
}

