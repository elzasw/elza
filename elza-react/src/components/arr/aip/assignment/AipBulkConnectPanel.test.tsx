import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AipLevelType, AipLinkState } from 'elza-api';

import { renderWithProviders, screen, fireEvent, createTestStore, act } from 'test/test-utils';
import AipBulkConnectPanel from './AipBulkConnectPanel';

/**
 * Hromadné připojení balíčků (stránka): uživatel vidí, kolik balíčků a které připojuje, u každé úrovně
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

// stav balíčků si stránka načte sama - otevírá se jen s jejich id v adrese
const webApi = vi.hoisted(() => ({ getAip: vi.fn() }));
vi.mock('../../../../actions', () => ({ WebApi: webApi }));

// the fund tree is the legacy lazy tree; the panel only reads its selection from the store
vi.mock('../../FundTreeDaos', () => ({ default: (): null => null }));

const treeActions = vi.hoisted(() => ({ fundTreeNodeExpand: vi.fn(), fundTreeSelectNode: vi.fn() }));
vi.mock('actions/arr/fundTree', async (importOriginal) =>
    ({ ...await importOriginal<typeof import('actions/arr/fundTree')>(), ...treeActions }));

// akce se provede hned - test ověřuje, která se zvolí, ne její průběh
vi.mock('../../../aip/AipActionRunner', () => ({
    runAipAction: async (_d: unknown, _i: unknown, _w: unknown, _t: unknown, request: () => unknown, onDone?: () => void) => {
        await request();
        onDone?.();
    },
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

let aips: { aipId: number; code: string; contentType?: string; linkState?: AipLinkState }[] = [];

const allAips = [
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

const fundNodes = [{ id: 10, name: 'Balíčky test' }];

/** Stav se strukturou balíčků a stromem archivního souboru; selectedId = cíl připojení. */
const state = () => {
    const base = createTestStore().getState() as { app: Record<string, object>; arrRegion: object };
    return {
        ...base,
        arrRegion: {
            ...base.arrRegion, activeIndex: 0,
            funds: [{ id: 1, versionId: 3, fundTreeAip: { nodes: fundNodes, selectedId: 10, expandedIds: {} } }],
        },
        app: { ...base.app, aipLogicalTree: { ...base.app.aipLogicalTree, fetched: true, data: logicalTree } },
    };
};

/** Vykreslí stránku a dočká se balíčků a kontroly připojení, které si při otevření vyžádá. */
const render = async (aipIds = [1, 2, 3]) => {
    const result = renderWithProviders(<AipBulkConnectPanel aipIds={aipIds} />, { preloadedState: state() });
    await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)); });
    return result;
};

const selectLevel = (name: string) => fireEvent.click(screen.getAllByText(name)[0]);

const connect = async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Připojit' }));
    await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)); });
};

describe('AipBulkConnectPanel', () => {
    beforeEach(() => {
        aips = allAips;
        [...Object.values(api), ...Object.values(treeActions)].forEach(fn => fn.mockReset());
        Object.values(treeActions).forEach(fn => fn.mockReturnValue({ type: 'test/noop' }));
        webApi.getAip.mockReset();
        webApi.getAip.mockImplementation((id: number) => Promise.resolve(aips.find(a => a.aipId === id)));
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

    it('links whole packages - all of the page, whatever level is selected', async () => {
        await render();
        selectLevel('Organizace');

        await connect();

        expect(api.aipBulkConnectToJp).toHaveBeenCalledWith(10, [1, 2, 3]);
    });

    it('links the selected level of its packages', async () => {
        await render();
        selectLevel('Organizace');
        fireEvent.click(screen.getByRole('radio', { name: /Vybranou úroveň/ }));

        await connect();

        expect(api.aipBulkConnectLogicToJp).toHaveBeenCalledWith(10, [1, 2], 55);
    });

    it('reloads the packages after an action and opens the target when levels were created', async () => {
        await render();
        selectLevel('Organizace');
        fireEvent.click(screen.getByRole('radio', { name: /Úrovně pod vybranou úrovní/ }));
        // after the action the first two packages are fully linked
        aips = [...allAips.slice(0, 2).map(a => ({ ...a, linkState: AipLinkState.FullyLinked })), allAips[2]];

        await connect();

        expect(treeActions.fundTreeNodeExpand).toHaveBeenCalledWith('FUND_TREE_AREA_AIP', fundNodes[0]);
        expect(screen.getByText('Vybrán 1 balíček')).toBeInTheDocument();
    });

    it('creates the levels below the selected level', async () => {
        await render();
        selectLevel('Organizace');
        fireEvent.click(screen.getByRole('radio', { name: /Úrovně pod vybranou úrovní/ }));

        await connect();

        expect(api.aipBulkCreateSublevels).toHaveBeenCalledWith(10, [1, 2], 55);
    });

    it('leaves out packages that are fully linked already', async () => {
        aips = [...allAips.slice(0, 2), { aipId: 3, code: 'aip-3', linkState: AipLinkState.FullyLinked }];
        await render();

        expect(screen.getByText('Vybrány 2 balíčky')).toBeInTheDocument();
        await connect();

        expect(api.aipBulkConnectToJp).toHaveBeenCalledWith(10, [1, 2]);
    });

    it('imports the structure of whole packages when the root is selected, with the file plan option', async () => {
        await render();
        fireEvent.click(screen.getByRole('radio', { name: /Převzít strukturu a popis/ }));
        fireEvent.click(screen.getByRole('checkbox', { name: 'Spisový plán jako kořenová série' }));

        await connect();

        expect(api.aipBulkImportDescription).toHaveBeenCalledWith(10, [1, 2, 3], true, undefined);
    });
});
