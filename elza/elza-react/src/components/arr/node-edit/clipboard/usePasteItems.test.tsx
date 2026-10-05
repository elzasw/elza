import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DataType, FormItemType, MandatoryType, NodeFormData, NodeItem } from 'elza-api';
import { WebApi } from 'actions/WebApi';
import { render, renderWithProviders, screen } from 'test/test-utils';
import { ClipboardItem, ItemClipboardPayload, readItemClipboard } from './itemClipboard';
import { usePasteItems } from './usePasteItems';

vi.mock('./itemClipboard', async (importOriginal) => ({
    ...(await importOriginal<typeof import('./itemClipboard')>()),
    readItemClipboard: vi.fn(),
}));

const FUND_ID = 1;
const FUND_VERSION_ID = 10;
const NODE_ID = 100;
const NODE_VERSION = 5;

interface Toast {
    style: string;
    title: string;
    message: React.ReactNode;
}

function itemType(itemTypeId: number, overrides: Partial<FormItemType> = {}): FormItemType {
    return {
        itemTypeId,
        type: MandatoryType.Possible,
        repeatable: true,
        undefinable: true,
        favoriteSpecIds: [],
        ...overrides,
    };
}

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): NodeItem {
    return {
        itemTypeId,
        data: { dataType: DataType.String, stringValue } as NodeItem['data'],
        ...extra,
    };
}

function setClipboard(items: ClipboardItem[], fundId = FUND_ID) {
    const payload: ItemClipboardPayload = {
        version: 1,
        fundId,
        fundVersionId: FUND_VERSION_ID,
        sourceNodeId: 99,
        copiedAt: 1,
        items,
    };
    vi.mocked(readItemClipboard).mockReturnValue(payload);
}

function renderPaste() {
    let pasteItems: ReturnType<typeof usePasteItems> | undefined;
    function Harness(): null {
        pasteItems = usePasteItems();
        return null;
    }
    const { store } = renderWithProviders(<Harness />, {
        preloadedState: {
            refTables: {
                descItemTypes: {
                    itemsMap: {
                        1: { id: 1, name: 'Název', dataTypeId: 1, useSpecification: false, descItemSpecs: [] },
                        2: { id: 2, name: 'Rozsah', dataTypeId: 1, useSpecification: false, descItemSpecs: [] },
                        3: { id: 3, name: 'Obsah', dataTypeId: 2, useSpecification: false, descItemSpecs: [] },
                    },
                },
                rulDataTypes: {
                    itemsMap: {
                        1: { id: 1, code: DataType.String },
                        2: { id: 2, code: DataType.Text },
                    },
                },
            },
        },
    });

    async function paste(formData: Pick<NodeFormData, 'descItems' | 'itemTypes'>, itemTypeId?: number) {
        await pasteItems?.({
            formData: formData as NodeFormData,
            nodeId: NODE_ID,
            nodeVersion: NODE_VERSION,
            fundId: FUND_ID,
            fundVersionId: FUND_VERSION_ID,
            itemTypeId,
        });
        const toasts: Toast[] = store.getState().toastr.toasts;
        return toasts[toasts.length - 1];
    }

    return { paste };
}

const ownItem = stringItem(1, 'a', { nodeId: NODE_ID, itemObjectId: 50, position: 1 });
const inheritedItem = stringItem(1, 'inherited', { nodeId: 1, itemObjectId: 60, position: 1 });

describe('usePasteItems', () => {
    let updateDescItems: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        updateDescItems = vi.spyOn(WebApi, 'updateDescItems').mockResolvedValue({});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('appends accepted items after the own ones and reports duplicates', async () => {
        setClipboard([stringItem(1, 'b', { position: 1 }), stringItem(1, 'a', { position: 2 }), stringItem(2, 'x')]);
        const { paste } = renderPaste();

        const toast = await paste({ descItems: [ownItem, inheritedItem], itemTypes: [itemType(1), itemType(2)] });

        expect(updateDescItems).toHaveBeenCalledWith(
            FUND_VERSION_ID,
            NODE_ID,
            NODE_VERSION,
            [stringItem(1, 'b', { position: 2 }), stringItem(2, 'x', { position: 1 })],
            [],
            [],
        );
        expect(toast.style).toBe('success');
        expect(toast.title).toMatch(/\b2\b/);
        render(<>{toast.message}</>);
        expect(screen.getByText(/\b1\b.*JP obsahuje/)).toBeTruthy();
    });

    it('pastes only the given DescItemType', async () => {
        setClipboard([stringItem(1, 'b'), stringItem(2, 'x')]);
        const { paste } = renderPaste();

        await paste({ descItems: [], itemTypes: [itemType(1), itemType(2)] }, 2);

        expect(updateDescItems).toHaveBeenCalledWith(
            FUND_VERSION_ID,
            NODE_ID,
            NODE_VERSION,
            [stringItem(2, 'x', { position: 1 })],
            [],
            [],
        );
    });

    it('remaps TEXT into a STRING type and reports the truncation under the target type', async () => {
        const longText: ClipboardItem = {
            itemTypeId: 3,
            data: { dataType: DataType.Text, textValue: 'x'.repeat(1005) } as NodeItem['data'],
        };
        setClipboard([longText]);
        const { paste } = renderPaste();

        const toast = await paste({ descItems: [], itemTypes: [itemType(1), itemType(3)] }, 1);

        expect(updateDescItems).toHaveBeenCalledWith(
            FUND_VERSION_ID,
            NODE_ID,
            NODE_VERSION,
            [stringItem(1, 'x'.repeat(1000), { position: 1 })],
            [],
            [],
        );
        expect(toast.style).toBe('warning');
        render(<>{toast.message}</>);
        expect(screen.getByText(/^Název: /)).toBeTruthy();
    });

    it('never remaps when pasting into the whole node', async () => {
        setClipboard([stringItem(1, 'a')]);
        const { paste } = renderPaste();

        await paste({ descItems: [], itemTypes: [itemType(1), itemType(2)] });

        expect(updateDescItems).toHaveBeenCalledWith(
            FUND_VERSION_ID,
            NODE_ID,
            NODE_VERSION,
            [stringItem(1, 'a', { position: 1 })],
            [],
            [],
        );
    });

    it('reports skipped items by type name in a warning', async () => {
        setClipboard([stringItem(1, 'b'), stringItem(2, 'x')]);
        const { paste } = renderPaste();

        const toast = await paste({
            descItems: [ownItem],
            itemTypes: [itemType(1, { repeatable: false }), itemType(2)],
        });

        expect(toast.style).toBe('warning');
        render(<>{toast.message}</>);
        expect(screen.getByText(/^Název: /)).toBeTruthy();
    });

    it('does not call the server when nothing is left to paste', async () => {
        setClipboard([stringItem(1, 'a')]);
        const { paste } = renderPaste();

        const toast = await paste({ descItems: [ownItem], itemTypes: [itemType(1)] });

        expect(updateDescItems).not.toHaveBeenCalled();
        expect(toast.style).toBe('success');
        expect(toast.title).toMatch(/\b0\b/);
    });

    it('shows the server error when the paste fails', async () => {
        updateDescItems.mockRejectedValue({ errorMessage: 'Verze JP neodpovídá' });
        setClipboard([stringItem(1, 'b')]);
        const { paste } = renderPaste();

        const toast = await paste({ descItems: [], itemTypes: [itemType(1)] });

        expect(toast.style).toBe('danger');
        expect(toast.message).toBe('Verze JP neodpovídá');
    });
});
