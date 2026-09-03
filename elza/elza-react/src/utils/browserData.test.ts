import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import {
    browserDataCategoryKeys,
    clearBrowserData,
    countBrowserData,
    hasMeaningfulCount,
    visibleBrowserDataCategoryKeys,
} from './browserData';

const disableStoreSave = vi.hoisted(() => vi.fn());

vi.mock('actions/store/storeEx', () => ({ disableStoreSave }));

// jsdom in this setup does not provide localStorage - supply an in-memory replacement.
function installStorageMock() {
    if (typeof (globalThis as Record<string, unknown>).localStorage === 'undefined') {
        const store = new Map<string, string>();
        const mock: Storage = {
            getItem: (key) => (store.has(key) ? store.get(key)! : null),
            setItem: (key, value) => void store.set(key, String(value)),
            removeItem: (key) => void store.delete(key),
            clear: () => store.clear(),
            key: (index) => Array.from(store.keys())[index] ?? null,
            get length() {
                return store.size;
            },
        };
        Object.defineProperty(globalThis, 'localStorage', { value: mock, configurable: true });
    }
}

beforeAll(installStorageMock);

afterEach(() => {
    localStorage.clear();
    disableStoreSave.mockClear();
});

function fillStorage() {
    localStorage.setItem('ELZA-STORE-STATE', '{}');
    localStorage.setItem('ELZA-USER-SETTINGS', '{}');
    localStorage.setItem('theme', 'dark');
    localStorage.setItem('arrDaos.leftSize', '240');
    localStorage.setItem('apDetail-globalCollapsed', 'true');
    localStorage.setItem('ELZA-DATAGRID-LAST-HIGHLIGHT', '{}');
    localStorage.setItem('descItem-1-2-new-0-1', '"typed"');
    localStorage.setItem('descItem-1-2-3-description', '"note"');
    localStorage.setItem('unrelated-app-key', 'keep me');
}

describe('countBrowserData', () => {
    it('counts the entries of every category', () => {
        fillStorage();

        expect(countBrowserData()).toEqual({
            appState: 1,
            display: 2,
            layout: 2,
            dataGrid: 1,
            descItemDrafts: 2,
        });
    });

    it('reports zero for categories with nothing stored', () => {
        const counts = countBrowserData();

        expect(browserDataCategoryKeys.every((categoryKey) => counts[categoryKey] === 0)).toBe(true);
    });
});

describe('visibleBrowserDataCategoryKeys', () => {
    it('leaves out the hidden categories but keeps them clearable', () => {
        expect(visibleBrowserDataCategoryKeys).not.toContain('layout');
        expect(browserDataCategoryKeys).toContain('layout');
    });
});

describe('hasMeaningfulCount', () => {
    it('counts the description item drafts, whose number varies', () => {
        expect(hasMeaningfulCount('descItemDrafts')).toBe(true);
    });

    it('does not count categories holding a fixed set of keys', () => {
        const countedCategories = browserDataCategoryKeys.filter(hasMeaningfulCount);

        expect(countedCategories).toEqual(['descItemDrafts']);
    });
});

describe('clearBrowserData', () => {
    it('clears only the selected category', () => {
        fillStorage();

        clearBrowserData(['descItemDrafts']);

        expect(localStorage.getItem('descItem-1-2-new-0-1')).toBeNull();
        expect(localStorage.getItem('descItem-1-2-3-description')).toBeNull();
        expect(localStorage.getItem('ELZA-USER-SETTINGS')).toBe('{}');
        expect(localStorage.getItem('ELZA-STORE-STATE')).toBe('{}');
    });

    it('never touches entries of another application on the same origin', () => {
        fillStorage();

        clearBrowserData(browserDataCategoryKeys);

        expect(localStorage.getItem('unrelated-app-key')).toBe('keep me');
        expect(localStorage.length).toBe(1);
    });

    it('stops the periodic store save before clearing the application state', () => {
        fillStorage();

        clearBrowserData(['appState']);

        expect(disableStoreSave).toHaveBeenCalledOnce();
    });

    it('leaves the store save running when the application state is kept', () => {
        fillStorage();

        clearBrowserData(['display', 'layout']);

        expect(disableStoreSave).not.toHaveBeenCalled();
        expect(localStorage.getItem('ELZA-STORE-STATE')).toBe('{}');
    });

    it('does nothing when no category is selected', () => {
        fillStorage();

        clearBrowserData([]);

        expect(localStorage.length).toBe(9);
    });
});
