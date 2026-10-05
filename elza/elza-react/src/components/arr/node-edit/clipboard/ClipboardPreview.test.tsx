import { describe, expect, it, vi } from 'vitest';
import { DataType, NodeItem } from 'elza-api';
import { fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { ClipboardItem } from './itemClipboard';
import { ClipboardPreview } from './ClipboardPreview';

interface TypeRefFixture {
    id: number;
    shortcut: string;
    viewOrder: number;
    descItemSpecs: { id: number; name: string; shortcut: string }[];
}

const itemTypeRefs: Record<number, TypeRefFixture> = {
    1: { id: 1, shortcut: 'Název', viewOrder: 20, descItemSpecs: [] },
    2: { id: 2, shortcut: 'Obsah', viewOrder: 10, descItemSpecs: [] },
    3: { id: 3, shortcut: 'Poznámka', viewOrder: 30, descItemSpecs: [{ id: 7, name: 'Interní', shortcut: 'Int.' }] },
    4: { id: 4, shortcut: 'Soubor', viewOrder: 40, descItemSpecs: [] },
    5: { id: 5, shortcut: 'Tabulka', viewOrder: 50, descItemSpecs: [] },
};

const preloadedState = { refTables: { descItemTypes: { itemsMap: itemTypeRefs } } };

function stringItem(itemTypeId: number, stringValue: string, extra: Partial<NodeItem> = {}): ClipboardItem {
    return { itemTypeId, data: { dataType: DataType.String, stringValue } as NodeItem['data'], ...extra };
}

function renderPreview(items: ClipboardItem[], isOtherFund = false) {
    const onRemoveItem = vi.fn();
    const onClear = vi.fn();
    renderWithProviders(
        <ClipboardPreview
            title="Vložit"
            items={items}
            isOtherFund={isOtherFund}
            onRemoveItem={onRemoveItem}
            onClear={onClear}
        />,
        { preloadedState },
    );
    return { onRemoveItem, onClear };
}

describe('ClipboardPreview', () => {
    it('groups values by type in the form order', () => {
        renderPreview([stringItem(1, 'První'), stringItem(2, 'Popis'), stringItem(1, 'Druhý')]);

        const labels = screen.getAllByText(/:$/).map((element) => element.textContent);
        expect(labels).toEqual(['Obsah:', 'Název:']);
        expect(screen.getByText('První')).toBeTruthy();
        expect(screen.getByText('Druhý')).toBeTruthy();
    });

    it('prefixes values with their spec and shows undefined items as an exception', () => {
        renderPreview([stringItem(3, 'Text', { itemSpecId: 7 }), { itemTypeId: 1, undefined: true }]);

        expect(screen.getByText('Int.:')).toBeTruthy();
        expect(screen.getByText('Výjimka')).toBeTruthy();
    });

    it('summarizes a JSON table by its row count', () => {
        const table: ClipboardItem = {
            itemTypeId: 5,
            data: { dataType: DataType.JsonTable, value: JSON.stringify({ rows: [{}, {}, {}] }) } as NodeItem['data'],
        };

        renderPreview([table]);

        expect(screen.getByText(/Tabulka.*\b3\b/)).toBeTruthy();
    });

    it('removes an item by its index in the clipboard, not in its group', () => {
        const { onRemoveItem } = renderPreview([stringItem(1, 'První'), stringItem(2, 'Popis'), stringItem(1, 'Druhý')]);

        // Rendered order: Obsah (index 1), then Název (indexes 0 and 2)
        const removeButtons = screen.getAllByRole('button', { name: 'Odebrat ze zkopírovaných' });
        fireEvent.click(removeButtons[2]);

        expect(onRemoveItem).toHaveBeenCalledWith(2);
    });

    it('clears the whole clipboard', () => {
        const { onClear } = renderPreview([stringItem(1, 'První')]);

        fireEvent.click(screen.getByRole('button', { name: 'Vymazat vše' }));

        expect(onClear).toHaveBeenCalled();
    });

    it('shows a file reference from another fund without loading it', () => {
        const file: ClipboardItem = { itemTypeId: 4, data: { dataType: DataType.FileRef, fileId: 42 } as NodeItem['data'] };

        renderPreview([file], true);

        expect(screen.getByText('42')).toBeTruthy();
    });
});
