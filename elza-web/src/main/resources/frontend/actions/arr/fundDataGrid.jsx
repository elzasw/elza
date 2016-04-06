/**
 * Akce pro hromadné tabulkové zobrazení a úpravy AS.
 */

import {WebApi} from 'actions';
import * as types from 'actions/constants/ActionTypes';
import {indexById, objectById} from 'stores/app/utils.jsx'
import {modalDialogHide} from 'actions/global/modalDialog'

export function isFundDataGridAction(action) {
    switch (action.type) {
        case types.FUND_FUND_DATA_GRID_FILTER:
        case types.FUND_FUND_DATA_GRID_FILTER_REQUEST:
        case types.FUND_FUND_DATA_GRID_FILTER_RECEIVE:
        case types.FUND_FUND_DATA_GRID_FILTER_CHANGE:
        case types.FUND_FUND_DATA_GRID_DATA_REQUEST:
        case types.FUND_FUND_DATA_GRID_DATA_RECEIVE:
        case types.FUND_FUND_DATA_GRID_PAGE_SIZE:
        case types.FUND_FUND_DATA_GRID_PAGE_INDEX:
        case types.FUND_FUND_DATA_GRID_COLUMN_SIZE:
        case types.FUND_FUND_DATA_GRID_SELECTION:
        case types.FUND_FUND_DATA_GRID_COLUMNS_SETTINGS:
        case types.FUND_FUND_DATA_GRID_FILTER_CLEAR_ALL:
            return true
        default:
            return false
    }
}

export function fundBulkModifications(versionId, descItemTypeId, specsIds, operationType, findText, replaceText, replaceSpecId, nodes) {
    console.log('#####findAndReplace', versionId, descItemTypeId, specsIds, operationType, findText, replaceText, replaceSpecId, nodes)
    
    return (dispatch, getState) => {
        switch (operationType) {
            case 'findAndReplace':
                    WebApi.replaceDataValues(versionId, descItemTypeId, specsIds, findText, replaceText, nodes)
                        .then(dispatch(modalDialogHide()))
                break
            case 'replace':
                    WebApi.XXXreplaceDataValues(versionId, descItemTypeId, specsIds, replaceText, replaceSpecId, nodes)
                        .then(dispatch(modalDialogHide()))
                break
            case 'delete':
                    WebApi.XXXdeleteDataValues(versionId, descItemTypeId, specsIds, nodes)
                        .then(dispatch(modalDialogHide()))
                break
        }
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

function _fundDataGridKey(state) {
    var str = ''
    str += '-pg' + state.pageSize
    str += '-pi' + state.pageIndex
    str += '-vc'
    Object.keys(state.visibleColumns).sort().forEach(k => {
        str += '-' + k
    })
    str += '-fi'
    Object.keys(state.filter).sort().forEach(k => {
        str += '-' + k
        str += JSON.stringify(state.filter[k])
    })
    return str
}

export function fundDataGridFilterClearAll(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FILTER_CLEAR_ALL,
        versionId,
    }
}

export function fundDataGridFilterUpdateData(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FILTER_CHANGE,
        versionId,
        descItemTypeId: null,
    }
}

export function fundDataGridFilterChange(versionId, descItemTypeId, filter) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FUND_FUND_DATA_GRID_FILTER_CHANGE,
            versionId,
            descItemTypeId,
            filter,
        })
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

        const dataKey = _fundDataGridKey(fundDataGrid)
        if (fundDataGrid.currentDataKey !== dataKey) {
            dispatch(_dataRequest(versionId, dataKey))

            WebApi.getFilteredNodes(versionId, pageIndex, pageSize, Object.keys(fundDataGrid.visibleColumns)).then(nodes => {
                const newState = getState();
                const newFund = objectById(state.arrRegion.funds, versionId, 'versionId')
                if (newFund !== null) {
                    const newFundDataGrid = fund.fundDataGrid
                    const newDataKey = _fundDataGridKey(fundDataGrid)

                    if (newDataKey === dataKey) {
                        var items = nodes.map(node => {
                            const {valuesMap, ...nodeRest} = node
                            return {id: nodeRest.node.id, ...nodeRest, ...node.valuesMap}
                        })

                        const newState = getState();
                        const newFund = objectById(newState.arrRegion.funds, versionId, 'versionId')
                        if (newFund) {
                            const newFundDataGrid = newFund.fundDataGrid
                            const newDataKey = _fundDataGridKey(newFundDataGrid)

                            if (newDataKey === dataKey) {   // ještě je pořád v tom stavu, pro jaký se načítala data
                                dispatch(_dataReceive(versionId, items))
                            }
                        }
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
 * Obecné nastavení sloupečků.
 */
export function fundDataGridSetColumnsSettings(versionId, visibleColumns, columnsOrder) {
    return {
        type: types.FUND_FUND_DATA_GRID_COLUMNS_SETTINGS,
        versionId,
        visibleColumns,
        columnsOrder,
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
console.log("$$$$$$$$$$$$$$$$$$$$$$$$$$$ FILTER", versionId, filter)
    return (dispatch, getState) => {
        dispatch(_filterRequest(versionId))

        WebApi.filterNodes(versionId, filter)
            .then(json => {
                dispatch(_filterReceive(versionId, json))
            })
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
function _dataRequest(versionId, dataKey) {
    return {
        type: types.FUND_FUND_DATA_GRID_DATA_REQUEST,
        versionId,
        dataKey,
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
