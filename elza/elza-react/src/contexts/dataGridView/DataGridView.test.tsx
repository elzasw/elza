import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeAll, describe, expect, it } from 'vitest';
import { DataGridViewProvider, __resetViewCacheForTests } from './DataGridViewProvider';
import { useDataGridView } from './useDataGridView';

// jsdom in this setup does not provide localStorage/sessionStorage - supply in-memory replacements.
function installStorageMock(name: 'localStorage' | 'sessionStorage') {
    if (typeof (globalThis as Record<string, unknown>)[name] === 'undefined') {
        const store = new Map<string, string>();
        const mock: Storage = {
            getItem: key => (store.has(key) ? store.get(key)! : null),
            setItem: (key, value) => void store.set(key, String(value)),
            removeItem: key => void store.delete(key),
            clear: () => store.clear(),
            key: index => Array.from(store.keys())[index] ?? null,
            get length() {
                return store.size;
            },
        };
        Object.defineProperty(globalThis, name, { value: mock, configurable: true });
    }
}

beforeAll(() => {
    installStorageMock('localStorage');
    installStorageMock('sessionStorage');
});

const VERSION_ID = 42;
const LAST_HIGHLIGHT_KEY = 'ELZA-DATAGRID-LAST-HIGHLIGHT';

function renderView(versionId = VERSION_ID) {
    return renderHook(() => useDataGridView(), {
        wrapper: ({ children }) => <DataGridViewProvider versionId={versionId}>{children}</DataGridViewProvider>,
    });
}

afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    // Clear the module-level cache so each test starts as if freshly reloaded.
    __resetViewCacheForTests();
});

describe('DataGridViewProvider', () => {
    it('keeps the full view in memory across remounts (in-app navigation)', () => {
        const first = renderView();
        act(() => first.result.current.setPageIndex(3));
        first.unmount();

        // Remounting the same fund (e.g. returning from another screen) restores the state.
        const second = renderView();
        expect(second.result.current.view.pageIndex).toBe(3);
    });

    it('purges the full view when the cache is reset (page reload)', () => {
        const first = renderView();
        act(() => first.result.current.setPageIndex(3));
        first.unmount();

        __resetViewCacheForTests();

        const second = renderView();
        expect(second.result.current.view.pageIndex).toBe(0);
    });

    it('clears selection and focus on pageSize change (selection is page-scoped)', () => {
        const { result } = renderView();

        act(() => result.current.setPageIndex(3));
        act(() => result.current.setSelectedRowIndexes([2]));
        act(() => result.current.setSelectedIds([1, 2]));
        act(() => result.current.setPageSize(50));

        expect(result.current.view.pageSize).toBe(50);
        expect(result.current.view.pageIndex).toBe(0);
        expect(result.current.view.selectedRowIndexes).toEqual([]);
        expect(result.current.view.selectedIds).toEqual([]);
    });

    it('clears selection on setPageIndex but goToPage leaves it untouched', () => {
        const { result } = renderView();

        act(() => result.current.setSelectedRowIndexes([2]));
        act(() => result.current.setSelectedIds([7]));

        // User pagination clears the page-scoped selection.
        act(() => result.current.setPageIndex(1));
        expect(result.current.view.pageIndex).toBe(1);
        expect(result.current.view.selectedRowIndexes).toEqual([]);
        expect(result.current.view.selectedIds).toEqual([]);

        // goToPage (restore/clamp/search) moves the page without clearing selection.
        act(() => result.current.setSelectedIds([9]));
        act(() => result.current.goToPage(4));
        expect(result.current.view.pageIndex).toBe(4);
        expect(result.current.view.selectedIds).toEqual([9]);
    });

    it('markRestoreDone clears the restore flag', () => {
        localStorage.setItem(LAST_HIGHLIGHT_KEY, JSON.stringify({ versionId: VERSION_ID, nodeId: 5 }));

        const { result } = renderView();
        expect(result.current.restorePending).toBe(true);

        act(() => result.current.markRestoreDone());
        expect(result.current.restorePending).toBe(false);
    });

    it('does not flag restorePending on a plain load without a stored highlight', () => {
        const { result } = renderView();
        expect(result.current.restorePending).toBe(false);
    });

    it('rememberRestoreNode writes the highlight with its column to localStorage', () => {
        const { result } = renderView();

        act(() => result.current.rememberRestoreNode(99, 3));
        expect(JSON.parse(localStorage.getItem(LAST_HIGHLIGHT_KEY)!)).toEqual({ versionId: VERSION_ID, nodeId: 99, col: 3 });

        act(() => result.current.rememberRestoreNode(undefined));
        expect(localStorage.getItem(LAST_HIGHLIGHT_KEY)).toBeNull();
    });

    it('falls back to the localStorage highlight after a reload, restoring node and column', () => {
        // Empty cache (as after a reload); localStorage holds the last-visited fund's highlight.
        localStorage.setItem(LAST_HIGHLIGHT_KEY, JSON.stringify({ versionId: VERSION_ID, nodeId: 77, col: 4 }));

        const { result } = renderView();

        expect(result.current.view.restoreNodeId).toBe(77);
        expect(result.current.view.cellFocus.col).toBe(4);
        expect(result.current.restorePending).toBe(true);
        // Only the highlight is restored, not a saved page.
        expect(result.current.view.pageIndex).toBe(0);
    });

    it('ignores the localStorage highlight when it belongs to another fund', () => {
        localStorage.setItem(LAST_HIGHLIGHT_KEY, JSON.stringify({ versionId: VERSION_ID + 1, nodeId: 77 }));

        const { result } = renderView();

        expect(result.current.view.restoreNodeId).toBeUndefined();
        expect(result.current.restorePending).toBe(false);
    });
});
