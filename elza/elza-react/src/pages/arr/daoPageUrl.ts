import { FsItemFilterByLinked, FsItemSortType } from 'elza-api';
import { urlFundDaos } from '../../constants';

export const DAO_TABS = ['unassignedPackages', 'packages', 'leftTree', 'fileSystemTree'] as const;

export type DaoTab = typeof DAO_TABS[number];

export const DEFAULT_DAO_TAB: DaoTab = 'unassignedPackages';

export const FILE_SYSTEM_DAO_TAB: DaoTab = 'fileSystemTree';

/**
 * Url segment of each tab. The default tab has no segment — "/fund/5/daos" is its
 * canonical url, which is also what the ribbon links to.
 */
const tabToSlug: Record<DaoTab, string> = {
    unassignedPackages: 'unassigned',
    packages: 'packages',
    leftTree: 'tree',
    fileSystemTree: 'filerepo',
};

const slugToTab = new Map<string, DaoTab>(DAO_TABS.map((tab) => [tabToSlug[tab], tab]));

/**
 * State of the DAO page that survives a reload and travels in a shared link.
 *
 * Everything except `tab` describes the file system browser, so it is read and
 * written only while that tab is open.
 */
export interface DaoPageUrlState {
    tab: DaoTab;
    repoId?: number;
    /** Directory shown in the file list, relative to the repository root. */
    path?: string;
    /** Name of the selected row; always a direct child of `path`. */
    item?: string;
    sort: FsItemSortType;
    linked: FsItemFilterByLinked;
    filter: string;
}

export interface DaoPageUrlParams {
    tab?: string;
    repoId?: string;
}

export const DEFAULT_DAO_PAGE_URL_STATE: DaoPageUrlState = {
    tab: DEFAULT_DAO_TAB,
    sort: FsItemSortType.NameAsc,
    linked: FsItemFilterByLinked.All,
    filter: '',
};

const parseEnum = <T extends string>(values: readonly T[], value: unknown): T | undefined => {
    return values.find((candidate) => candidate === value);
};

const parseRepoId = (repoId: unknown): number | undefined => {
    if (repoId == undefined) {
        return undefined;
    }
    const parsed = Number(repoId);
    return Number.isInteger(parsed) ? parsed : undefined;
};

const parseText = (value: unknown): string | undefined => {
    return typeof value === 'string' && value.length > 0 ? value : undefined;
};

/**
 * Turn anything claiming to be page state into state we can act on. Both inputs are
 * outside our control — a url someone typed, and a blob a previous release wrote into
 * local storage — so an unknown tab, a repository id that is not a number or a sorting
 * that no longer exists falls back to its default rather than reaching a component.
 */
export const normalizeDaoPageState = (raw: Partial<Record<keyof DaoPageUrlState, unknown>>): DaoPageUrlState => {
    const tab = parseEnum(DAO_TABS, raw.tab) ?? DEFAULT_DAO_TAB;
    if (tab !== FILE_SYSTEM_DAO_TAB) {
        return { ...DEFAULT_DAO_PAGE_URL_STATE, tab };
    }

    return {
        tab,
        repoId: parseRepoId(raw.repoId),
        path: parseText(raw.path),
        item: parseText(raw.item),
        sort: parseEnum(Object.values(FsItemSortType), raw.sort) ?? FsItemSortType.NameAsc,
        linked: parseEnum(Object.values(FsItemFilterByLinked), raw.linked) ?? FsItemFilterByLinked.All,
        filter: parseText(raw.filter) ?? '',
    };
};

export const parseDaoPageUrl = (params: DaoPageUrlParams, search: string): DaoPageUrlState => {
    const query = new URLSearchParams(search);
    return normalizeDaoPageState({
        tab: params.tab != undefined ? slugToTab.get(params.tab) : DEFAULT_DAO_TAB,
        repoId: params.repoId,
        path: query.get('path'),
        item: query.get('item'),
        sort: query.get('sort'),
        linked: query.get('linked'),
        filter: query.get('q'),
    });
};

export const buildDaoPageUrl = (fundId: number, versionId: number | undefined, state: DaoPageUrlState): string => {
    const slug = state.tab === DEFAULT_DAO_TAB ? undefined : tabToSlug[state.tab];
    const isFileSystemTab = state.tab === FILE_SYSTEM_DAO_TAB;
    const url = urlFundDaos(fundId, versionId, slug, isFileSystemTab ? state.repoId : undefined);
    if (!isFileSystemTab) {
        return url;
    }

    const query = new URLSearchParams();
    if (state.path) {
        query.set('path', state.path);
    }
    if (state.item) {
        query.set('item', state.item);
    }
    if (state.sort !== FsItemSortType.NameAsc) {
        query.set('sort', state.sort);
    }
    if (state.linked !== FsItemFilterByLinked.All) {
        query.set('linked', state.linked);
    }
    if (state.filter) {
        query.set('q', state.filter);
    }

    // Slashes are legal unescaped in a query value, so a path stays readable in a shared link.
    const search = query.toString().replace(/%2F/g, '/');
    return search ? `${url}?${search}` : url;
};
