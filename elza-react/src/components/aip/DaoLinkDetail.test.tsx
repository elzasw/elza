import { describe, expect, it, vi } from 'vitest';
import { DaDaoType, DaoLink } from 'elza-api';

import { renderWithProviders, screen } from 'test/test-utils';
import DaoLinkDetail from './DaoLinkDetail';

// Napojení se do store dostávají přes DetailActions; test je tam vloží rovnou, aby nemusel
// obcházet API - fetch se proto nahrazuje akcí, kterou žádný reducer nezpracuje.
vi.mock('actions/aip/aip', async (importOriginal) => ({
    ...await importOriginal<typeof import('actions/aip/aip')>(),
    daoLinksFetchIfNeeded: () => ({ type: 'test/noop' }),
}));

/**
 * Napojení jednotky popisu na digitální objekty. V uzavřené verzi je nelze měnit, ale přečíst
 * se musí dát stejně jako v režimu úprav - jinak o nich uživatel neví.
 */

const link = (overrides: Partial<DaoLink> = {}): DaoLink => ({
    daoLinkId: 302,
    daoLinkUuid: 'b5b1d2f0-0000-0000-0000-000000000001',
    aipId: 11,
    daoId: 56,
    daoType: DaDaoType.Logical,
    name: 'dokument:test digitálního dokumentu',
    ...overrides,
} as DaoLink);

const stateWith = (items: Array<DaoLink>) => ({
    app: { daoLinkList: { isFetching: false, fetched: true, data: { data: { items } } } },
});

describe('DaoLinkDetail', () => {
    it('v uzavřené verzi napojení vypíše, ale odpojit nenabídne', () => {
        renderWithProviders(<DaoLinkDetail nodeId={234324} readOnly />,
            { preloadedState: stateWith([link()]) });

        expect(screen.getByText('Napojení')).toBeInTheDocument();
        expect(screen.getByText('Úroveň inherentního popisu:')).toBeInTheDocument();
        expect(screen.getByText('dokument:test digitálního dokumentu')).toBeInTheDocument();
        expect(screen.queryByTitle('Odstranit')).toBeNull();
    });

    it('v režimu úprav lze napojení odpojit', () => {
        renderWithProviders(<DaoLinkDetail nodeId={234324} />,
            { preloadedState: stateWith([link()]) });

        expect(screen.getByTitle('Odstranit')).toBeInTheDocument();
    });

    it('počet komponent vypíše slovy, ne jako holé číslo', () => {
        renderWithProviders(<DaoLinkDetail nodeId={234324} readOnly />,
            { preloadedState: stateWith([link({ childrenCount: 7 })]) });

        expect(screen.getByText('7 komponent')).toBeInTheDocument();
        // komponenty se dají rozbalit i v uzavřené verzi - je to čtení, ne změna
        expect(screen.getByTitle('Zobrazit komponenty')).toBeInTheDocument();
    });

    it('bez napojení nezabírá v panelu místo', () => {
        const { container } = renderWithProviders(<DaoLinkDetail nodeId={234324} readOnly />,
            { preloadedState: stateWith([]) });

        expect(container).toBeEmptyDOMElement();
    });
});
