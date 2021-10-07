/**
 * Akce pro hromadné tabulkové zobrazení a úpravy AS.
 */

import {WebApi} from 'actions/index';
import * as types from 'actions/constants/ActionTypes';
import {objectById} from 'stores/app/utils';
import {COL_REFERENCE_MARK} from 'components/arr/FundDataGridConst';

// Null hodnota, která se používaná v klientovi pro reprezentaci null hodnoty
export const FILTER_NULL_VALUE = '____$<NULL>$___';

export function isFundDataGridAction(action) {
    switch (action.type) {
        case types.FUND_FUND_DATA_GRID_INIT:
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
        case types.FUND_FUND_DATA_GRID_PREPARE_EDIT:
        case types.FUND_FUND_DATA_GRID_FULLTEXT_RESULT:
        case types.FUND_FUND_DATA_GRID_FULLTEXT_CLEAR:
        case types.FUND_FUND_DATA_GRID_FULLTEXT_EXTENDED:
        case types.FUND_FUND_DATA_GRID_FULLTEXT_NEXT_ITEM:
        case types.FUND_FUND_DATA_GRID_FULLTEXT_PREV_ITEM:
        case types.FUND_FUND_DATA_GRID_CHANGE_CELL_FOCUS:
        case types.FUND_FUND_DATA_GRID_CHANGE_SELECTED_ROW_INDEXES:
            return true;
        default:
            return false;
    }
}

export function fundBulkModifications(
    versionId,
    descItemTypeId,
    specsIds,
    operationType,
    findText,
    replaceText,
    replaceSpecId,
    nodes,
    selectionType,
    replaceValue,
    values,
) {
    console.log(
        '#####fundBulkModifications',
        versionId,
        descItemTypeId,
        specsIds,
        operationType,
        findText,
        replaceText,
        replaceSpecId,
        nodes,
        selectionType,
    );

    return (dispatch, getState) => {
        switch (operationType) {
            case 'findAndReplace':
                return WebApi.replaceDataValues(
                    versionId,
                    descItemTypeId,
                    specsIds,
                    findText,
                    replaceText,
                    nodes,
                    selectionType,
                );
            case 'replace':
                return WebApi.placeDataValues(
                    versionId,
                    descItemTypeId,
                    specsIds,
                    replaceText,
                    replaceSpecId,
                    nodes,
                    selectionType,
                );
            case 'delete':
                return WebApi.deleteDataValues(versionId, descItemTypeId, specsIds, nodes, selectionType, values);
            case 'setSpecification':
                return WebApi.setSpecification(
                    versionId,
                    descItemTypeId,
                    specsIds,
                    replaceSpecId,
                    nodes,
                    selectionType,
                );
            case 'setValue':
                return WebApi.setDataValues(
                    versionId,
                    descItemTypeId,
                    specsIds,
                    replaceValue,
                    nodes,
                    selectionType,
                    values
                );
            default:
                console.warn('#####fundBulkModifications - operation not implemented', operationType);
                return null;
        }
    };
}

export function fundDataInitIfNeeded(versionId, initData) {
    return (dispatch, getState) => {
        const fundDataGrid = getFundDataGrid(getState, versionId);
        if (fundDataGrid && !fundDataGrid.initialised) {
            dispatch({
                type: types.FUND_FUND_DATA_GRID_INIT,
                versionId,
                initData,
            });
        }
    };
}

export function fundDataFulltextSearch(versionId, filterText, luceneQuery, searchParams, data) {
    return (dispatch, getState) => {
        return WebApi.getFilteredFulltextNodes(versionId, filterText, luceneQuery, searchParams).then(json => {
            dispatch(fundDataFulltextSearchResult(versionId, filterText, luceneQuery, json, data));
        });
    };
}

function fundDataFulltextSearchResult(versionId, filterText, luceneQuery, searchedItems, data) {
    return {
        type: types.FUND_FUND_DATA_GRID_FULLTEXT_RESULT,
        versionId,
        filterText,
        luceneQuery,
        searchedItems,
        data,
    };
}

export function fundDataChangeCellFocus(versionId, row, col) {
    return {
        type: types.FUND_FUND_DATA_GRID_CHANGE_CELL_FOCUS,
        versionId,
        row,
        col,
    };
}

