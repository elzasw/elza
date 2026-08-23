import { describe, it, expect, vi, beforeAll } from 'vitest';

import { renderWithProviders, screen, fireEvent } from 'test/test-utils';
import { NodeDaosForm } from './NodeDaosForm';

/**
 * Okno digitálních entit: rám je Fluent okno (CollapsibleDragWindow), ne
 * bootstrapový modál. Obsah je pro tyhle testy zastoupený atrapou — jde o rám
 * a o zavření okna, až seznam zůstane prázdný.
 */

vi.mock('./ArrDaos', () => ({
    ArrDaos: () => <div data-testid="arr-daos" />,
}));

const TITLE = 'Digitální entity pro jednotku popisu';

const fundWith = (rows: unknown[]) => ({
    arrRegion: {
        activeIndex: 0,
        funds: [
            {
                versionId: 1,
                nodeDaoList: { rows, sourceRows: rows, filteredRows: rows, fetched: true, isFetching: false },
            },
        ],
    },
});

beforeAll(() => {
    // CollapsibleDragWindow se měří přes ResizeObserver, jsdom ho nemá.
    vi.stubGlobal(
        'ResizeObserver',
        class {
            observe() { return; }
            unobserve() { return; }
            disconnect() { return; }
        },
    );
});

describe('NodeDaosForm', () => {
    it('vykreslí okno s titulkem a obsahem', () => {
        renderWithProviders(<NodeDaosForm nodeId={5} readMode onClose={vi.fn()} />, {
            preloadedState: fundWith([{ id: 1, fileCount: 1, daoLink: { id: 2 } }]),
        });

        expect(screen.getByText(TITLE)).toBeInTheDocument();
        expect(screen.getByTestId('arr-daos')).toBeInTheDocument();
        // rám už není bootstrapový modál
        expect(document.querySelector('.modal-body')).toBeNull();
    });

    it('tlačítko Zavřít zavírá okno', () => {
        const onClose = vi.fn();
        renderWithProviders(<NodeDaosForm nodeId={5} readMode onClose={onClose} />, {
            preloadedState: fundWith([{ id: 1, fileCount: 1, daoLink: { id: 2 } }]),
        });

        fireEvent.click(screen.getByRole('button', { name: 'Zavřít' }));
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('prázdný seznam okno nezavře, dokud v něm nic nebylo', () => {
        const onClose = vi.fn();
        renderWithProviders(<NodeDaosForm nodeId={5} readMode onClose={onClose} />, {
            preloadedState: fundWith([]),
        });

        expect(onClose).not.toHaveBeenCalled();
    });
});
