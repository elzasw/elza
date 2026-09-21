import { describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { FsItem, FsItemFilterByLinked, FsItemSortType, FsItemType, FsRepo } from 'elza-api';
import { renderWithProviders, screen, waitFor } from '../../../../test/test-utils';
import { server } from '../../../../test/mocks/server';
import { FileSystemBrowser } from './FileSystemBrowser';
import { FileSystemBrowserState } from './types';

const FUND_ID = 5;

const repos: FsRepo[] = [
    { fsRepoId: 3, name: 'Skeny', path: '/mnt/skeny', available: true },
    { fsRepoId: 7, name: 'Fotky', path: '/mnt/fotky', available: true },
];

const scan: FsItem = {
    itemType: FsItemType.File,
    name: 'scan01.tif',
    size: 1024,
    lastChange: '2026-01-02T03:04:05Z',
    links: [],
};

const defaultState: FileSystemBrowserState = {
    sort: FsItemSortType.NameAsc,
    linked: FsItemFilterByLinked.All,
    filter: '',
};

/** Sub-folders of each directory, keyed by path; the tree asks for these with filterType=FOLDER. */
const subFolders: Record<string, string[]> = {
    '': ['photos'],
    photos: ['1968'],
};

/** Records the query of every item listing, so a test can assert what was asked for. */
const mockApi = () => {
    const itemRequests: URLSearchParams[] = [];
    server.use(
        http.get(`/api/v1/fund/${FUND_ID}/fsrepos`, () => HttpResponse.json(repos)),
        http.get(`/api/v1/fund/${FUND_ID}/fsrepo/:repoId/items`, ({ request }) => {
            const url = new URL(request.url);
            itemRequests.push(url.searchParams);
            if (url.searchParams.get('filterType') === FsItemType.Folder) {
                const names = subFolders[url.searchParams.get('path') ?? ''] ?? [];
                return HttpResponse.json({
                    items: names.map((name) => ({
                        itemType: FsItemType.Folder,
                        name,
                        lastChange: scan.lastChange,
                        hasChildren: true,
                    })),
                });
            }
            return HttpResponse.json({ items: [scan] });
        }),
    );
    return itemRequests;
};

describe('FileSystemBrowser', () => {
    it('lists the directory named by its state and reports the selected item back', async () => {
        const itemRequests = mockApi();
        const onSelect = vi.fn();

        renderWithProviders(
            <FileSystemBrowser
                fundId={FUND_ID}
                onSelect={onSelect}
                onStateChange={vi.fn()}
                state={{ ...defaultState, repoId: 7, path: 'photos/1968', item: 'scan01.tif' }}
            />,
        );

        await waitFor(() => expect(onSelect).toHaveBeenCalled());

        const listing = itemRequests.find((params) => params.get('path') === 'photos/1968');
        expect(listing).toBeDefined();

        const [item, fullPath, repo] = onSelect.mock.calls[onSelect.mock.calls.length - 1];
        expect(item).toMatchObject({ name: 'scan01.tif' });
        expect(fullPath).toBe('7/photos/1968/scan01.tif');
        expect(repo).toMatchObject({ fsRepoId: 7 });
    });

    it('passes the sorting and filters from its state to the server', async () => {
        const itemRequests = mockApi();

        renderWithProviders(
            <FileSystemBrowser
                fundId={FUND_ID}
                onStateChange={vi.fn()}
                state={{
                    repoId: 3,
                    path: 'photos',
                    sort: FsItemSortType.SizeDesc,
                    linked: FsItemFilterByLinked.Unlinked,
                    filter: 'scan',
                }}
            />,
        );

        await waitFor(() => expect(itemRequests.length).toBeGreaterThan(0));

        const listing = itemRequests.find((params) => params.get('path') === 'photos');
        expect(listing?.get('sortingType')).toBe(FsItemSortType.SizeDesc);
        expect(listing?.get('filterByLink')).toBe(FsItemFilterByLinked.Unlinked);
        expect(listing?.get('fileFilter')).toBe('scan');
    });

    it('opens the ancestors of the shown directory in the tree', async () => {
        mockApi();

        renderWithProviders(
            <FileSystemBrowser
                fundId={FUND_ID}
                onStateChange={vi.fn()}
                state={{ ...defaultState, repoId: 3, path: 'photos/1968' }}
            />,
        );

        // "1968" only ever appears in the tree as a child of the expanded "photos", which
        // itself is a child of the expanded repository root — no clicking involved.
        expect(await screen.findByTitle('1968')).toBeTruthy();
    });

    it('offers the expander on a repository restored deep inside', async () => {
        mockApi();

        renderWithProviders(
            <FileSystemBrowser
                fundId={FUND_ID}
                onStateChange={vi.fn()}
                state={{ ...defaultState, repoId: 3, path: 'photos/1968' }}
            />,
        );

        // The tree has fetched and shown this repository's sub-folders, so the row that
        // holds them must offer the collapse button too.
        await screen.findByTitle('1968');
        const expander = screen.getByTitle('Skeny').querySelector('span > span');
        expect(expander).toHaveStyle({ visibility: 'visible' });
    });

    it('falls back to the first repository when the state names an unknown one', async () => {
        mockApi();
        const onStateChange = vi.fn();

        renderWithProviders(
            <FileSystemBrowser
                fundId={FUND_ID}
                onStateChange={onStateChange}
                state={{ ...defaultState, repoId: 999 }}
            />,
        );

        await waitFor(() => expect(onStateChange).toHaveBeenCalledWith({
            repoId: 3,
            path: undefined,
            item: undefined,
        }));
    });

    it('drops a selected item that the directory no longer holds', async () => {
        mockApi();
        const onStateChange = vi.fn();
        const onSelect = vi.fn();

        renderWithProviders(
            <FileSystemBrowser
                fundId={FUND_ID}
                onSelect={onSelect}
                onStateChange={onStateChange}
                state={{ ...defaultState, repoId: 3, path: 'photos', item: 'deleted.tif' }}
            />,
        );

        await waitFor(() => expect(onStateChange).toHaveBeenCalledWith({ item: undefined }));
        expect(onSelect).toHaveBeenLastCalledWith(undefined, undefined, undefined);
    });
});
