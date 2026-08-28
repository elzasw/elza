import { describe, it, expect, vi, beforeAll } from 'vitest';

import { renderWithProviders, screen, createTestStore } from 'test/test-utils';
import AipTable from './AipTable';
import { colDef } from './columns';

/**
 * Seznam AIP: fond i instituce jsou v AIP jen odkazy, které se v ELZA nemusí podařit
 * dohledat — balíček z digitálního archivu nese kód, který v ELZA nemusí existovat.
 * Takový AIP se musí vypsat, ne shodit seznam.
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
    it('vypíše fond a instituci, když jsou dohledané', () => {
        renderWithProviders(
            <AipTable filterDisabled hiddenValues={onlyColumns('code', 'fund.name', 'institution.name')} />,
            { preloadedState: storeWithRows([aip()]) },
        );

        expect(screen.getByText('AIP-1')).toBeInTheDocument();
        expect(screen.getByText('Fond A')).toBeInTheDocument();
        expect(screen.getByText('Instituce A')).toBeInTheDocument();
    });

    it('vypíše AIP i bez dohledané instituce a fondu', () => {
        renderWithProviders(
            <AipTable filterDisabled hiddenValues={onlyColumns('code', 'fund.name', 'institution.name')} />,
            { preloadedState: storeWithRows([aip({ fund: null, institution: null })]) },
        );

        expect(screen.getByText('AIP-1')).toBeInTheDocument();
        expect(screen.getAllByText('-')).toHaveLength(2);
    });
});
