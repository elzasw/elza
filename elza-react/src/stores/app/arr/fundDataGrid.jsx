import * as types from 'actions/constants/ActionTypes';
import {consolidateState} from 'components/Utils';
import subNodeForm from './subNodeForm';
import {nodeFormActions} from 'actions/arr/subNodeForm';
import {getMapFromList} from 'stores/app/utils';

const initialState = {
    initialised: false, // jestli byl prvotně inicializován, např. seznam zobrazovaných sloupců atp.
    isFetchingFilter: false,
    fetchedFilter: false,
    isFetchingData: false,
    fetchedData: false,
    rowsDirty: false, // zda jsou data tabulky již neaktuální - počet řádek na serveru již nemusí odpovídat řádkům na klientovi - např. byla přidána nová JP
    filterDirty: false, // pokud nastala od poskledního přefiltrování nějaká změna v hodnotách PP - výsledek filtru již nemusí odpovídat tomu, co se zobrazuje
    pageSize: 25, // aktuální velikost stránky
    pageIndex: 0, // aktuální stránka
    items: [],
    itemsCount: 0,
    filter: {}, // mapa id desc item type na filter data
    visibleColumns: {}, // seznam mapa id na boolean viditelných sloupečků   // "4", "5", "8", "9", "11", "14", "17", "38", "42", "44", "50", "53"
    columnsOrder: [], // seznam id desc item type - pořadí zobrazování sloupečků
    columnInfos: {}, // mapa id desc item type na informace o sloupečku, např. jeho šířce atp.
    selectedIds: [],
    selectedRowIndexes: [],
    currentDataKey: '',
    subNodeForm: subNodeForm(),
    nodeId: null, // id node právě editovaného řádku
    parentNodeId: null, // id parent node právě editovaného řádku
    descItemTypeId: null, // id atributu právě editovaného řádku
    searchText: '',
    searchExtended: false,
    luceneQuery: false,
    data: {type: 'FORM'},
    showFilterResult: false,
    searchedItems: [], // výsledky hledání dat
    searchedCurrentIndex: 0, // index aktuálně vybrané položky ve výsledcích hledání
    cellFocus: {row: 0, col: 0},
};
// new Array("4", "5", "8", "9", "11", "14", "17", "38", "42", "44", "50", "53").forEach(a => {
//     initialState.visibleColumns[a] = true
// });

function changeSearchedIndex(state, newIndex) {
    if (state.searchedItems.length === 0) {
        return {
            ...state,
            searchedCurrentIndex: 0,
        };
    } else {
        const info = state.searchedItems[newIndex];
        var pageIndex = state.pageIndex;
        var selectedIds = state.selectedIds;

        if (info.index < state.pageIndex * state.pageSize || info.index >= (state.pageIndex + 1) * state.pageSize) {
            // je mimo aktuálně zobrazovanou stránku
            pageIndex = Math.floor(info.index / state.pageSize);
            selectedIds = [];
        }

        const row = info.index - pageIndex * state.pageSize;

        return {
            ...state,
            selectedIds: selectedIds,
            pageIndex: pageIndex,
            searchedCurrentIndex: newIndex,
            selectedRowIndexes: [row],
            cellFocus: {row, col: state.cellFocus.col},
        };
    }
}

