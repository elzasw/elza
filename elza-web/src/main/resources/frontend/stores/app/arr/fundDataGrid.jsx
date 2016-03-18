import * as types from 'actions/constants/ActionTypes';

const initialState = {
    isFetchingFilter: false,
    fetchedFilter: false,
    isFetchingData: false,
    fetchedData: false,
    pageSize: 10,   // aktuální velikost stránky
    pageIndex: 0,   // aktuální stránka
    fetchedPageSize: -1,     // naposledy načtána data - s jakou velikostí stránky byla data načtena
    fetchedPageIndex: -1,    // naposledy načtána data - pro jakou stránku byly data načtena
    fetchingPageSize: -1,     // aktuálně načtaná data
    fetchingPageIndex: -1,    // aktuálně načtaná data
    dirty: false,
    items: [],
    itemsCount: 0,
    filter: {},
    visibleColumns: {1: true, 2: true, 3: true},   // seznam mapa id na boolean viditelných sloupečků
    columnsOrder: [],   // seznam id desc item type - pořadí zobrazování sloupečků
    columnInfos: {},    // mapa id desc item type na informace o sloupečku, např. jeho šířce atp.
    selectedIds: [],
}
for (var a=1; a<100; a++) {
initialState.visibleColumns[a] = true
}

export default function fundDataGrid(state = initialState, action = {}) {
    switch (action.type) {
        case types.FUND_FUND_DATA_GRID_PAGE_SIZE:
            return {
                ...state,
                pageSize: action.pageSize,
                pageIndex: 0,
                dirty: true,
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
            }
        case types.FUND_FUND_DATA_GRID_DATA_REQUEST:
            return {
                ...state,
                isFetchingData: true,
                fetchingPageSize: action.pageSize,
                fetchingPageIndex: action.pageIndex,
            }
        case types.FUND_FUND_DATA_GRID_DATA_RECEIVE:
            return {
                ...state,
                isFetchingData: false,
                fetchedData: true,
                items: action.items,
                fetchedPageIndex: state.pageIndex,
                fetchedPageSize: state.pageSize,
            }
        default:
            return state
    }
}

