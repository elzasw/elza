import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AipLevelType, AipLinkState, LinkedNodeVO } from 'elza-api';

import { renderWithProviders, screen, fireEvent, createTestStore, act } from 'test/test-utils';
import AipConnectPanel from './AipConnectPanel';

/**
 * Připojení jednoho balíčku (karta průzkumníku): ve struktuře balíčku lze vybrat skutečné části (úrovně,
 * reprezentace, soubory), metadata se nenabízejí, a zvolený způsob připojení vede na
 * odpovídající volání. Způsoby, které výběr nedovoluje, jsou nedostupné.
 */

const api = vi.hoisted(() => ({
    aipBulkConnectToJp: vi.fn(),
    aipBulkCreateSublevels: vi.fn(),
    aipBulkImportDescription: vi.fn(),
    aipConnectCheck: vi.fn(),
}));
const webApi = vi.hoisted(() => ({
    connectAipPartToJp: vi.fn(),
    connectSelectedToJp: vi.fn(),
    createJpFromSelectedAip: vi.fn(),
}));

vi.mock('../../../../api', () => ({ Api: { aips: api } }));
vi.mock('../../../../actions', () => ({ WebApi: webApi }));

vi.mock('../../../aip/AipActionRunner', () => ({
    runAipAction: async (_d: unknown, _i: unknown, _w: unknown, _t: unknown, request: () => unknown, onDone?: () => void) => {
        await request();
        onDone?.();
    },
}));

vi.mock('components/shared/web-socket/WebsocketProvider', () => ({ useWebsocket: () => ({}) }));

// the fund tree is the legacy lazy tree; the panel only reads its selection from the store
vi.mock('../../FundTreeDaos', () => ({ default: (): null => null }));

const treeActions = vi.hoisted(() => ({ fundTreeNodeExpand: vi.fn(), fundTreeSelectNode: vi.fn() }));
vi.mock('actions/arr/fundTree', async (importOriginal) =>
    ({ ...await importOriginal<typeof import('actions/arr/fundTree')>(), ...treeActions }));

vi.mock('actions/aip/aip', async (importOriginal) => {
    const actual = await importOriginal<typeof import('actions/aip/aip')>();
    return {
        ...actual,
        aipFetchIfNeeded: () => ({ type: 'test/noop' }),
        aipsFetchIfNeeded: () => ({ type: 'test/noop' }),
    };
});

vi.mock('actions/aip/exp', async (importOriginal) => {
    const actual = await importOriginal<typeof import('actions/aip/exp')>();
    return { ...actual, fetchAipStructureIfNeeded: () => ({ type: 'test/noop' }) };
});

/** Struktura balíčku, jak ji vrací server - jen to, co dialog čte. */
type FileNode = { uuid: string; daoId: number; filename: string; label: string; size: number; linkedNodes: LinkedNodeVO[] };
type FolderNode = {
    uuid: string; daoId: number; label: string; levelType?: AipLevelType;
    childFolders: FolderNode[]; childFiles: FileNode[]; linkedNodes?: LinkedNodeVO[];
};

const file: FileNode = { uuid: 'f1', daoId: 101, filename: 'test.docx', label: 'test.docx', size: 2048, linkedNodes: [] };

const structure: FolderNode = {
    uuid: 'root', daoId: -3, label: 'Balíček', levelType: AipLevelType.Package, childFiles: [],
    childFolders: [
        {
            uuid: 'reps', daoId: -1, label: 'Reprezentace', levelType: AipLevelType.Representations, childFiles: [],
            childFolders: [{
                uuid: 'rep', daoId: 100, label: 'submission', childFiles: [], linkedNodes: [],
                childFolders: [{ uuid: 'fold', daoId: 100, label: 'komponenty', childFolders: [], childFiles: [file], linkedNodes: [] }],
            }],
        },
        {
            uuid: 'logical', daoId: -2, label: 'Logická struktura', levelType: AipLevelType.LogicalStructure, childFiles: [],
            childFolders: [{
                uuid: 'lvl', daoId: 200, label: 'Organizace', childFolders: [], childFiles: [file],
                linkedNodes: [{ id: 1, nodeId: 7, name: 'Balíčky test' }],
            }],
        },
        {
            uuid: 'meta', daoId: -4, label: 'Metadata', levelType: AipLevelType.Metadata, childFolders: [],
            childFiles: [{ uuid: 'm', daoId: 300, filename: 'METS.xml', label: 'METS.xml', size: 10, linkedNodes: [] }],
        },
    ],
};

const fundNodes = [{ id: 10, name: 'Balíčky test' }];