export default function fundDataGrid(state = initialState, action = {}) {
    if (nodeFormActions.isSubNodeFormAction(action)) {
        const result = {
            ...state,
            subNodeForm: subNodeForm(state.subNodeForm, action),
        };
        return consolidateState(state, result);
    }

    switch (action.type) {
        case types.STORE_LOAD:
            return {
                ...state,
                isFetchingFilter: false,
                fetchedFilter: false,
                isFetchingData: false,
                fetchedData: false,
                items: [],
                itemsCount: 0,
                selectedIds: [],
                selectedRowIndexes: [],
                currentDataKey: '',
                subNodeForm: subNodeForm(),
                searchedItems: [],
                searchedCurrentIndex: 0,
                cellFocus: {row: 0, col: 0},
                data: {type: 'FORM'},
            };
        case types.STORE_SAVE: {
            const {pageSize, initialised, pageIndex, filter, visibleColumns, columnsOrder, columnInfos} = state;

            return {
                initialised,
                pageSize,
                pageIndex,
                filter,
                visibleColumns,
                columnsOrder,
                columnInfos,
            };
        }
        case types.FUND_FUND_DATA_GRID_INIT: {
            let visibleColumns = {};
            let columnInfos = {};

            // Pokud visible column již jsou, nechají se, jinak se inicializují z akce, kde jsou implcitní nastavené v pravidlech
            if (state.visibleColumns && Object.keys(state.visibleColumns) > 0) {
                // je definovaný
                visibleColumns = state.visibleColumns;
            } else {
                // není definováno, vezmeme z nastavení v pravidlech
                visibleColumns = {};
                action.initData.visibleColumns.forEach(id => {
                    visibleColumns[id] = true;
                });
            }

            // Pokud je informace o sloupcích již jsou, nechají se, jinak se inicializují z akce, kde jsou nastavené v pravidlech
            if (state.columnInfos && Object.keys(state.columnInfos) > 0) {
                // je definovaný
                columnInfos = state.columnInfos;
            } else {
                // není definováno, vezmeme z nastavení v pravidlech
                columnInfos = action.initData.columnInfos || {};
            }

            // ---

            return {
                ...state,
                initialised: true,
                visibleColumns,
                columnInfos,
                currentDataKey: '',
                isFetchingFilter: false,
                fetchedFilter: false,
                isFetchingData: false,
                fetchedData: false,
            };
        }
        case types.FUND_FUND_DATA_GRID_CHANGE_SELECTED_ROW_INDEXES:
            return {
                ...state,
                selectedRowIndexes: action.indexes,
            };
        case types.FUND_FUND_DATA_GRID_CHANGE_CELL_FOCUS:
            return {
                ...state,
                cellFocus: {row: action.row, col: action.col},
            };
        case types.FUND_FUND_DATA_GRID_FULLTEXT_NEXT_ITEM:
            return changeSearchedIndex(state, state.searchedCurrentIndex + 1);
        case types.FUND_FUND_DATA_GRID_FULLTEXT_PREV_ITEM: {
            return changeSearchedIndex(state, state.searchedCurrentIndex - 1);
        }
        case types.FUND_FUND_DATA_GRID_FULLTEXT_EXTENDED:
            return {
                ...state,
                searchText: '',
                showFilterResult: false,
                searchExtended: !state.searchExtended,
            };
        case types.FUND_FUND_DATA_GRID_FULLTEXT_CLEAR:
            return {
                ...state,
                showFilterResult: false,
            };
        case types.FUND_FUND_DATA_GRID_FULLTEXT_RESULT:
            const midState = {
                ...state,
                searchedItems: action.searchedItems,
                searchText: action.filterText,
                showFilterResult: action.filterText !== '' || action.luceneQuery,
                luceneQuery: action.luceneQuery,
                data: action.data,
            };
            return changeSearchedIndex(midState, 0);
        case types.FUND_FUND_DATA_GRID_FILTER_CLEAR_ALL:
            return {
                ...state,
                filter: {},
                fetchedFilter: false,
                isFetchingFilter: false,
            };
        case types.FUND_FUND_DATA_GRID_PREPARE_EDIT:
            const result = {
                ...state,
                nodeId: action.nodeId,
                parentNodeId: action.parentNodeId,
                descItemTypeId: action.descItemTypeId,
            };

            if (action.nodeId !== state.subNodeForm.fetchingId) {
                result.subNodeForm = subNodeForm();
            }

            return result;
        case types.FUND_FUND_DATA_GRID_PAGE_SIZE:
            const isBiggerPage = action.pageIndex > state.pageIndex;
            return {
                ...state,
                pageSize: action.pageSize,
                pageIndex: 0,
                selectedIds: isBiggerPage ? state.selectedIds : [],
                selectedRowIndexes: isBiggerPage ? state.selectedRowIndexes : [],
                cellFocus: {row: 0, col: 0},
            };
        case types.FUND_FUND_DATA_GRID_COLUMNS_SETTINGS:
            return {
                ...state,
                visibleColumns: action.visibleColumns,
                columnsOrder: action.columnsOrder,
                cellFocus: {row: 0, col: 0},
            };
        case types.FUND_FUND_DATA_GRID_COLUMN_SIZE:
            let columnInfos = {...state.columnInfos};
            const info = columnInfos[action.columnId] || {};
            info.width = action.width;
            columnInfos[action.columnId] = info;
            return {
                ...state,
                columnInfos: columnInfos,
            };
        case types.FUND_FUND_DATA_GRID_SELECTION:
            return {
                ...state,
                selectedIds: [...action.ids],
            };
        case types.FUND_FUND_DATA_GRID_PAGE_INDEX:
            return {
                ...state,
                pageIndex: action.pageIndex,
                selectedIds: [],
                cellFocus: {row: 0, col: 0},
            };
        case types.FUND_FUND_DATA_GRID_FILTER_CHANGE:
            let filter = {...state.filter};

            if (action.descItemTypeId !== null) {
                // null je pro případ, kdy jen chceme aktualizovat data
                if (action.filter) {
                    filter[action.descItemTypeId] = action.filter;
                } else {
                    delete filter[action.descItemTypeId];
                }
            }

            return {
                ...state,
                filter: filter,
                fetchedFilter: false,
                cellFocus: {row: 0, col: 0},
            };
        case types.FUND_FUND_DATA_GRID_FILTER_REQUEST:
            return {
                ...state,
                isFetchingFilter: true,
            };
        case types.FUND_FUND_DATA_GRID_FILTER_RECEIVE:
            const viewInfo = {
                pageIndex: state.pageIndex,
                selectedIds: state.selectedIds,
                selectedRowIndexes: state.selectedRowIndexes,
                cellFocus: state.cellFocus,
            };

            if (action.resetViewState) {
                // reset stránkování označení atp.
                viewInfo.pageIndex = 0;
                viewInfo.selectedIds = [];
                viewInfo.selectedRowIndexes = [0];
                viewInfo.cellFocus = {row: 0, col: 0};
            } else {
                // Pokud je aktuální zobrazení stránky mimo záznamy, zobrazíme poslední stránku
                if (viewInfo.pageIndex * state.pageSize >= action.itemsCount) {
                    viewInfo.pageIndex =
                        Math.floor(action.itemsCount / state.pageSize) -
                        (action.itemsCount % state.pageSize > 0 ? 0 : 1);
                }

                // Cell focus - pokud je mimo řádek, nastavíme na poslední řádek na stránce
                const restRows = action.itemsCount - viewInfo.pageIndex * state.pageSize;
                if (viewInfo.cellFocus.row >= restRows) {
                    viewInfo.cellFocus = {row: restRows - 1, col: viewInfo.cellFocus.col};
                }
            }

            return {
                ...state,
                rowsDirty: false,
                filterDirty: false,
                isFetchingFilter: false,
                fetchedFilter: true,
                itemsCount: action.itemsCount,
                currentDataKey: '', // vynucení načtení dat!!!
                ...viewInfo,
            };
        case types.CHANGE_ADD_LEVEL:
        case types.CHANGE_DELETE_LEVEL:
        case types.CHANGE_MOVE_LEVEL: {
            return {
                ...state,
                rowsDirty: true,
            };
        }
        case types.FUND_NODE_INCREASE_VERSION:
            return {
                ...state,
                subNodeForm: subNodeForm(state.subNodeForm, action),
            };
        case types.FUND_SUBNODE_UPDATE:
        case types.CHANGE_NODES:
            return {
                ...state,
                filterDirty: true, // filtr po změně nějakého PP již nemusí odpovídat
                currentDataKey: '',
                subNodeForm: subNodeForm(state.subNodeForm, action),
            };
        case types.FUND_FUND_DATA_GRID_DATA_REQUEST:
            return {
                ...state,
                isFetchingData: true,
                currentDataKey: action.dataKey,
            };
        case types.FUND_FUND_DATA_GRID_DATA_RECEIVE:
            // Pokud vrácené záznamy neobsahují ty záznamy, které jsou aktuálně v selectedIds, je selectedIds opraveno
            const rowsMap = getMapFromList(action.items);
            const newSelectedIds = [];
            state.selectedIds.forEach(id => {
                if (rowsMap[id]) {
                    newSelectedIds.push(id);
                }
            });

            // ---
            return {
                ...state,
                isFetchingData: false,
                fetchedData: true,
                items: action.items,
                selectedIds: newSelectedIds,
            };
        default:
            return state;
    }
}