export function fundDataChangeRowIndexes(versionId, indexes) {
    return {
        type: types.FUND_FUND_DATA_GRID_CHANGE_SELECTED_ROW_INDEXES,
        versionId,
        indexes,
    };
}

export function fundDataFulltextExtended(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FULLTEXT_EXTENDED,
        versionId,
    };
}

export function fundDataFulltextClear(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FULLTEXT_CLEAR,
        versionId,
    };
}

export function fundDataFulltextPrevItem(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FULLTEXT_PREV_ITEM,
        versionId,
    };
}

export function fundDataFulltextNextItem(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FULLTEXT_NEXT_ITEM,
        versionId,
    };
}

function getFundDataGrid(getState, versionId) {
    const state = getState();
    const fund = objectById(state.arrRegion.funds, versionId, 'versionId');
    if (!fund) {
        return null;
    }

    const fundDataGrid = fund.fundDataGrid;
    return fundDataGrid;
}

export function fundDataGridFetchFilterIfNeeded(versionId) {
    return (dispatch, getState) => {
        const fundDataGrid = getFundDataGrid(getState, versionId);
        if (fundDataGrid && !fundDataGrid.fetchedFilter && !fundDataGrid.isFetchingFilter) {
            dispatch(fundDataGridFilter(versionId, fundDataGrid.filter));
        }
    };
}

function _fundDataGridKey(state) {
    var str = '';
    str += '-pg' + state.pageSize;
    str += '-pi' + state.pageIndex;
    str += '-vc';
    Object.keys(state.visibleColumns)
        .sort()
        .forEach(k => {
            str += '-' + k;
        });
    str += '-fi';
    Object.keys(state.filter)
        .sort()
        .forEach(k => {
            str += '-' + k;
            str += JSON.stringify(state.filter[k]);
        });
    return str;
}

export function fundDataGridFilterClearAll(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FILTER_CLEAR_ALL,
        versionId,
    };
}

export function fundDataGridPrepareEdit(versionId, nodeId, parentNodeId, descItemTypeId) {
    return {
        type: types.FUND_FUND_DATA_GRID_PREPARE_EDIT,
        versionId,
        nodeId,
        parentNodeId,
        descItemTypeId,
    };
}

/**
 * Aktualizace řádek v tabulce - zachová aktuální stránku, ale znovu přefiltruje data.
 * @param versionId verze AS
 */
export function fundDataGridRefreshRows(versionId) {
    return (dispatch, getState) => {
        const fundDataGrid = getFundDataGrid(getState, versionId);
        if (!fundDataGrid) {
            return;
        }

        dispatch(_fundDataGridFilter(versionId, fundDataGrid.filter, false));
    };
}

export function fundDataGridFilterChange(versionId, descItemTypeId, filter) {
    return (dispatch, getState) => {
        dispatch({
            type: types.FUND_FUND_DATA_GRID_FILTER_CHANGE,
            versionId,
            descItemTypeId,
            filter,
        });
    };
}

export function fundDataGridFetchDataIfNeeded(versionId, pageIndex, pageSize) {
    return (dispatch, getState) => {
        const fundDataGrid = getFundDataGrid(getState, versionId);
        if (!fundDataGrid) {
            return;
        }

        if (!fundDataGrid.fetchedFilter || fundDataGrid.isFetchingFilter) {
            // již musí být načtený filtr
            return;
        }

        const dataKey = _fundDataGridKey(fundDataGrid);
        if (fundDataGrid.currentDataKey !== dataKey) {
            dispatch(_dataRequest(versionId, dataKey));

            WebApi.getFilteredNodes(versionId, pageIndex, pageSize, Object.keys(fundDataGrid.visibleColumns)).then(
                nodes => {
                    const newState = getState();
                    const newFund = objectById(newState.arrRegion.funds, versionId, 'versionId');
                    if (newFund !== null) {
                        const newFundDataGrid = newFund.fundDataGrid;
                        const newDataKey = _fundDataGridKey(newFundDataGrid);

                        if (newDataKey === dataKey) {
                            // ještě je pořád v tom stavu, pro jaký se načítala data
                            var items = nodes.map(node => {
                                const {valuesMap, ...nodeRest} = node;
                                return {
                                    id: nodeRest.node.id,
                                    ...nodeRest,
                                    ...node.valuesMap,
                                    referenceMark: node.referenceMark,
                                };
                            });
                            dispatch(_dataReceive(versionId, items));
                        }
                    }
                },
            );
        }
    };
}

