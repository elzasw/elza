import { afterEach, describe, expect, it, vi } from 'vitest';
import { DataType, NodeItem } from 'elza-api';
import { renderWithProviders } from 'test/test-utils';
import { appendItemClipboard, readItemClipboard, writeItemClipboard } from './itemClipboard';
import { useCopyItems } from './useCopyItems';

vi.mock('./itemClipboard', async (importOriginal) => ({
    ...(await importOriginal<typeof import('./itemClipboard')>()),
    writeItemClipboard: vi.fn(),
    appendItemClipboard: vi.fn(),
    readItemClipboard: vi.fn(),
}));

const NODE_ID = 100;
const source = { fundId: 1, fundVersionId: 10, sourceNodeId: NODE_ID };

interface Toast {
    style: string;
    title: string;
    message: unknown;
}

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): NodeItem {
    return { itemTypeId, data: { dataType: DataType.String, stringValue } as NodeItem['data'], ...extra };
}

const descItems = [
    stringItem(1, 'own', { nodeId: NODE_ID, itemObjectId: 50, position: 1 }),
    stringItem(1, 'inherited', { nodeId: 1, itemObjectId: 60, position: 1 }),
    stringItem(2, 'other', { nodeId: NODE_ID, itemObjectId: 70, position: 1 }),
];

function renderCopy() {
    let copyItems: ReturnType<typeof useCopyItems> | undefined;
    function Harness(): null {
        copyItems = useCopyItems();
        return null;
    }
    const { store } = renderWithProviders(<Harness />);

    function copy(params: { itemTypeId?: number; append?: boolean }) {
        copyItems?.({ descItems, nodeId: NODE_ID, fundId: 1, fundVersionId: 10, ...params });
        const toasts: Toast[] = store.getState().toastr.toasts;
        return toasts[toasts.length - 1];
    }

    return { copy };
}

describe('useCopyItems', () => {
    afterEach(() => {
        vi.mocked(writeItemClipboard).mockReset();
        vi.mocked(appendItemClipboard).mockReset();
        vi.mocked(readItemClipboard).mockReset();
    });

    it('replaces the clipboard with the own items of one type', () => {
        vi.mocked(writeItemClipboard).mockReturnValue(true);
        const { copy } = renderCopy();

        const toast = copy({ itemTypeId: 1 });

        expect(writeItemClipboard).toHaveBeenCalledWith(source, [
            { itemTypeId: 1, itemSpecId: undefined, position: 1, undefined: undefined, data: descItems[0].data },
        ]);
        expect(appendItemClipboard).not.toHaveBeenCalled();
        expect(toast.style).toBe('success');
        expect(toast.title).toMatch(/\b1\b/);
        expect(toast.message).toBeNull();
    });

    it('copies all own items of the node without a type', () => {
        vi.mocked(writeItemClipboard).mockReturnValue(true);
        const { copy } = renderCopy();

        copy({});

        expect(vi.mocked(writeItemClipboard).mock.calls[0][1].map(({ itemTypeId }) => itemTypeId)).toEqual([1, 2]);
    });

    it('appends and reports the clipboard total', () => {
        vi.mocked(appendItemClipboard).mockReturnValue(true);
        vi.mocked(readItemClipboard).mockReturnValue({ version: 1, ...source, copiedAt: 1, items: [{}, {}, {}] });
        const { copy } = renderCopy();

        const toast = copy({ itemTypeId: 1, append: true });

        expect(appendItemClipboard).toHaveBeenCalled();
        expect(writeItemClipboard).not.toHaveBeenCalled();
        expect(String(toast.message)).toMatch(/\b3\b/);
    });

    it('reports a failed write', () => {
        vi.mocked(writeItemClipboard).mockReturnValue(false);
        const { copy } = renderCopy();

        const toast = copy({ itemTypeId: 1 });

        expect(toast.style).toBe('danger');
    });
});
