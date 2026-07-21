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
    // Attribute type to focus after a reload; the component resolves it to a column index.
    restoreDescItemTypeId?: number;
}

export interface DataGridViewContextValue {
    view: DataGridViewState;
    setPageIndex: (pageIndex: number) => void;
    goToPage: (pageIndex: number) => void;
    setPageSize: (pageSize: number) => void;
    setSelectedIds: (selectedIds: Array<string | number>) => void;
    setSelectedRowIndexes: (selectedRowIndexes: number[]) => void;
    setCellFocus: (cellFocus: CellFocus) => void;
    // Stores the node to restore (focused row, otherwise the first checked one) and its attribute type.
    rememberRestoreNode: (nodeId: number | undefined, descItemTypeId?: number) => void;
    // Forces a restore of a specific cell, overriding the in-memory view (the "open in datagrid" deep link).
    requestRestore: (nodeId: number, col: number) => void;
    // Sets the resolved focus column and clears the pending restoreDescItemTypeId (used on reload once
    // the component has translated the stored attribute type into a column index).
    resolveRestoreColumn: (col: number) => void;
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
