import { PropsWithChildren } from 'react';
import { Provider } from 'react-redux';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { DataType, FormItemType, MandatoryType, NodeFormData, NodeItem, NodeStatus } from 'elza-api';
import { act, createTestStore, renderHook } from 'test/test-utils';
import { createLocalStorageItemKey } from '../item-form/desc-items/utils';
import { useNodeFormData } from './hooks';

const fakeWebsocket = {
    addListener: (listener: unknown) => listener,
    removeListener: (): void => undefined,
};

vi.mock('components/shared/web-socket/WebsocketProvider', () => ({
    useWebsocket: () => fakeWebsocket,
}));

// jsdom in this setup does not provide localStorage - supply an in-memory replacement.
function installStorageMock() {
    if (typeof (globalThis as Record<string, unknown>).localStorage === 'undefined') {
        const store = new Map<string, string>();
        const mock: Storage = {
            getItem: (key) => store.get(key) ?? null,
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

const FUND_VERSION_ID = 10;
const NODE_ID = 100;
const NODE_VERSION = 5;
const TITLE_TYPE_ID = 1;
const SCOPE_TYPE_ID = 2;

function itemType(itemTypeId: number): FormItemType {
    return {
        itemTypeId,
        type: MandatoryType.Possible,
        repeatable: false,
        undefinable: false,
        favoriteSpecIds: [],
    };
}

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): NodeItem {
    return {
        itemTypeId,
        nodeId: NODE_ID,
        position: 1,
        data: { dataType: DataType.String, stringValue } as NodeItem['data'],
        ...extra,
    };
}

function formData(descItems: NodeItem[]): NodeFormData {
    return {
        parent: { id: NODE_ID, version: NODE_VERSION, uuid: 'node' },
        descItems,
        itemTypes: [itemType(TITLE_TYPE_ID), itemType(SCOPE_TYPE_ID)],
        arrPerm: true,
    };
}

const savedTitle = stringItem(TITLE_TYPE_ID, 'a', { itemObjectId: 50 });
// A new seed object re-applies the seed, so it has to keep its identity across renders.
const nodeStatus: NodeStatus = { id: NODE_ID, version: NODE_VERSION };

function renderFormData() {
    const store = createTestStore({
        refTables: {
            descItemTypes: {
                itemsMap: {
                    [TITLE_TYPE_ID]: { id: TITLE_TYPE_ID, name: 'Název', dataTypeId: 1, useSpecification: false },
                    [SCOPE_TYPE_ID]: { id: SCOPE_TYPE_ID, name: 'Rozsah', dataTypeId: 1, useSpecification: false },
                },
            },
            rulDataTypes: {
                itemsMap: { 1: { id: 1, code: DataType.String } },
            },
        },
    });
    function Wrapper({ children }: PropsWithChildren) {
        return <Provider store={store}>{children}</Provider>;
    }

    const hook = renderHook(
        ({ seedFormData }: { seedFormData: NodeFormData }) =>
            useNodeFormData(FUND_VERSION_ID, NODE_ID, NODE_VERSION, {
                seedFromParent: true,
                seedFormData,
                seedNodeStatus: nodeStatus,
            }),
        { wrapper: Wrapper, initialProps: { seedFormData: formData([]) } }
    );

    function refresh(descItems: NodeItem[]) {
        hook.rerender({ seedFormData: formData(descItems) });
    }

    function addedTypeIds() {
        return hook.result.current.addedFormItems.map(({ item }) => item.itemTypeId);
    }

    return { result: hook.result, refresh, addedTypeIds };
}

describe('useNodeFormData createItemsOutsideForm', () => {
    afterEach(() => {
        localStorage.clear();
    });

    it('drops the empty added field of a type the save created an item of', async () => {
        const { result, refresh, addedTypeIds } = renderFormData();
        act(() => {
            result.current.addEmptyDescItem(TITLE_TYPE_ID);
        });

        await act(() => result.current.createItemsOutsideForm(async () => [stringItem(TITLE_TYPE_ID, 'a')]));
        refresh([savedTitle]);

        expect(result.current.formItems.map(({ item }) => item)).toEqual([savedTitle]);
        expect(addedTypeIds()).toEqual([]);
    });

    it('keeps the empty added field of a type the save did not touch', async () => {
        const { result, refresh, addedTypeIds } = renderFormData();
        act(() => {
            result.current.addEmptyDescItem(TITLE_TYPE_ID);
            result.current.addEmptyDescItem(SCOPE_TYPE_ID);
        });

        await act(() => result.current.createItemsOutsideForm(async () => [stringItem(TITLE_TYPE_ID, 'a')]));
        refresh([savedTitle]);

        expect(addedTypeIds()).toEqual([SCOPE_TYPE_ID]);
    });

    it('keeps an added field holding a value typed but not saved yet', async () => {
        const { result, refresh, addedTypeIds } = renderFormData();
        act(() => {
            result.current.addEmptyDescItem(TITLE_TYPE_ID);
        });
        localStorage.setItem(createLocalStorageItemKey(result.current.addedFormItems[0].item), JSON.stringify('b'));

        await act(() => result.current.createItemsOutsideForm(async () => [stringItem(TITLE_TYPE_ID, 'a')]));
        refresh([savedTitle]);

        expect(addedTypeIds()).toEqual([TITLE_TYPE_ID]);
    });

    it('keeps the empty added field when the refresh does not come from such a save', () => {
        const { result, refresh, addedTypeIds } = renderFormData();
        act(() => {
            result.current.addEmptyDescItem(TITLE_TYPE_ID);
        });

        refresh([savedTitle]);

        expect(addedTypeIds()).toEqual([TITLE_TYPE_ID]);
    });

    it('holds a refresh that arrives during the save until the save finishes', async () => {
        const { result, refresh, addedTypeIds } = renderFormData();
        act(() => {
            result.current.addEmptyDescItem(TITLE_TYPE_ID);
        });
        let finishSave: (createdItems: NodeItem[]) => void = () => undefined;
        const save = () => new Promise<NodeItem[]>((resolve) => (finishSave = resolve));

        let saving: Promise<void> = Promise.resolve();
        act(() => {
            saving = result.current.createItemsOutsideForm(save);
        });
        refresh([savedTitle]);
        expect(result.current.formItems).toEqual([]);

        await act(async () => {
            finishSave([stringItem(TITLE_TYPE_ID, 'a')]);
            await saving;
        });

        expect(result.current.formItems.map(({ item }) => item)).toEqual([savedTitle]);
        expect(addedTypeIds()).toEqual([]);
    });
});
