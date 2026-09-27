import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AipDetailVO, AipLevelType, AipProblemType } from 'elza-api';

import { WebApi } from 'actions/WebApi';
import { DaoFileFolderVO } from 'api/DaoFileFolderVO';
import { fireEvent, renderWithProviders, screen } from 'test/test-utils';
import { Route } from 'react-router-dom';
import { AipExplorerTabs } from './AipExplorerTabs';

/**
 * The explorer opens on the package as a whole; the structure hides the synthetic root so the
 * sections of the package are the top level; the problem leads straight to the file it is about.
 */

// The browser of the package files has its own tests; here only the props it is given matter.
vi.mock('./PackageBrowser', () => ({
    default: ({aipId, selectPath}: {aipId: number; selectPath?: string}) =>
        <div data-testid="package-browser">{`${aipId}:${selectPath ?? ''}`}</div>,
}));

// The connection has its own tests; here only whether and when it is offered.
vi.mock('components/arr/aip/assignment/AipConnectPanel', () => ({
    default: ({aipId}: {aipId: number}) => <div data-testid="connect-panel">{aipId}</div>,
}));

const detail = {
    aipId: 11,
    code: '04ccc520-c5a9-4c9f-a83f-28d91fd37aa7',
    digitalRepositoryId: 1,
    metadataLoad: true,
    problemType: AipProblemType.MetadataError,
    problemDescription: 'Popis se nepodařilo načíst.',
    problemFile: 'metadata/descriptive/pruvodka.xml',
} as AipDetailVO;

/** A node of the structure as the server sends it - the synthetic levels carry a negative daoId. */
type StructureNode = DaoFileFolderVO & { daoId: number };

const level = (uuid: string, daoId: number, label: string, levelType?: AipLevelType,
               childFolders: StructureNode[] = []): StructureNode => ({
    uuid, daoId, daoFileFolderId: daoId, label, levelType, childFiles: [], childFolders,
});

/**
 * The structure as the server builds it: a synthetic root over the three sections. The virtual
 * levels are identified by levelType and carry the Czech label only as a fallback.
 */
const structure = level('root', -3, 'Balíček', AipLevelType.Package, [
    level('rep', -1, 'Reprezentace', AipLevelType.Representations),
    level('log', -2, 'Logická struktura', AipLevelType.LogicalStructure),
    level('meta', -4, 'Metadata', AipLevelType.Metadata),
]);

beforeEach(() => {
    vi.spyOn(WebApi, 'getAip').mockResolvedValue(detail);
    vi.spyOn(WebApi, 'getDaDaoListByAipId').mockResolvedValue(structure);
});

describe('AipExplorerTabs', () => {
    it('nabídne připojení k popisu jen v archivním souboru, do kterého lze zapisovat', async () => {
        const {rerender} = renderWithProviders(<AipExplorerTabs aipId={11}/>, {route: '/aip/11/explorer?tab=connect'});

        // not connectable (e.g. the fund is still loading): the package is shown instead
        expect(screen.queryByRole('tab', {name: 'Připojení k popisu'})).toBeNull();
        expect(screen.getByRole('tab', {name: 'Balíček'})).toHaveAttribute('aria-selected', 'true');

        rerender(<AipExplorerTabs aipId={11} connectable/>);

        expect(screen.getByRole('tab', {name: 'Připojení k popisu'})).toHaveAttribute('aria-selected', 'true');
        expect(await screen.findByTestId('connect-panel')).toHaveTextContent('11');
    });

    it('otevřená karta a vybraná část jsou v adrese - na vnitřek balíčku lze odkázat', async () => {
        renderWithProviders(<>
            <AipExplorerTabs aipId={11}/>
            <Route path="*" render={({location}) => <output>{location.search}</output>}/>
        </>, {route: '/aip/11/explorer'});

        fireEvent.click(await screen.findByRole('tab', {name: 'Struktura'}));
        expect(screen.getByRole('status')).toHaveTextContent('?tab=structure');

        fireEvent.click(await screen.findByRole('treeitem', {name: /Logická struktura/}));
        expect(screen.getByRole('status')).toHaveTextContent('?tab=structure&select=log');
    });

    it('adresa s vybranou částí ji otevře a nepřepíše ji kořenem', async () => {
        renderWithProviders(<>
            <AipExplorerTabs aipId={11}/>
            <Route path="*" render={({location}) => <output>{location.search}</output>}/>
        </>, {route: '/aip/11/explorer?tab=structure&select=log'});

        expect(await screen.findByRole('treeitem', {name: /Logická struktura/})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Struktura'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('status')).toHaveTextContent('?tab=structure&select=log');
    });

    it('otevře se na balíčku, záložky jsou Balíček, Struktura a Soubory', async () => {
        renderWithProviders(<AipExplorerTabs aipId={11}/>);

        expect(await screen.findByText('04ccc520-c5a9-4c9f-a83f-28d91fd37aa7')).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Balíček'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('tab', {name: 'Struktura'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Soubory'})).toBeInTheDocument();
    });

    it('soubor problému otevře v souborech balíčku', async () => {
        renderWithProviders(<AipExplorerTabs aipId={11}/>);

        fireEvent.click(await screen.findByRole('button', {name: 'metadata/descriptive/pruvodka.xml'}));

        expect(screen.getByRole('tab', {name: 'Soubory'})).toHaveAttribute('aria-selected', 'true');
        // the file the problem is about is handed over to the browser of the package files
        expect(screen.getByTestId('package-browser'))
            .toHaveTextContent('11:metadata/descriptive/pruvodka.xml');
    });

    it('struktura začíná částmi balíčku, ne jeho kořenem', async () => {
        renderWithProviders(<AipExplorerTabs aipId={11}/>);

        fireEvent.click(await screen.findByRole('tab', {name: 'Struktura'}));

        expect(await screen.findByRole('treeitem', {name: /Reprezentace/})).toBeInTheDocument();
        expect(screen.getByRole('treeitem', {name: /Logická struktura/})).toBeInTheDocument();
        expect(screen.queryByRole('treeitem', {name: /Balíček/})).toBeNull();
    });

    it('název úrovně je celý na jednom řádku a i v nápovědě', async () => {
        renderWithProviders(<AipExplorerTabs aipId={11}/>);

        fireEvent.click(await screen.findByRole('tab', {name: 'Struktura'}));

        expect(await screen.findByTitle('Logická struktura')).toHaveClass('explorer-tree-label');
    });

    it('úroveň bez typu se pojmenuje popiskem ze serveru', async () => {
        // starší server typy úrovní neposílá - strom pak stojí jen na popisku
        vi.spyOn(WebApi, 'getDaDaoListByAipId').mockResolvedValue(
            level('root', -3, 'Balíček', undefined, [level('rep', -1, 'Reprezentace')]));

        renderWithProviders(<AipExplorerTabs aipId={11}/>);
        fireEvent.click(await screen.findByRole('tab', {name: 'Struktura'}));

        expect(await screen.findByRole('treeitem', {name: /Reprezentace/})).toBeInTheDocument();
    });
});
