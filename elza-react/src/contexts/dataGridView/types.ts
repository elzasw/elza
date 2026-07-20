export interface CellFocus {
    row: number;
    col: number;
}

export interface DataGridViewState {
    pageIndex: number;
    pageSize: number;
    selectedIds: Array<string | number>;
    selectedRowIndexes: number[];
    cellFocus: CellFocus;
    // Node whose page is recomputed from the server when returning to the screen.
    restoreNodeId?: number;
}

export interface DataGridViewContextValue {
    view: DataGridViewState;
    setPageIndex: (pageIndex: number) => void;
    goToPage: (pageIndex: number) => void;
    setPageSize: (pageSize: number) => void;
    setSelectedIds: (selectedIds: Array<string | number>) => void;
    setSelectedRowIndexes: (selectedRowIndexes: number[]) => void;
    setCellFocus: (cellFocus: CellFocus) => void;
    // Stores the node to restore (focused row, otherwise the first checked one) and its focused column.
    rememberRestoreNode: (nodeId: number | undefined, col?: number) => void;
    // True until the page restore from restoreNodeId has completed after the screen loads.
    restorePending: boolean;
    markRestoreDone: () => void;
}

export const DATA_GRID_VIEW_DEFAULT: DataGridViewState = {
    pageIndex: 0,
    pageSize: 25,
    selectedIds: [],
    selectedRowIndexes: [],
    cellFocus: { row: 0, col: 0 },
    restoreNodeId: undefined,
};
