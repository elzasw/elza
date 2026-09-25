import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AipLevelType } from 'elza-api';

import { renderWithProviders, screen, fireEvent, createTestStore, act } from 'test/test-utils';
import AipAssignmentModal from './AipAssignmentModal';

/**
 * Hromadné připojení balíčků: uživatel vidí, kolik balíčků a které připojuje, u každé úrovně
 * struktury počet balíčků, a zvolený způsob připojení vede na odpovídající akci.
 */

const api = vi.hoisted(() => ({
    aipBulkConnectToJp: vi.fn(),
    aipBulkConnectLogicToJp: vi.fn(),
    aipBulkCreateSublevels: vi.fn(),
    aipBulkImportDescription: vi.fn(),
    aipConnectCheck: vi.fn(),
}));

vi.mock('../../../../api', () => ({ Api: { aips: api } }));

// akce se provede hned - test ověřuje, která se zvolí, ne její průběh
vi.mock('../../../aip/AipActionRunner', () => ({
    runAipAction: (_d: unknown, _i: unknown, _w: unknown, _t: unknown, request: () => unknown) => request(),
}));

vi.mock('components/shared/web-socket/WebsocketProvider', () => ({ useWebsocket: () => ({}) }));

vi.mock('actions/aip/aip', async (importOriginal) => {
    const actual = await importOriginal<typeof import('actions/aip/aip')>();
    return {
        ...actual,
        fetchAipLogicalTreeIfNeeded: () => ({ type: 'test/noop' }),
        aipsFetchIfNeeded: () => ({ type: 'test/noop' }),
    };
});

const aips = [
    { aipId: 1, code: 'aip-1', contentType: 'NSESSS' },
    { aipId: 2, code: 'aip-2', contentType: 'NSESSS' },
    { aipId: 3, code: 'aip-3' },
];

const logicalTree = {
    nodes: [
        { UUID: 'root', value: [1, 2, 3], levelType: AipLevelType.LogicalStructure },
        { UUID: 'group', value: [1, 2], daLeveViewId: 55, parent: 'root', name: 'Organizace' },
    ],
};

const fundTree = { nodes: [{ id: 10, name: 'Balíčky test' }] };

const state = () => {
    const base = createTestStore().getState() as { app: Record<string, object> };
    return {
        ...base,
        app: { ...base.app, aipLogicalTree: { ...base.app.aipLogicalTree, fetched: true, data: logicalTree } },
    };
};

/** Vykreslí dialog a dočká se odpovědi na kontrolu připojení, kterou si dialog při otevření vyžádá. */
const render = async () => {
    const result = renderWithProviders(<AipAssignmentModal aips={aips as never} tree={fundTree} />,
        { preloadedState: state() });
    await act(async () => { await Promise.resolve(); });
    return result;
};

const selectLevel = (name: string) => fireEvent.click(screen.getAllByText(name)[0]);

const connect = () => fireEvent.click(screen.getByRole('button', { name: 'Připojit' }));

describe('AipAssignmentModal', () => {
    beforeEach(() => {
        Object.values(api).forEach(fn => fn.mockReset());
        api.aipConnectCheck.mockResolvedValue({ data: { blocked: [] } });
    });

    it('says how many packages are selected and lists them on request', async () => {
        await render();

        expect(screen.getByText('Vybrány 3 balíčky')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Zobrazit balíčky vybrané úrovně' }));
        expect(screen.getByText('aip-1')).toBeInTheDocument();
        expect(screen.getByText('aip-3')).toBeInTheDocument();
    });

    it('shows how many packages each level of the structure stands for', async () => {
        await render();

        expect(screen.getByText('Organizace')).toBeInTheDocument();
        expect(screen.getAllByText('2').length).toBeGreaterThan(0);
    });

    it('links whole packages - all of the dialog, whatever level is selected', async () => {
        await render();
        selectLevel('Organizace');

        connect();

        expect(api.aipBulkConnectToJp).toHaveBeenCalledWith(10, [1, 2, 3]);
    });

    it('links the selected level of its packages', async () => {
        await render();
        selectLevel('Organizace');
        fireEvent.click(screen.getByRole('radio', { name: /Vybranou úroveň/ }));

        connect();

        expect(api.aipBulkConnectLogicToJp).toHaveBeenCalledWith(10, [1, 2], 55);
    });

    it('creates the levels below the selected level', async () => {
        await render();
        selectLevel('Organizace');
        fireEvent.click(screen.getByRole('radio', { name: /Úrovně pod vybranou úrovní/ }));

        connect();

        expect(api.aipBulkCreateSublevels).toHaveBeenCalledWith(10, [1, 2], 55);
    });

    it('imports the structure of whole packages when the root is selected, with the file plan option', async () => {
        await render();
        fireEvent.click(screen.getByRole('radio', { name: /Převzít strukturu a popis/ }));
        fireEvent.click(screen.getByRole('checkbox', { name: 'Spisový plán jako kořenová série' }));

        connect();

        expect(api.aipBulkImportDescription).toHaveBeenCalledWith(10, [1, 2, 3], true, undefined);
    });
});
