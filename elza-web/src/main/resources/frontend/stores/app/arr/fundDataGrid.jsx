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
    dirty: false,
    items: [],
    itemsCount: 0,
    filter: {},
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