/** Stav s balíčkem, jeho strukturou a stromem archivního souboru; selectedId = cíl připojení. */
const state = (selectedId: number | null = 10) => {
    const base = createTestStore().getState() as { app: Record<string, object>; arrRegion: object };
    return {
        ...base,
        arrRegion: {
            ...base.arrRegion, activeIndex: 0,
            funds: [{ id: 1, versionId: 3, fundTreeAip: { nodes: fundNodes, selectedId, expandedIds: {} } }],
        },
        app: {
            ...base.app,
            aip: { ...base.app.aip, id: 5, fetched: true,
                   data: { aipId: 5, code: 'aip-5', contentType: 'NSESSS', linkState: AipLinkState.PartiallyLinked } },
            aipStructure: { ...base.app.aipStructure, id: 5, fetched: true, data: structure },
        },
    };
};

const render = async (selectedId: number | null = 10) => {
    renderWithProviders(<AipConnectPanel aipId={5} />, { preloadedState: state(selectedId) });
    await act(async () => { await Promise.resolve(); });
};

const check = (label: string, index = 0) => fireEvent.click(screen.getAllByRole('checkbox', { name: label })[index]);
const choose = (name: RegExp) => fireEvent.click(screen.getByRole('radio', { name }));
/** Reprezentace jsou na začátku sbalené. */
const expandRepresentations = () => fireEvent.click(screen.getByText('Reprezentace'));
const showFiles = () => fireEvent.click(screen.getByRole('switch', { name: 'Zobrazit soubory' }));
/** Připojí a dočká se dokončení - po přímém připojení se dialog načte znovu. */
const connect = async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Připojit' }));
    await act(async () => { await Promise.resolve(); });
};

