import { describe, it, expect, vi, beforeAll } from 'vitest';
import { AipProblemType, QueueItemState } from 'elza-api';

import { renderWithProviders, screen, fireEvent, createTestStore } from 'test/test-utils';
import AipTable from './AipTable';
import { colDef } from './columns';

/**
 * Seznam AIP: fond i instituce jsou v AIP jen odkazy, které se v ELZA nemusí podařit
 * dohledat — balíček z digitálního archivu nese kód, který v ELZA nemusí existovat.
 * Takový AIP se musí vypsat i s popisem problému, ne shodit seznam.
 */

vi.mock('./AipDetail.tsx', () => ({ default: () => <div data-testid="aip-detail" /> }));

// Načítání a filtrování seznamu by přepsalo předpřipravená data; test ověřuje vykreslení.
vi.mock('../../actions/aip/aip.ts', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../actions/aip/aip')>();
    return {
        ...actual,
        aipsFetchIfNeeded: () => ({ type: 'test/noop' }),
        aipsFilter: () => ({ type: 'test/noop' }),
    };
});

const onlyColumns = (...keys: string[]) =>
    Object.values(colDef)
        .map((def) => def.key)
        .filter((key) => !keys.includes(key));

/**
 * Area stores nesou vlastní reducer, proto se vychází z reálného výchozího stavu
 * a přepisují se jen načtená data.
 */
const storeWithRows = (rows: unknown[]) => {
    const base = createTestStore().getState() as Record<string, any>;
    return {
        ...base,
        app: {
            ...base.app,
            aipList: {
                ...base.app.aipList,
                fetched: true,
                isFetching: false,
                rows,
                count: rows.length,
                filter: { ...base.app.aipList.filter, from: 0, pageSize: 20, filters: [] },
            },
        },
    };
};

const aip = (overrides: Record<string, unknown> = {}) => ({
    aipId: 1,
    code: 'AIP-1',
    fund: { id: 7, name: 'Fond A' },
    institution: { id: 9, name: 'Instituce A' },
    ...overrides,
});

const render = (rows: unknown[]) =>
    renderWithProviders(
        <AipTable filterDisabled hiddenValues={onlyColumns('code', 'fund.name', 'problemType')} />,
        { preloadedState: storeWithRows(rows) },
    );

beforeAll(() => {
    // Fluent UI tabulka se měří přes ResizeObserver, jsdom ho nemá.
    vi.stubGlobal(
        'ResizeObserver',
        class {
            observe() { return; }
            unobserve() { return; }
            disconnect() { return; }
        },
    );
});

describe('AipTable', () => {
    it('vypíše dohledaný fond a AIP bez problému', () => {
        render([aip()]);

        expect(screen.getByText('AIP-1')).toBeInTheDocument();
        expect(screen.getByText('Fond A')).toBeInTheDocument();
        // sloupec s problémem zůstává prázdný
        expect(screen.getAllByText('-')).toHaveLength(1);
    });

    it('vypíše AIP bez dohledaného fondu i s popisem problému', () => {
        render([aip({
            fund: null,
            institution: null,
            problemType: AipProblemType.UnknownFund,
            problemDescription: "Fond '42' nebyl nalezen podle čísla fondu ani podle interního kódu.",
        })]);

        expect(screen.getByText('AIP-1')).toBeInTheDocument();
        expect(screen.getByText('Nenalezen fond')).toHaveAttribute(
            'title',
            "Fond '42' nebyl nalezen podle čísla fondu ani podle interního kódu.",
        );
        // nedohledaný fond se vypíše pomlčkou, seznam se nesmí zhroutit
        expect(screen.getAllByText('-')).toHaveLength(1);
    });

    it('tlačítko průzkumníka se nabízí jen tam, kde je kam navigovat', async () => {
        const onExplore = vi.fn();
        const { container } = renderWithProviders(
            <AipTable filterDisabled hiddenValues={onlyColumns('code')} onExplore={onExplore} />,
            { preloadedState: storeWithRows([aip()]) },
        );

        fireEvent.click(screen.getByTitle('Otevřít průzkumník'));
        expect(onExplore).toHaveBeenCalledWith(1);

        // bez callbacku se sloupec s akcí nevykreslí
        const plain = renderWithProviders(
            <AipTable filterDisabled hiddenValues={onlyColumns('code')} />,
            { preloadedState: storeWithRows([aip()]) },
        );
        expect(plain.container.querySelector('.aip-action-cell')).toBeNull();
        expect(container.querySelector('.aip-action-cell')).not.toBeNull();
    });

    it('stav importu se vypíše přeloženě, ne jako hodnota enumu', () => {
        renderWithProviders(
            <AipTable filterDisabled hiddenValues={onlyColumns('code', 'importState')} />,
            { preloadedState: storeWithRows([aip({ importState: QueueItemState.ImportOk })]) },
        );

        expect(screen.getByText('Aktualizováno/Staženo')).toBeInTheDocument();
    });

    it('nedohledaná instituce sama o sobě AIP neblokuje', () => {
        render([aip({
            institution: null,
            problemType: AipProblemType.UnknownInstitution,
            problemDescription: "Instituce s kódem 'INST-X' nebyla nalezena.",
        })]);

        expect(screen.getByText('Fond A')).toBeInTheDocument();
        expect(screen.getByText('Nenalezena instituce')).toBeInTheDocument();
    });
});
