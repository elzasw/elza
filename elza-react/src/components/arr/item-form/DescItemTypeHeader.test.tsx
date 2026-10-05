import { describe, expect, it, vi } from 'vitest';
import { DescItemTypeRef, NodeSettings } from 'typings/store';
import { fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { DescItemTypeHeader, Props } from './DescItemTypeHeader';

const typeRef = { id: 1, shortcut: 'Název', dataTypeId: 1 } as DescItemTypeRef;

function renderHeader(overrides: Partial<Props> = {}) {
    const props: Props = {
        typeRef,
        typeWidth: 1,
        nodeSettings: { descItemTypeCopyIds: [] } as unknown as NodeSettings,
        handleCopyFromPrev: vi.fn(),
        canCopyFromPrev: true,
        handleCopyToggle: vi.fn(),
        handleCopyValues: vi.fn(),
        canCopyValues: true,
        handlePasteValues: vi.fn(),
        canPasteValues: false,
        ...overrides,
    };
    renderWithProviders(<DescItemTypeHeader {...props} />, {
        preloadedState: { refTables: { rulDataTypes: { itemsMap: {} } } },
    });
    return props;
}

const copyValuesName = /Kopírovat hodnoty PP pro vložení/;
const pasteValuesName = /Vložit zkopírované hodnoty PP/;

describe('DescItemTypeHeader', () => {
    it('copies on click and appends on Ctrl+click', () => {
        const { handleCopyValues } = renderHeader();
        const copyButton = screen.getByRole('button', { name: copyValuesName });

        fireEvent.click(copyButton);
        fireEvent.click(copyButton, { ctrlKey: true });

        expect(handleCopyValues).toHaveBeenNthCalledWith(1, 1, false);
        expect(handleCopyValues).toHaveBeenNthCalledWith(2, 1, true);
    });

    it('disables copying without an own value', () => {
        renderHeader({ canCopyValues: false });

        expect(screen.getByRole('button', { name: copyValuesName })).toHaveProperty('disabled', true);
    });

    it('shows the paste button only when the clipboard has values for the type', () => {
        renderHeader();
        expect(screen.queryByRole('button', { name: pasteValuesName })).toBeNull();
    });

    it('pastes into the type', () => {
        const { handlePasteValues } = renderHeader({ canPasteValues: true });

        fireEvent.click(screen.getByRole('button', { name: pasteValuesName }));

        expect(handlePasteValues).toHaveBeenCalledWith(1);
    });

    it('offers the repeated copy toggle in the menu', () => {
        const { handleCopyToggle } = renderHeader();

        // Without an active toggle, the menu trigger is the last button of the header
        const buttons = screen.getAllByRole('button');
        fireEvent.click(buttons[buttons.length - 1]);
        fireEvent.click(screen.getByRole('menuitem', { name: /opakovaného kopírování/ }));

        expect(handleCopyToggle).toHaveBeenCalledWith(1);
    });

    it('shows an active toggle on the header, where a click turns it off', () => {
        const { handleCopyToggle } = renderHeader({
            nodeSettings: { descItemTypeCopyIds: [1] } as unknown as NodeSettings,
        });

        fireEvent.click(screen.getByRole('button', { name: /opakovaného kopírování/ }));

        expect(handleCopyToggle).toHaveBeenCalledWith(1);
    });

    it('hides the toggle status while the toggle is off', () => {
        renderHeader();

        expect(screen.queryByRole('button', { name: /opakovaného kopírování/ })).toBeNull();
    });
});