describe('AipConnectPanel', () => {
    beforeEach(() => {
        [...Object.values(api), ...Object.values(webApi), ...Object.values(treeActions)].forEach(fn => fn.mockReset());
        Object.values(treeActions).forEach(fn => fn.mockReturnValue({ type: 'test/noop' }));
        Object.values(webApi).forEach(fn => fn.mockResolvedValue(undefined));
        api.aipConnectCheck.mockResolvedValue({ data: { blocked: [] } });
    });

    it('shows levels and representations with a file summary, files only on request', async () => {
        await render();

        expect(screen.queryByRole('checkbox', { name: 'submission' })).toBeNull();
        expandRepresentations();
        expect(screen.getByRole('checkbox', { name: 'submission' })).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Organizace' })).toBeInTheDocument();
        expect(screen.queryByRole('checkbox', { name: 'test.docx' })).toBeNull();
        expect(screen.getAllByText('1 soubor').length).toBe(2);
        expect(screen.queryByText('METS.xml')).toBeNull();

        showFiles();
        expect(screen.getAllByRole('checkbox', { name: 'test.docx' }).length).toBeGreaterThanOrEqual(1);
        expect(screen.queryByRole('checkbox', { name: 'komponenty' })).toBeNull();
        expect(screen.queryByText('METS.xml')).toBeNull();
    });

    it('drops the selected files when the files are hidden again', async () => {
        await render();
        showFiles();
        check('test.docx');
        check('Organizace');
        expect(screen.getByText(/vybrány 2 části/)).toBeInTheDocument();

        showFiles();

        expect(screen.getByText(/vybrána 1 část/)).toBeInTheDocument();
    });

    it('shows a component holding one file as that file', async () => {
        const component: FolderNode = { uuid: 'comp', daoId: 210, label: 'komponenta:test.docx', childFolders: [], childFiles: [file], linkedNodes: [] };
        const doc: FolderNode = { uuid: 'doc', daoId: 201, label: 'dokument', childFolders: [component], childFiles: [], linkedNodes: [] };
        const logical: FolderNode = { ...structure.childFolders[1], childFolders: [{ ...structure.childFolders[1].childFolders[0], childFiles: [], childFolders: [doc] }] };
        const withComponent = { ...structure, childFolders: [structure.childFolders[0], logical] };
        const base = state();
        renderWithProviders(<AipConnectPanel aipId={5} />, {
            preloadedState: { ...base, app: { ...base.app, aipStructure: { ...base.app.aipStructure, data: withComponent } } },
        });
        await act(async () => { await Promise.resolve(); });

        expect(screen.getByRole('checkbox', { name: 'dokument' })).toBeInTheDocument();
        expect(screen.queryByText('komponenta:test.docx')).toBeNull();

        showFiles();
        expect(screen.queryByText('komponenta:test.docx')).toBeNull();
        check('test.docx');
        choose(/Vybrané části \(1\)/);
        await connect();
        expect(webApi.connectAipPartToJp).toHaveBeenCalledWith(10, 5, [101]);
    });

    it('tells which files are already connected, also through a higher part', async () => {
        const other: FileNode = { uuid: 'f2', daoId: 102, filename: 'other.pdf', label: 'other.pdf', size: 1024, linkedNodes: [] };
        const reps = structure.childFolders[0];
        const rep = reps.childFolders[0];
        const partial: FolderNode = {
            ...structure,
            childFolders: [
                { ...reps, childFolders: [{ ...rep, childFolders: [{ ...rep.childFolders[0], childFiles: [file, other] }] }] },
                ...structure.childFolders.slice(1),
            ],
        };
        const base = state();
        renderWithProviders(<AipConnectPanel aipId={5} />, {
            preloadedState: { ...base, app: { ...base.app, aipStructure: { ...base.app.aipStructure, data: partial } } },
        });
        await act(async () => { await Promise.resolve(); });

        // test.docx is connected with its level "Organizace", other.pdf is not
        expect(screen.getByText('(připojeno 1 z 2 souborů)')).toBeInTheDocument();
        expandRepresentations();
        expect(screen.getByText('· připojeno 1')).toBeInTheDocument();

        showFiles();
        // the level itself and its file, connected with it
        expect(screen.getAllByLabelText('Připojeno k: Balíčky test').length).toBe(2);
    });

    it('asks for a target before connecting', async () => {
        await render(null);

        expect(screen.getByText('Vyberte ve stromu jednotku popisu, ke které se připojí.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Připojit' })).toBeDisabled();
    });

    it('opens the target after creating levels under it, so the new ones are seen', async () => {
        await render();
        check('Organizace');
        choose(/Úrovně pod vybranou úrovní/);

        await connect();

        expect(treeActions.fundTreeNodeExpand).toHaveBeenCalledWith('FUND_TREE_AREA_AIP', fundNodes[0]);
    });

    it('links the whole package', async () => {
        await render();

        await connect();

        expect(api.aipBulkConnectToJp).toHaveBeenCalledWith(10, [5]);
    });

    it('asks for a new choice after an action instead of reusing it on the whole package', async () => {
        await render();
        check('Organizace');
        choose(/Úrovně pod vybranou úrovní/);
        await connect();

        // the selection is gone - the same mode would now mean the whole package
        expect(screen.getByText('Zvolte, co připojit.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Připojit' })).toBeDisabled();
    });

    it('does not offer to connect a package whole that the server would refuse', async () => {
        api.aipConnectCheck.mockResolvedValue({ data: { blocked: [{ aipId: 5, reason: 'už napojen' }] } });
        await render();

        expect(screen.getByRole('button', { name: 'Připojit' })).toBeDisabled();
        // its parts can still be connected
        check('Organizace');
        choose(/Vybrané části \(1\)/);
        expect(screen.getByRole('button', { name: 'Připojit' })).toBeEnabled();
    });

    it('links the selected parts, with or without their lower parts', async () => {
        await render();
        showFiles();
        check('test.docx');
        check('Organizace');
        choose(/Vybrané části \(2\)/);

        await connect();
        expect(webApi.connectAipPartToJp).toHaveBeenCalledWith(10, 5, [101, 200]);

        check('test.docx');
        check('Organizace');
        choose(/Vybrané části \(2\)/);
        fireEvent.click(screen.getByRole('checkbox', { name: 'bez nižších částí' }));
        await connect();
        expect(webApi.connectSelectedToJp).toHaveBeenCalledWith(10, 5, [101, 200]);
    });

    it('creates a new level for each selected part', async () => {
        await render();
        expandRepresentations();
        check('submission');
        choose(/každou do nové JP/);

        await connect();

        expect(webApi.createJpFromSelectedAip).toHaveBeenCalledWith(10, 5, [100]);
    });

    it('creates the levels below, or imports the structure below, the selected level', async () => {
        await render();
        check('Organizace');
        choose(/Úrovně pod vybranou úrovní/);
        await connect();
        expect(api.aipBulkCreateSublevels).toHaveBeenCalledWith(10, [5], undefined, 200);

        // the selection is cleared after an action
        check('Organizace');
        choose(/Převzít strukturu a popis/);
        await connect();
        expect(api.aipBulkImportDescription).toHaveBeenCalledWith(10, [5], false, undefined, 200);
    });

    it('offers the level modes only for a single level or nothing selected', async () => {
        await render();
        showFiles();
        check('test.docx');

        expect(screen.getByRole('radio', { name: /Úrovně pod vybranou úrovní/ })).toBeDisabled();
        expect(screen.getByRole('radio', { name: /Převzít strukturu a popis/ })).toBeDisabled();
    });
});