/**
 * Nastavení velikosti stránky.
 */
export function fundDataGridSetPageSize(versionId, pageSize) {
    return (dispatch, getState) => {
        dispatch(_setPageSize(versionId, pageSize));
    };
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
    };
}

/**
 * Nastavení aktuální stránky.
 */
export function fundDataGridSetPageIndex(versionId, pageIndex) {
    return (dispatch, getState) => {
        dispatch(_setPageIndex(versionId, pageIndex));
    };
}

/**
 * Nastavení velikosti stránky.
 */
function _setPageSize(versionId, pageSize) {
    return {
        type: types.FUND_FUND_DATA_GRID_PAGE_SIZE,
        versionId,
        pageSize,
    };
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
    };
}

/**
 * Nastavení označených sloupečků.
 */
export function fundDataGridSetSelection(versionId, ids) {
    return {
        type: types.FUND_FUND_DATA_GRID_SELECTION,
        versionId,
        ids,
    };
}

/**
 * Nastavení velikosti stránky.
 */
function _setPageIndex(versionId, pageIndex) {
    return {
        type: types.FUND_FUND_DATA_GRID_PAGE_INDEX,
        versionId,
        pageIndex,
    };
}

/**
 * Filtrování dat podle předaného filtru.
 */
export function fundDataGridFilter(versionId, filter) {
    return _fundDataGridFilter(versionId, filter, true);
}

export function createFilterStructure(filter) {
    const callFilter = {columnFilters: {filters: {}}};

    // Ladění objektu filtru pro server
    Object.keys(filter).forEach(key => {
        if (key === COL_REFERENCE_MARK) {
            callFilter.nodeId = filter[COL_REFERENCE_MARK].nodeId;
        } else {
            callFilter.columnFilters.filters[key] = {...filter[key]};
            const filterData = callFilter.columnFilters.filters[key];

            // Typy selected a unselected na velká písmena
            if (typeof filterData.specsType !== 'undefined' && filterData.specsType !== null) {
                filterData.specsType = filterData.specsType.toUpperCase();
            }
            if (typeof filterData.valuesType !== 'undefined' && filterData.valuesType !== null) {
                filterData.valuesType = filterData.valuesType.toUpperCase();
            }

            // Hodnoty null na reálné null
            if (filterData.values) {
                filterData.values = filterData.values.map(v => {
                    return v === FILTER_NULL_VALUE ? null : v;
                });
            }
            if (filterData.specs) {
                filterData.specs = filterData.specs.map(v => {
                    return v === FILTER_NULL_VALUE ? null : Number(v);
                });
            }
        }
    });
    return callFilter;
}

function _fundDataGridFilter(versionId, filter, resetViewState = true) {
    return (dispatch, getState) => {
        dispatch(_filterRequest(versionId));
        const callFilter = createFilterStructure(filter);

        WebApi.filterNodes(versionId, callFilter).then(json => {
            dispatch(_filterReceive(versionId, json, resetViewState));
        });
    };
}

/**
 * Byl volán request na filtrování dat.
 */
function _filterRequest(versionId) {
    return {
        type: types.FUND_FUND_DATA_GRID_FILTER_REQUEST,
        versionId,
    };
}

/**
 * Byla vyfiltrována data.
 */
function _filterReceive(versionId, itemsCount, resetViewState) {
    return {
        type: types.FUND_FUND_DATA_GRID_FILTER_RECEIVE,
        versionId,
        itemsCount,
        resetViewState,
    };
}

/**
 * Byl volán request na data.
 */
function _dataRequest(versionId, dataKey) {
    return {
        type: types.FUND_FUND_DATA_GRID_DATA_REQUEST,
        versionId,
        dataKey,
    };
}

/**
 * Byla vrácena data.
 */
function _dataReceive(versionId, items) {
    return {
        type: types.FUND_FUND_DATA_GRID_DATA_RECEIVE,
        versionId,
        items,
    };
}
