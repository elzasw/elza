import { act, renderHook } from '@testing-library/react';
import { DataType, NodeItem } from 'elza-api';
import { afterEach, beforeAll, describe, expect, it } from 'vitest';
import {
    CLIPBOARD_STORAGE_KEY,
    appendItemClipboard,
    clearItemClipboard,
    createClipboardItems,
    parseClipboardPayload,
    readItemClipboard,
    removeItemClipboardItem,
    useItemClipboard,
    writeItemClipboard,
} from './itemClipboard';

const source = { fundId: 1, fundVersionId: 10, sourceNodeId: 100 };

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): NodeItem {
    return {
        itemTypeId,
        data: { dataType: DataType.String, stringValue } as NodeItem['data'],
        ...extra,
    };
}

function validPayload(overrides: Record<string, unknown> = {}) {
    return JSON.stringify({
        version: 1,
        ...source,
        copiedAt: 1,
        items: [stringItem(1, 'a')],
        ...overrides,
    });
}

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
    clearItemClipboard();
});

describe('createClipboardItems', () => {
    const descItems = [
        stringItem(1, 'own', { id: 5, itemObjectId: 50, nodeId: 100, nodeVersion: 3, position: 1, readOnly: false }),
        stringItem(1, 'inherited', { nodeId: 99, position: 1 }),
        stringItem(2, 'other type', { nodeId: 100, position: 1 }),
        { itemTypeId: 3, nodeId: 100, position: 1, undefined: true },
    ];

    it('keeps own items only and strips their identity', () => {
        const items = createClipboardItems(descItems, 100);

        expect(items).toHaveLength(3);
        expect(items[0]).toEqual({
            itemTypeId: 1,
            itemSpecId: undefined,
            position: 1,
            undefined: undefined,
            data: { dataType: DataType.String, stringValue: 'own' },
        });
    });

    it('limits items to one DescItemType', () => {
        const items = createClipboardItems(descItems, 100, 1);

        expect(items.map(({ itemTypeId }) => itemTypeId)).toEqual([1]);
    });

    it('keeps the undefined flag', () => {
        const items = createClipboardItems(descItems, 100, 3);

        expect(items[0].undefined).toBe(true);
    });
});

describe('parseClipboardPayload', () => {
    it('accepts a valid payload', () => {
        expect(parseClipboardPayload(validPayload())?.items).toHaveLength(1);
    });

    it('accepts an undefined item without data', () => {
        const payload = validPayload({ items: [{ itemTypeId: 1, undefined: true }] });

        expect(parseClipboardPayload(payload)).toBeDefined();
    });

    it.each([
        ['missing', null],
        ['not JSON', '{'],
        ['another version', validPayload({ version: 2 })],
        ['missing fund', validPayload({ fundId: undefined })],
        ['no items', validPayload({ items: [] })],
        ['item without type', validPayload({ items: [{ data: { dataType: DataType.String } }] })],
        ['unknown data type', validPayload({ items: [{ itemTypeId: 1, data: { dataType: 'FOO' } }] })],
        ['defined item without data', validPayload({ items: [{ itemTypeId: 1 }] })],
    ])('treats %s as empty', (_label, raw) => {
        expect(parseClipboardPayload(raw)).toBeUndefined();
    });
});

describe('item clipboard store', () => {
    it('writes and reads the payload', () => {
        const isWritten = writeItemClipboard(source, [stringItem(1, 'a')]);

        expect(isWritten).toBe(true);
        expect(readItemClipboard()).toMatchObject({ version: 1, ...source, items: [stringItem(1, 'a')] });
    });

    it('does not store an empty copy', () => {
        expect(writeItemClipboard(source, [])).toBe(false);
        expect(readItemClipboard()).toBeUndefined();
    });

    it('appends items of other types and replaces items of the same type', () => {
        writeItemClipboard(source, [stringItem(1, 'a'), stringItem(1, 'b'), stringItem(2, 'c')]);

        const isWritten = appendItemClipboard({ ...source, sourceNodeId: 200 }, [stringItem(1, 'x'), stringItem(3, 'y')]);

        expect(isWritten).toBe(true);
        expect(readItemClipboard()).toMatchObject({
            sourceNodeId: 200,
            items: [stringItem(2, 'c'), stringItem(1, 'x'), stringItem(3, 'y')],
        });
    });

    it('appends into an empty clipboard', () => {
        appendItemClipboard(source, [stringItem(1, 'a')]);

        expect(readItemClipboard()?.items).toEqual([stringItem(1, 'a')]);
    });

    it('replaces content of another fund instead of appending', () => {
        writeItemClipboard(source, [stringItem(2, 'c')]);

        appendItemClipboard({ ...source, fundId: 2 }, [stringItem(1, 'a')]);

        expect(readItemClipboard()).toMatchObject({ fundId: 2, items: [stringItem(1, 'a')] });
    });

    it('removes one item by its index and keeps the source', () => {
        writeItemClipboard(source, [stringItem(1, 'a'), stringItem(1, 'b')]);

        removeItemClipboardItem(0);

        expect(readItemClipboard()).toMatchObject({ ...source, items: [stringItem(1, 'b')] });
    });

    it('empties the clipboard when the last item is removed', () => {
        writeItemClipboard(source, [stringItem(1, 'a')]);

        removeItemClipboardItem(0);

        expect(readItemClipboard()).toBeUndefined();
    });

    it('updates the hook on write and clear', () => {
        const { result } = renderHook(() => useItemClipboard());
        expect(result.current).toBeUndefined();

        act(() => {
            writeItemClipboard(source, [stringItem(1, 'a')]);
        });
        expect(result.current?.sourceNodeId).toBe(100);

        act(() => {
            clearItemClipboard();
        });
        expect(result.current).toBeUndefined();
    });

    it('updates the hook when another tab writes', () => {
        const { result } = renderHook(() => useItemClipboard());

        act(() => {
            localStorage.setItem(CLIPBOARD_STORAGE_KEY, validPayload({ sourceNodeId: 200 }));
            window.dispatchEvent(new StorageEvent('storage', { key: CLIPBOARD_STORAGE_KEY }));
        });

        expect(result.current?.sourceNodeId).toBe(200);
    });
});
