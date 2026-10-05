import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { FsItemFilterByLinked, FsItemSortType } from 'elza-api';
import { DEFAULT_DAO_PAGE_URL_STATE } from './daoPageUrl';
import { loadDaoPageState, saveDaoPageState } from './daoPageStorage';

// Supply an in-memory replacement where the environment provides no localStorage.
function installStorageMock() {
    if (typeof (globalThis as Record<string, unknown>).localStorage === 'undefined') {
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
        Object.defineProperty(globalThis, 'localStorage', { value: mock, configurable: true });
    }
}

beforeAll(installStorageMock);

afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
});

const browsingState = {
    ...DEFAULT_DAO_PAGE_URL_STATE,
    tab: 'fileSystemTree' as const,
    repoId: 3,
    path: 'photos/1968',
    item: 'scan01.tif',
    sort: FsItemSortType.SizeDesc,
    linked: FsItemFilterByLinked.Unlinked,
    filter: 'scan',
};

describe('daoPageStorage', () => {
    it('reads back what it stored', () => {
        saveDaoPageState(5, browsingState);
        expect(loadDaoPageState(5)).toEqual(browsingState);
    });

    it('keeps each fund apart', () => {
        saveDaoPageState(5, browsingState);
        expect(loadDaoPageState(6)).toBeUndefined();
    });

    it('has nothing to report for a fund never visited', () => {
        expect(loadDaoPageState(5)).toBeUndefined();
    });

    it('normalizes a stored value written by an older release', () => {
        localStorage.setItem(
            'ELZA-DAO-PAGE-5',
            JSON.stringify({ tab: 'retiredTab', repoId: 'abc', sort: 'BY_COLOUR' }),
        );
        expect(loadDaoPageState(5)).toEqual(DEFAULT_DAO_PAGE_URL_STATE);
    });

    it('survives a corrupted entry', () => {
        vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        localStorage.setItem('ELZA-DAO-PAGE-5', 'not json');
        expect(loadDaoPageState(5)).toBeUndefined();
    });

    it('survives storage that refuses to be read or written', () => {
        vi.spyOn(console, 'warn').mockImplementation(() => undefined);
        // On a real Storage, assigning a method to the instance stores an item named after it
        // instead of replacing the method - the spy has to go on the prototype.
        const storage = typeof Storage !== 'undefined' && localStorage instanceof Storage
            ? Storage.prototype
            : localStorage;
        vi.spyOn(storage, 'getItem').mockImplementation(() => {
            throw new Error('access denied');
        });
        vi.spyOn(storage, 'setItem').mockImplementation(() => {
            throw new Error('quota exceeded');
        });

        expect(() => saveDaoPageState(5, browsingState)).not.toThrow();
        expect(loadDaoPageState(5)).toBeUndefined();
    });
});
