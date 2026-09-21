import { describe, expect, it } from 'vitest';
import { FsItemFilterByLinked, FsItemSortType } from 'elza-api';
import { DEFAULT_DAO_PAGE_URL_STATE, buildDaoPageUrl, parseDaoPageUrl } from './daoPageUrl';

const fileSystemState = {
    ...DEFAULT_DAO_PAGE_URL_STATE,
    tab: 'fileSystemTree' as const,
    repoId: 3,
};

describe('parseDaoPageUrl', () => {
    it('treats a missing tab segment as the default tab', () => {
        expect(parseDaoPageUrl({}, '')).toEqual(DEFAULT_DAO_PAGE_URL_STATE);
    });

    it('falls back to the default tab for an unknown slug', () => {
        expect(parseDaoPageUrl({ tab: 'nonsense' }, '').tab).toBe('unassignedPackages');
    });

    it('maps each slug to its tab', () => {
        expect(parseDaoPageUrl({ tab: 'packages' }, '').tab).toBe('packages');
        expect(parseDaoPageUrl({ tab: 'tree' }, '').tab).toBe('leftTree');
        expect(parseDaoPageUrl({ tab: 'filerepo' }, '').tab).toBe('fileSystemTree');
    });

    it('reads the file system browser state', () => {
        const state = parseDaoPageUrl(
            { tab: 'filerepo', repoId: '3' },
            '?path=photos/1968&item=scan01.tif&sort=SIZE_DESC&linked=UNLINKED&q=scan',
        );
        expect(state).toEqual({
            tab: 'fileSystemTree',
            repoId: 3,
            path: 'photos/1968',
            item: 'scan01.tif',
            sort: FsItemSortType.SizeDesc,
            linked: FsItemFilterByLinked.Unlinked,
            filter: 'scan',
        });
    });

    it('ignores the browser state under another tab', () => {
        const state = parseDaoPageUrl({ tab: 'packages', repoId: '3' }, '?path=photos&sort=SIZE_DESC');
        expect(state).toEqual({ ...DEFAULT_DAO_PAGE_URL_STATE, tab: 'packages' });
    });

    it('drops a repo id that is not a number', () => {
        expect(parseDaoPageUrl({ tab: 'filerepo', repoId: 'abc' }, '').repoId).toBeUndefined();
    });

    it('falls back to the defaults for unknown sort and filter values', () => {
        const state = parseDaoPageUrl({ tab: 'filerepo' }, '?sort=BY_COLOUR&linked=MAYBE');
        expect(state.sort).toBe(FsItemSortType.NameAsc);
        expect(state.linked).toBe(FsItemFilterByLinked.All);
    });

    it('decodes a path and an item containing url syntax', () => {
        const state = parseDaoPageUrl(
            { tab: 'filerepo', repoId: '3' },
            `?path=${encodeURIComponent('sken #1/půda')}&item=${encodeURIComponent('a?b&c.tif')}`,
        );
        expect(state.path).toBe('sken #1/půda');
        expect(state.item).toBe('a?b&c.tif');
    });
});

describe('buildDaoPageUrl', () => {
    it('leaves the default tab without a segment', () => {
        expect(buildDaoPageUrl(5, undefined, DEFAULT_DAO_PAGE_URL_STATE)).toBe('/fund/5/daos');
    });

    it('keeps the fund version when the url pins one', () => {
        expect(buildDaoPageUrl(5, 12, DEFAULT_DAO_PAGE_URL_STATE)).toBe('/fund/5/v/12/daos');
    });

    it('writes the repository as a path segment', () => {
        expect(buildDaoPageUrl(5, undefined, fileSystemState)).toBe('/fund/5/daos/filerepo/3');
    });

    it('omits the repository segment when none is chosen', () => {
        expect(buildDaoPageUrl(5, undefined, { ...fileSystemState, repoId: undefined }))
            .toBe('/fund/5/daos/filerepo');
    });

    it('omits query parameters that hold their default', () => {
        const url = buildDaoPageUrl(5, undefined, {
            ...fileSystemState,
            sort: FsItemSortType.NameAsc,
            linked: FsItemFilterByLinked.All,
            filter: '',
        });
        expect(url).toBe('/fund/5/daos/filerepo/3');
    });

    it('writes the whole browser state', () => {
        const url = buildDaoPageUrl(5, undefined, {
            ...fileSystemState,
            path: 'photos/1968',
            item: 'scan01.tif',
            sort: FsItemSortType.SizeDesc,
            linked: FsItemFilterByLinked.Unlinked,
            filter: 'scan',
        });
        expect(url).toBe('/fund/5/daos/filerepo/3?path=photos/1968&item=scan01.tif&sort=SIZE_DESC&linked=UNLINKED&q=scan');
    });

    it('drops the browser state under another tab', () => {
        const url = buildDaoPageUrl(5, undefined, { ...fileSystemState, tab: 'packages', path: 'photos' });
        expect(url).toBe('/fund/5/daos/packages');
    });

    it('survives a round trip of a path containing url syntax', () => {
        const state = { ...fileSystemState, path: 'sken #1/půda', item: 'a?b&c.tif' };
        const url = buildDaoPageUrl(5, undefined, state);
        const [, search] = url.split('?');
        expect(parseDaoPageUrl({ tab: 'filerepo', repoId: '3' }, `?${search}`)).toEqual(state);
    });
});
