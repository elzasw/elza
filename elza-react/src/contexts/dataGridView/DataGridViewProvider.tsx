import { createContext, ReactNode, useCallback, useEffect, useRef, useState } from 'react';
import { CellFocus, DataGridViewContextValue, DataGridViewState, DATA_GRID_VIEW_DEFAULT } from './types';

export const DataGridViewContext = createContext<DataGridViewContextValue | null>(null);

// Full per-fund view state is kept in module-level memory: it survives navigating between funds
// and to/from other screens within the same page load, but is wiped on a page reload (F5).
const viewCache = new Map<number, DataGridViewState>();

// Test-only: reset the in-memory cache between tests (simulates a page reload).
export function __resetViewCacheForTests(): void {
    viewCache.clear();
}

// Only the highlighted node of the last-visited fund persists across reloads in localStorage.
const LAST_HIGHLIGHT_KEY = 'ELZA-DATAGRID-LAST-HIGHLIGHT';

interface LastHighlight {
    versionId: number;
    nodeId: number;
    // Attribute type of the highlighted cell's column; resolved to a column index at restore time.
    // Absent for the reference-mark column. Stored as a type id (not a column index) so it survives
    // the user hiding or reordering columns between sessions.
    descItemTypeId?: number;
}

function loadLastHighlight(): LastHighlight | null {
    try {
        const raw = localStorage.getItem(LAST_HIGHLIGHT_KEY);
        if (raw) {
            return JSON.parse(raw);
        }
    } catch {
        // corrupted storage content - ignore
    }
    return null;
}

interface LoadedView {
    view: DataGridViewState;
    // True only when the highlight came from localStorage (after a reload), so a page restore is needed.
    restorePending: boolean;
}

function loadView(versionId: number): LoadedView {
    // In-memory state (same page load) wins; its restore, if any, already ran.
    const cached = viewCache.get(versionId);
    if (cached) {
        return { view: cached, restorePending: false };
    }
    // After a reload the cache is empty; restore only the last-visited fund's highlight, if it matches.
    const lastHighlight = loadLastHighlight();
    if (lastHighlight && lastHighlight.versionId === versionId) {
        const view = {
            ...DATA_GRID_VIEW_DEFAULT,
            restoreNodeId: lastHighlight.nodeId,
            restoreDescItemTypeId: lastHighlight.descItemTypeId,
        };
        return { view, restorePending: true };
    }
    return { view: DATA_GRID_VIEW_DEFAULT, restorePending: false };
}

interface Props {
    versionId: number;
    children: ReactNode;
}

export function DataGridViewProvider({ versionId, children }: Props) {
    const initial = useRef(loadView(versionId));
    const [view, setView] = useState<DataGridViewState>(() => initial.current.view);
    // Restore is needed when we arrived from a stored highlight (after a reload); the data fetch completes it.
    const [restorePending, setRestorePending] = useState<boolean>(() => initial.current.restorePending);

    // When switching to another version (another fund), reload its view.
    const loadedVersionRef = useRef(versionId);
    useEffect(() => {
        if (loadedVersionRef.current !== versionId) {
            loadedVersionRef.current = versionId;
            const loaded = loadView(versionId);
            setView(loaded.view);
            setRestorePending(loaded.restorePending);
        }
    }, [versionId]);

    useEffect(() => {
        viewCache.set(versionId, view);
    }, [versionId, view]);

    // Selection is page-scoped: changing the page clears the checked rows and the focus.
    const setPageIndex = useCallback((pageIndex: number) => {
        setView(prev => ({ ...prev, pageIndex, selectedIds: [], selectedRowIndexes: [], cellFocus: { row: 0, col: 0 } }));
    }, []);

    // Change the page without touching selection or focus - for restore, page clamp, and search-hit jumps.
    const goToPage = useCallback((pageIndex: number) => {
        setView(prev => (prev.pageIndex === pageIndex ? prev : { ...prev, pageIndex }));
    }, []);

    const setPageSize = useCallback((pageSize: number) => {
        setView(prev => ({
            ...prev,
            pageSize,
            pageIndex: 0,
            selectedIds: [],
            selectedRowIndexes: [],
            cellFocus: { row: 0, col: 0 },
        }));
    }, []);

    const setSelectedIds = useCallback((selectedIds: Array<string | number>) => {
        setView(prev => ({ ...prev, selectedIds }));
    }, []);

    const setSelectedRowIndexes = useCallback((selectedRowIndexes: number[]) => {
        setView(prev => ({ ...prev, selectedRowIndexes }));
    }, []);

    const setCellFocus = useCallback((cellFocus: CellFocus) => {
        setView(prev => ({ ...prev, cellFocus }));
    }, []);

    const rememberRestoreNode = useCallback((nodeId: number | undefined, descItemTypeId?: number) => {
        setView(prev => ({ ...prev, restoreNodeId: nodeId }));
        // Persist the single cross-reload highlight (node + attribute type) for the last-visited fund.
        if (nodeId != null) {
            localStorage.setItem(LAST_HIGHLIGHT_KEY, JSON.stringify({ versionId, nodeId, descItemTypeId }));
        } else {
            const last = loadLastHighlight();
            if (last && last.versionId === versionId) {
                localStorage.removeItem(LAST_HIGHLIGHT_KEY);
            }
        }
    }, [versionId]);

    const markRestoreDone = useCallback(() => {
        setRestorePending(false);
    }, []);

    // Force a restore of a specific cell, overriding the in-memory view (used by the "open in datagrid"
    // deep link). Clears the prior selection and focus up front so nothing stale shows before the target
    // cell resolves, and seeds the target column so the row-focus step lands on the right cell.
    const requestRestore = useCallback((nodeId: number, col: number) => {
        setView(prev => ({
            ...prev,
            restoreNodeId: nodeId,
            restoreDescItemTypeId: undefined,
            selectedIds: [],
            selectedRowIndexes: [],
            cellFocus: { row: 0, col },
        }));
        setRestorePending(true);
    }, []);

    // Records the column index the component resolved from restoreDescItemTypeId, then clears the pending type.
    const resolveRestoreColumn = useCallback((col: number) => {
        setView(prev => ({ ...prev, cellFocus: { ...prev.cellFocus, col }, restoreDescItemTypeId: undefined }));
    }, []);

    const value: DataGridViewContextValue = {
        view,
        setPageIndex,
        goToPage,
        setPageSize,
        setSelectedIds,
        setSelectedRowIndexes,
        setCellFocus,
        rememberRestoreNode,
        requestRestore,
        resolveRestoreColumn,
        restorePending,
        markRestoreDone,
    };

    return <DataGridViewContext.Provider value={value}>{children}</DataGridViewContext.Provider>;
}
