import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AipLevelType, AipLinkState, LinkedNodeVO } from 'elza-api';

import { renderWithProviders, screen, fireEvent, createTestStore, act } from 'test/test-utils';
import AipIndividualAssignmentModal from './AipIndividualAssignmentModal';

/**
 * Připojení jednoho balíčku: ve struktuře balíčku lze vybrat skutečné části (úrovně,
 * reprezentace, soubory), metadata se nenabízejí, a zvolený způsob připojení vede na
 * odpovídající volání. Způsoby, které výběr nedovoluje, jsou nedostupné.
 */

const api = vi.hoisted(() => ({
    aipBulkConnectToJp: vi.fn(),
    aipBulkCreateSublevels: vi.fn(),
    aipBulkImportDescription: vi.fn(),
}));
const webApi = vi.hoisted(() => ({
    connectAipPartToJp: vi.fn(),
    connectSelectedToJp: vi.fn(),
    createJpFromSelectedAip: vi.fn(),
}));

vi.mock('../../../../api', () => ({ Api: { aips: api } }));
vi.mock('../../../../actions', () => ({ WebApi: webApi }));

vi.mock('../../../aip/AipActionRunner', () => ({
    runAipAction: (_d: unknown, _i: unknown, _w: unknown, _t: unknown, request: () => unknown) => request(),
}));

vi.mock('components/shared/web-socket/WebsocketProvider', () => ({ useWebsocket: () => ({}) }));

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

const fundTree = { nodes: [{ id: 10, name: 'Balíčky test' }] };

const state = () => {
    const base = createTestStore().getState() as { app: Record<string, object> };
    return {
        ...base,
        app: {
            ...base.app,
            aip: { ...base.app.aip, id: 5, fetched: true,
                   data: { aipId: 5, code: 'aip-5', contentType: 'NSESSS', linkState: AipLinkState.PartiallyLinked } },
            aipStructure: { ...base.app.aipStructure, id: 5, fetched: true, data: structure },
        },
    };
};

const render = async () => {
    renderWithProviders(<AipIndividualAssignmentModal aipId={5} tree={fundTree} />, { preloadedState: state() });
    await act(async () => { await Promise.resolve(); });
};

const check = (label: string, index = 0) => fireEvent.click(screen.getAllByRole('checkbox', { name: label })[index]);
const choose = (name: RegExp) => fireEvent.click(screen.getByRole('radio', { name }));
/** Připojí a dočká se dokončení - po přímém připojení se dialog načte znovu. */
const connect = async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Připojit' }));
    await act(async () => { await Promise.resolve(); });
};

describe('AipIndividualAssignmentModal', () => {
    beforeEach(() => {
        [...Object.values(api), ...Object.values(webApi)].forEach(fn => fn.mockReset());
        Object.values(webApi).forEach(fn => fn.mockResolvedValue(undefined));
    });

    it('offers the real parts of the package, not its metadata nor the folders of a representation', async () => {
        await render();

        expect(screen.getByText('aip-5')).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'submission' })).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Organizace' })).toBeInTheDocument();
        expect(screen.getAllByRole('checkbox', { name: 'test.docx' }).length).toBe(2);
        expect(screen.queryByRole('checkbox', { name: 'komponenty' })).toBeNull();
        expect(screen.queryByText('METS.xml')).toBeNull();
    });

    it('links the whole package', async () => {
        await render();

        await connect();

        expect(api.aipBulkConnectToJp).toHaveBeenCalledWith(10, [5]);
    });

    it('links the selected parts, with or without their lower parts', async () => {
        await render();
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

        choose(/Převzít strukturu a popis/);
        await connect();
        expect(api.aipBulkImportDescription).toHaveBeenCalledWith(10, [5], false, undefined, 200);
    });

    it('offers the level modes only for a single level or nothing selected', async () => {
        await render();
        check('test.docx');

        expect(screen.getByRole('radio', { name: /Úrovně pod vybranou úrovní/ })).toBeDisabled();
        expect(screen.getByRole('radio', { name: /Převzít strukturu a popis/ })).toBeDisabled();
    });
});
