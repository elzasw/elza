/**
 * Akce pro hromadné tabulkové zobrazení a úpravy AS.
 */

import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {indexById, objectById} from 'stores/app/utils.jsx'

export function isFundDataGridAction(action) {
    switch (action.type) {
        case types.FUND_FUND_DATA_GRID_FILTER:
        case types.FUND_FUND_DATA_GRID_FILTER_REQUEST:
        case types.FUND_FUND_DATA_GRID_FILTER_RECEIVE:
        case types.FUND_FUND_DATA_GRID_DATA_REQUEST:
        case types.FUND_FUND_DATA_GRID_DATA_RECEIVE:
        case types.FUND_FUND_DATA_GRID_PAGE_SIZE:
        case types.FUND_FUND_DATA_GRID_PAGE_INDEX:
        case types.FUND_FUND_DATA_GRID_COLUMN_SIZE:
        case types.FUND_FUND_DATA_GRID_SELECTION:
            return true
        default:
            return false
    }
}

export function fundDataGridFetchFilterIfNeeded(versionId) {
    return (dispatch, getState) => {
        const state = getState();
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId')
        if (!fund) {
            return
        }

        const fundDataGrid = fund.fundDataGrid
        if (!fundDataGrid.fetchedFilter && !fundDataGrid.isFetchingFilter) {
            dispatch(fundDataGridFilter(versionId, fundDataGrid.filter))
        }
    }
}

export function fundDataGridFetchDataIfNeeded(versionId, pageIndex, pageSize) {
    return (dispatch, getState) => {
        const state = getState();
        const fund = objectById(state.arrRegion.funds, versionId, 'versionId')
        if (!fund) {
            return
        }

        const fundDataGrid = fund.fundDataGrid
        if (!fundDataGrid.fetchedFilter || fundDataGrid.isFetchingFilter) {  // již musí být načtený filtr
            return
        }

        if ((!fundDataGrid.fetchedData && !fundDataGrid.isFetchingData)
            ||
            (fundDataGrid.fetchedPageSize !== pageSize && fundDataGrid.fetchingPageSize !== pageSize)
            ||
            (fundDataGrid.fetchedPageIndex !== pageIndex && fundDataGrid.fetchingPageIndex !== pageIndex)
        ) {
            dispatch(_dataRequest(versionId, pageIndex, pageSize))

            new Promise(function (resolve, reject) {
                var items = []
                for (var a=pageIndex * pageSize; a<pageIndex * pageSize + pageSize; a++) {
                    items.push({
                        id: a,
                        firstname: 'jan ' + a,
                        surname: 'novak ' + a,
                        age: 10+2*a,
                        address: 'Nejaka ulice ' + a + ', 330 22, Plzen',
                        tel: 2*a%10 + 3*a%10 + 4*a%10 + 5*a%10 + 6*a%10 + 7*a%10 + 8*a%10 + 9*a%10 + 2*a%10
                    })
                    if (a % 4 == 0) {
                        items[items.length-1].address = items[items.length-1].address + items[items.length-1].address + items[items.length-1].address
                    }
                }
                resolve(items)
            }).then(items => {
                const newState = getState();
                const newFund = objectById(newState.arrRegion.funds, versionId, 'versionId')
                if (newFund) {
                    const newFundDataGrid = newFund.fundDataGrid

                    if (newFundDataGrid.pageIndex === pageIndex && newFundDataGrid.pageSize === pageSize) {
                        dispatch(_dataReceive(versionId, items))
                    }
                }
            })
        }
    }
}

/**
 * Nastavení velikosti stránky.
 */
export function fundDataGridSetPageSize(versionId, pageSize) {
    return (dispatch, getState) => {
        dispatch(_setPageSize(versionId, pageSize))
    }
}

/**
 * Nastavení aktuální stránky.
 */
export function fundDataGridSetPageIndex(versionId, pageIndex) {
    return (dispatch, getState) => {
        dispatch(_setPageIndex(versionId, pageIndex))
    }
}

/**
 * Nastavení velikosti stránky.
 */
function _setPageSize(versionId, pageSize) {
    return {
        type: types.FUND_FUND_DATA_GRID_PAGE_SIZE,
        versionId,
        pageSize,
    }
}

/**
 * Nastavení šířky sloupečku.
 */
export function fundDataGridSetColumnSize(versionId, columnId, width) {
    return {
        type: types.FUND_FUND_DATA_GRID_COLUMN_SIZE,
        versionId,
        columnId,
        width,
    }
}

/**
 * Nastavení označených sloupečků.
 */
export function fundDataGridSetSelection(versionId, ids) {
    return {
        type: types.FUND_FUND_DATA_GRID_SELECTION,
        versionId,
        ids,
    }
}

/**
 * Nastavení velikosti stránky.
 */
function _setPageIndex(versionId, pageIndex) {
    return {
        type: types.FUND_FUND_DATA_GRID_PAGE_INDEX,
        versionId,
        pageIndex,
    }
}

/**
 * Filtrování dat podle předaného filtru.
 */
export function fundDataGridFilter(versionId, filter) {
    return (dispatch, getState) => {
        dispatch(_filterRequest(versionId))
        const count = 1000
        dispatch(_filterReceive(versionId, count))
    }
}

/**
 * Byl volán request na filtrování dat.
 */
function _filterRequest(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FILTER_REQUEST,
        versionId,
    }
}

/**
 * Byla vyfiltrována data.
 */
function _filterReceive(versionId, itemsCount) {
    return {
        type: types.FUND_FUND_DATA_GRID_FILTER_RECEIVE,
        versionId,
        itemsCount,
    }
}

/**
 * Byl volán request na data.
 */
function _dataRequest(versionId,pageIndex, pageSize) {
    return {
        type: types.FUND_FUND_DATA_GRID_DATA_REQUEST,
        versionId,
        pageIndex,
        pageSize,
    }
}

/**
 * Byla vrácena data.
 */
function _dataReceive(versionId, items) {
    return {
        type: types.FUND_FUND_DATA_GRID_DATA_RECEIVE,
        versionId,
        items,
    }
}
