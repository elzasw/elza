import { Fragment, useRef, useState, useEffect, useLayoutEffect } from 'react';
import { Api } from 'api';
import { getFullPath } from 'api/api';
import classNames from 'classnames';
import { Button, Menu, MenuItem, MenuList, MenuPopover, MenuTrigger, Popover, PopoverSurface, PopoverTrigger } from '@fluentui/react-components';
import { ArrowClockwiseFilled, ArrowDownloadRegular, ArrowUpRegular, DeleteRegular, DocumentRegular, FilterRegular, FolderRegular, LinkRegular, TextSortAscendingRegular } from '@fluentui/react-icons';
import { FsRepo, FsItem, FsItemType, FsItemSortType, FsItemFilterByLinked, FsLink } from 'elza-api';
import { useDebouncedEffect } from 'utils/hooks/hooks';
import { useAppThunkDispatch } from 'utils/hooks';
import { routerNavigate } from 'actions/router.jsx';
import { urlFundNode } from '../../../../constants';
import { FormattedMessage, defineMessages, useIntl } from 'react-intl';
import { Icon, Splitter } from 'components/shared';
import { daoMessages } from 'components/arr/daoMessages';
import { humanFileSize } from 'components/Utils.jsx';
import "./FileSystemBrowser.scss"
import { Tree, TreeExposedFunctions } from './Tree';
import { FileSystemBrowserState, RenderItem, RenderItemType, isListItem, isLastKeyItem } from './types';
import { buildFullPath, extractRepoIdFromFullPath } from './extractRepoIdFromFullPath';

const messages = defineMessages({
    sortLabel: {
        id: 'arr.daos.fileSystem.sort.label',
        defaultMessage: 'Řazení',
    },
    sortNameAsc: {
        id: 'arr.daos.fileSystem.sort.nameAsc',
        defaultMessage: 'Název A→Z',
    },
    sortNameDesc: {
        id: 'arr.daos.fileSystem.sort.nameDesc',
        defaultMessage: 'Název Z→A',
    },
    sortSizeAsc: {
        id: 'arr.daos.fileSystem.sort.sizeAsc',
        defaultMessage: 'Velikost vzestupně',
    },
    sortSizeDesc: {
        id: 'arr.daos.fileSystem.sort.sizeDesc',
        defaultMessage: 'Velikost sestupně',
    },
    sortLastChangeAsc: {
        id: 'arr.daos.fileSystem.sort.lastChangeAsc',
        defaultMessage: 'Datum změny vzestupně',
    },
    sortLastChangeDesc: {
        id: 'arr.daos.fileSystem.sort.lastChangeDesc',
        defaultMessage: 'Datum změny sestupně',
    },
    filterPlaceholder: {
        id: 'arr.daos.fileSystem.filter.placeholder',
        defaultMessage: 'Filtrovat…',
    },
    filterClear: {
        id: 'arr.daos.fileSystem.filter.clear',
        defaultMessage: 'Vymazat filtr',
    },
    filterByLinkLabel: {
        id: 'arr.daos.fileSystem.filterByLink.label',
        defaultMessage: 'Filtr',
    },
    filterByLinkAll: {
        id: 'arr.daos.fileSystem.filterByLink.all',
        defaultMessage: 'Vše',
    },
    filterByLinkLinked: {
        id: 'arr.daos.fileSystem.filterByLink.linked',
        defaultMessage: 'Připojené',
    },
    filterByLinkUnlinked: {
        id: 'arr.daos.fileSystem.filterByLink.unlinked',
        defaultMessage: 'Nepřipojené',
    },
    linksTrigger: {
        id: 'arr.daos.fileSystem.links.trigger',
        defaultMessage: 'Seznam vazeb',
    },
    linksTitle: {
        id: 'arr.daos.fileSystem.links.title',
        defaultMessage: 'Přejít k jednotce popisu',
    },
    linkForbidden: {
        id: 'arr.daos.fileSystem.links.forbidden',
        defaultMessage: 'Nemáte oprávnění k tomuto archivnímu souboru',
    },
    repoUnavailableTitle: {
        id: 'arr.daos.fileSystem.repo.unavailableTitle',
        defaultMessage: 'Repozitář není dostupný',
    },
    repoUnavailableDetail: {
        id: 'arr.daos.fileSystem.repo.unavailableDetail',
        defaultMessage: 'Cesta {path} na serveru neexistuje nebo ji nelze číst. Obsah repozitáře proto nelze zobrazit — zkontrolujte nastavení externího systému.',
    },
    refresh: {
        id: 'arr.daos.fileSystem.refresh',
        defaultMessage: 'Obnovit seznam souborů a složek',
    },
    reposLoadErrorTitle: {
        id: 'arr.daos.fileSystem.repos.loadErrorTitle',
        defaultMessage: 'Nelze načíst seznam repozitářů',
    },
    reposLoadErrorDetail: {
        id: 'arr.daos.fileSystem.repos.loadErrorDetail',
        defaultMessage: 'Zkuste to znovu tlačítkem Obnovit.',
    },
    itemsLoadErrorTitle: {
        id: 'arr.daos.fileSystem.items.loadErrorTitle',
        defaultMessage: 'Nelze načíst obsah složky',
    },
    itemsLoadErrorDetail: {
        id: 'arr.daos.fileSystem.items.loadErrorDetail',
        defaultMessage: 'Zkuste to znovu tlačítkem Obnovit nebo přejděte na jinou složku.',
    },
    itemsLoading: {
        id: 'arr.daos.fileSystem.items.loading',
        defaultMessage: 'Načítání obsahu složky…',
    },
    contextDownload: {
        id: 'arr.daos.fileSystem.context.download',
        defaultMessage: 'Stáhnout',
    },
});

interface Props {
    fundId: number;
    onSelect?: (item?: FsItem, fullPath?: string, repo?: FsRepo) => void;
    refreshCounter?: number;
    state: FileSystemBrowserState;
    onStateChange: (changes: Partial<FileSystemBrowserState>) => void;
}

export type FileSystemBrowserProps = Props;

export const FileSystemBrowser = ({
    fundId,
    onSelect = () => { return; },
    refreshCounter,
    state,
    onStateChange,
}: Props) => {
    const treeRef = useRef<TreeExposedFunctions>(null);
    const breadcrumbsRef = useRef<HTMLDivElement>(null);

    const { sort: sortType, linked: filterByLink, filter: committedFilter } = state;

    const [levelList, setLevelList] = useState<RenderItem[]>([]);
    const [loadedPath, setLoadedPath] = useState<string>();
    const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({});
    const [childrenMap, setChildrenMap] = useState<Record<string, boolean>>({});
    const [repos, setRepos] = useState<FsRepo[]>([]);

    const intl = useIntl();
    const dispatch = useAppThunkDispatch();

    const [filterInput, setFilterInput] = useState(committedFilter);
    const [treeSize, setTreeSize] = useState<number>(100);
    const [localRefreshTick, setLocalRefreshTick] = useState(0);
    const [reposError, setReposError] = useState<boolean>(false);
    const [itemsError, setItemsError] = useState<boolean>(false);
    const [itemsLoading, setItemsLoading] = useState<boolean>(false);

    const selectedTreeItemPath = state.repoId != undefined
        ? buildFullPath(state.repoId, state.path)
        : undefined;
    const selectedListItem = state.item != undefined && selectedTreeItemPath != undefined
        ? `${selectedTreeItemPath}/${state.item}`
        : undefined;

    const selectTreeItem = (fullPath: string) => {
        const [repoId, path] = extractRepoIdFromFullPath(fullPath);
        onStateChange({ repoId, path, item: undefined });
    };

    // Number of middle path segments currently collapsed into the "…" separator.
    // The first segment and the last segment (when depth > 1) always stay visible;
    // this only ever grows/shrinks to make the breadcrumb row fit its available width.
    const [hiddenMiddleCount, setHiddenMiddleCount] = useState(0);

    // Path changed → start fully expanded again; the measuring effect below will
    // collapse only as much as is actually needed for the new path.
    useEffect(() => {
        setHiddenMiddleCount(0);
    }, [selectedTreeItemPath]);

    // Available width changed (e.g. window resize) → re-expand and let the
    // measuring effect re-collapse from scratch, in case there's now more room.
    useEffect(() => {
        const el = breadcrumbsRef.current;
        if (!el || typeof ResizeObserver === 'undefined') {
            return;
        }
        const observer = new ResizeObserver(() => {
            setHiddenMiddleCount(0);
        });
        observer.observe(el.parentElement || el);
        return () => observer.disconnect();
    }, []);

    // After each render, if the breadcrumb row overflows its available width,
    // collapse one more middle segment and let this effect re-check again.
    useLayoutEffect(() => {
        const el = breadcrumbsRef.current;
        if (!el) {
            return;
        }
        const totalSegments = selectedTreeItemPath ? selectedTreeItemPath.split("/").length : 0;
        const maxHiddenCount = Math.max(0, totalSegments - 2);
        if (el.scrollWidth > el.clientWidth && hiddenMiddleCount < maxHiddenCount) {
            setHiddenMiddleCount((count) => count + 1);
        }
    }, [selectedTreeItemPath, hiddenMiddleCount, repos]);

    // Only the settled filter is published, so typing does not rewrite the url per keystroke.
    useDebouncedEffect(() => {
        if (filterInput !== committedFilter) {
            onStateChange({ filter: filterInput });
        }
    }, 300, [filterInput]);

    // The owner can change the filter on its own (a restored url, browser back).
    useEffect(() => {
        setFilterInput(committedFilter);
    }, [committedFilter]);

    // Keep the branch leading to the shown directory open, so a directory restored from
    // the url is revealed in the tree instead of hiding behind collapsed ancestors.
    useEffect(() => {
        if (!selectedTreeItemPath) {
            return;
        }
        const segments = selectedTreeItemPath.split("/");
        const ancestors = segments.slice(0, -1).map((_segment, index) => segments.slice(0, index + 1).join("/"));
        setExpandedItems((prev) => {
            const missing = ancestors.filter((ancestor) => !prev[ancestor]);
            if (missing.length === 0) {
                return prev;
            }
            return { ...prev, ...Object.fromEntries(missing.map((ancestor) => [ancestor, true])) };
        });
    }, [selectedTreeItemPath]);

    // Repository of the selected tree item; unavailable ones cannot be browsed and
    // the file list is replaced by an explanation instead.
    const selectedRepo = repos.find((repo) => repo.fsRepoId === state.repoId);
    const isSelectedRepoUnavailable = selectedRepo != undefined && !selectedRepo.available;

    const loadLevel = async (fullPath: string, lastKey: string | undefined, depth: number = 0, filter?: FsItemType) => {
        const [repoId, path] = extractRepoIdFromFullPath(fullPath)
        const { data: items } = await Api.funds.fundFsRepoItems(fundId, repoId, filter, path, lastKey, filterByLink, sortType, committedFilter || undefined);
        const itemLevel: RenderItem[] = items.items.map((item) => {
            const extendedItemBase: FsItem = {
                ...item,
            }
            return {
                type: RenderItemType.Item,
                data: extendedItemBase,
                depth,
                parentFullPath: fullPath,
                fullPath: `${fullPath}/${item.name}`,
            }
        })
        if (items.lastKey != undefined) {
            itemLevel.push({
                type: RenderItemType.LastKey,
                data: {
                    lastKey: items.lastKey,
                    path: fullPath,
                },
                parentFullPath: fullPath,
                fullPath: `${fullPath}/?lastKey`,
                depth,
            })
        }
        if (!childrenMap[fullPath] && itemLevel.find((item) => { return isListItem(item) && item.data.itemType === FsItemType.Folder })) {
            setChildrenMap((prev) => ({ ...prev, [fullPath]: true }));
        }
        return itemLevel;
    }

    const handleDownloadFile = (item: FsItem, fullPath: string) => {
        const [repoId, path] = extractRepoIdFromFullPath(fullPath);
        const params = new URLSearchParams();
        if (path) params.set('path', path);
        const url = getFullPath(`fund/${fundId}/fsrepo/${repoId}/item-data?${params}`);
        // Explicit anchor with `download` streams via the browser and forces save
        // even for inline-renderable types (image, txt) that the server serves
        // with Content-Disposition: inline.
        const a = document.createElement('a');
        a.href = url;
        a.download = item.name;
        document.body.appendChild(a);
        a.click();
        a.remove();
    };

    const renderListItem = (item: RenderItem) => {
        if (isLastKeyItem(item)) {
            return <div
                className="list-item"
                onClick={() => {
                    const index = levelList.findIndex((listItem) => {
                        if (isLastKeyItem(listItem)) {
                            return listItem.fullPath === item.fullPath;
                        }
                    })
                    loadMoreListItems(item.parentFullPath || "", item.data.lastKey, index, item.depth)
                }}
            >
                <span className="item-part left" title={intl.formatMessage(daoMessages.daosFileSystemLoadMore)}>
                    {<FormattedMessage {...daoMessages.daosFileSystemLoadMore} />}
                </span>
            </div>
        }

        const buildDateString = (date: Date) => {
            return `${date.toLocaleDateString([], { year: "2-digit", month: "numeric", day: "numeric" })}  ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`
        }

        if (isListItem(item)) {
            const isSelected = item.fullPath === selectedListItem;
            const lastChangeDate = new Date(item.data.lastChange);
            const isFile = item.data.itemType === FsItemType.File;
            const row = <div
                className={classNames("list-item", { "selected": isSelected })}
                onDoubleClick={(e) => {
                    e.preventDefault();
                    if (item.data.itemType == FsItemType.Folder && item.parentFullPath) {
                        if (treeRef.current) { treeRef.current.toggleExpand(item.parentFullPath, true) }
                        selectTreeItem(item.fullPath);
                    }
                }}
                onClick={() => {
                    onStateChange({ item: item.data.name });
                    onSelect(item.data, item.fullPath, selectedRepo);
                }}
            >
                <span className="item-part left no-shrink" title={item.data.name}>
                    {item.data.itemType === FsItemType.Folder ? <FolderRegular fontSize={18} /> : <DocumentRegular fontSize={18} />}
                </span>
                {item.data.links && item.data.links.length > 0 && (
                    <Popover>
                        <PopoverTrigger disableButtonEnhancement>
                            <button
                                type="button"
                                className="link-popover-trigger"
                                aria-label={intl.formatMessage(messages.linksTrigger)}
                                title={intl.formatMessage(messages.linksTrigger)}
                                onClick={(e) => e.stopPropagation()}
                            >
                                <LinkRegular fontSize={16} />
                            </button>
                        </PopoverTrigger>
                        <PopoverSurface>
                            <div className="fs-link-popover">
                                <div className="fs-link-popover__title">
                                    {intl.formatMessage(messages.linksTitle)}
                                </div>
                                <ul className="fs-link-popover__list">
                                    {item.data.links.map((link: FsLink) => (
                                        <li key={`${link.fundId}-${link.nodeId}`}>
                                            {link.readable ? (
                                                <button
                                                    type="button"
                                                    className="fs-link-popover__link"
                                                    title={link.nodePath}
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        dispatch(routerNavigate(urlFundNode(link.fundId, undefined, link.nodeId)));
                                                    }}
                                                >
                                                    <span className="fs-link-popover__fund">{link.fundName}</span>
                                                    <span className="fs-link-popover__node">{link.nodeLabel}</span>
                                                </button>
                                            ) : (
                                                <div
                                                    className="fs-link-popover__link fs-link-popover__link--disabled"
                                                    title={intl.formatMessage(messages.linkForbidden)}
                                                >
                                                    <span className="fs-link-popover__fund">{link.fundName}</span>
                                                    <span className="fs-link-popover__node">{link.nodeLabel}</span>
                                                </div>
                                            )}
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        </PopoverSurface>
                    </Popover>
                )}
                <span className="item-part left" title={item.data.name}>
                    {item.data.name}
                </span>
                <span className="spacer" />
                {item.data.size != null && <span className="item-part right no-shrink" style={{ width: "10ch" }} title={humanFileSize(item.data.size)}>
                    {humanFileSize(item.data.size)}
                </span>}
                <span className="item-part right no-shrink" style={{ width: "18ch" }} title={buildDateString(lastChangeDate)}>
                    {buildDateString(lastChangeDate)}
                </span>
            </div>;
            if (!isFile) {
                return row;
            }
            return (
                <Menu openOnContext>
                    <MenuTrigger disableButtonEnhancement>
                        {row}
                    </MenuTrigger>
                    <MenuPopover>
                        <MenuList>
                            <MenuItem
                                icon={<ArrowDownloadRegular />}
                                onClick={() => handleDownloadFile(item.data, item.fullPath)}
                            >
                                {intl.formatMessage(messages.contextDownload)}
                            </MenuItem>
                        </MenuList>
                    </MenuPopover>
                </Menu>
            );
        }
    }

    const loadMoreListItems = async (_path: string | undefined = undefined, lastKey: string, index: number, depth: number) => {
        if (selectedTreeItemPath) {
            const itemsEx: RenderItem[] = await loadLevel(selectedTreeItemPath, lastKey, depth);

            setLevelList((prev) => {
                const next = [...prev];
                next.splice(index, 1, ...itemsEx);
                return next;
            });
        }
    }

    useEffect(() => {
        let cancelled = false;
        (async () => {
            if (isSelectedRepoUnavailable) {
                setLevelList([]);
                setLoadedPath(selectedTreeItemPath);
                setItemsError(false);
                setItemsLoading(false);
                return;
            }
            if (selectedTreeItemPath) {
                setItemsLoading(true);
                try {
                    const itemsEx = await loadLevel(selectedTreeItemPath, undefined, 0);
                    if (!cancelled) {
                        setLevelList(itemsEx);
                        setLoadedPath(selectedTreeItemPath);
                        setItemsError(false);
                    }
                } catch (e) {
                    console.error('Failed to load fs items', e);
                    if (!cancelled) {
                        setLevelList([]);
                        setLoadedPath(selectedTreeItemPath);
                        setItemsError(true);
                    }
                } finally {
                    if (!cancelled) setItemsLoading(false);
                }
            }
        })();
        return () => { cancelled = true; };
    }, [selectedTreeItemPath, isSelectedRepoUnavailable, sortType, filterByLink, committedFilter, refreshCounter, localRefreshTick])

    // Each load replaces every FsItem instance, so the copy handed to the parent — its
    // list of links above all — would go stale. Re-emit the selected item from the fresh
    // list, or drop the selection when the item is no longer listed. Waits for a load of
    // the current directory, otherwise a selection restored from the url would be dropped
    // against the empty initial list.
    useEffect(() => {
        if (!selectedListItem || loadedPath !== selectedTreeItemPath) {
            return;
        }
        const refreshed = levelList.find((item) => isListItem(item) && item.fullPath === selectedListItem);
        if (refreshed && isListItem(refreshed)) {
            onSelect(refreshed.data, refreshed.fullPath, selectedRepo);
        } else {
            onStateChange({ item: undefined });
            onSelect(undefined, undefined, undefined);
        }
    }, [levelList, loadedPath, selectedTreeItemPath, selectedListItem, selectedRepo, onSelect, onStateChange])

    useEffect(() => {
        return () => {
            onSelect(undefined, undefined, undefined);
        }
    }, [])

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const { data } = await Api.funds.fundFsRepos(fundId);
                if (!cancelled) {
                    setRepos(data);
                    setReposError(false);
                }
            } catch (e) {
                console.error('Failed to load fs repositories', e);
                if (!cancelled) setReposError(true);
            }
        })();
        return () => { cancelled = true; };
    }, [fundId, refreshCounter, localRefreshTick])

    // Fall back to the first repository when none is chosen, and also when the url names
    // one this fund does not have.
    useEffect(() => {
        if (repos.length === 0) {
            return;
        }
        const isKnownRepo = repos.some((repo) => repo.fsRepoId === state.repoId);
        if (!isKnownRepo) {
            onStateChange({ repoId: repos[0].fsRepoId, path: undefined, item: undefined });
        }
    }, [repos, state.repoId, onStateChange])


    // const getImageUrl = () => {
    //     if (selectedListItem) {
    //         const [repoId, path] = extractRepoIdFromFullPath(selectedListItem);
    //         return `/api/digirepo/${repoId}?filePath=${path}`;
    //     }
    // }

    const generateBreadcrumbs = () => {
        const pathParts = selectedTreeItemPath?.split("/") || [];
        const breadcrumbParts: string[] = [];
        pathParts.map((_pathPart, index) => {
            const partArr: string[] = []
            for (let i = index; i >= 0; i--) {
                partArr.push(pathParts[i]);
            }
            breadcrumbParts.push(partArr.reverse().join("/"))
        })
        const repoName = repos.find((repo) => repo.fsRepoId.toString() === pathParts[0])?.name || pathParts[0];

        const total = breadcrumbParts.length;
        // The first and last segments are never collapsed — only clamp for safety,
        // the measuring effect already keeps hiddenMiddleCount within this bound.
        const maxHiddenCount = Math.max(0, total - 2);
        const hiddenCount = Math.min(hiddenMiddleCount, maxHiddenCount);
        // When collapsing, the hidden range always starts right after the first
        // segment (index 1) and grows towards the last segment.
        const hiddenStart = hiddenCount > 0 ? 1 : null;
        const hiddenEnd = hiddenCount > 0 ? hiddenStart! + hiddenCount - 1 : null;

        return <div className="breadcrumbs" ref={breadcrumbsRef}>
            {breadcrumbParts.map((breadcrumb, index) => {
                const isLast = index === total - 1;
                const isHidden = hiddenStart !== null && index >= hiddenStart && index <= hiddenEnd!;

                if (isHidden) {
                    // Render the "…" separator only once, right where the hidden range starts.
                    if (index !== hiddenStart) {
                        return null;
                    }
                    const hiddenNames = breadcrumbParts
                        .slice(hiddenStart, hiddenEnd! + 1)
                        .map((bp) => bp.split("/").pop());
                    return <Fragment key={`ellipsis-${hiddenStart}`}>
                        <span className="ellipsis" title={hiddenNames.join(" / ")}>
                            &hellip;
                        </span>
                        <div className="divider">
                            <Icon glyph="fa-angle-right" />
                        </div>
                    </Fragment>
                }

                const parts = breadcrumb.split("/")
                return <Fragment key={breadcrumb}>
                    <div className="btn" title={breadcrumb} onClick={() => { selectTreeItem(breadcrumb) }}>
                        {index === 0 ? repoName : parts[parts.length - 1]}
                    </div>
                    {!isLast
                        && <div className="divider">
                            <Icon glyph="fa-angle-right" />
                        </div>}
                </Fragment>
            })}
        </div>

    }

    const handleSelectParent = () => {
        const pathParts = selectedTreeItemPath?.split("/");
        if (pathParts?.length && pathParts.length > 1) {
            pathParts?.pop();
            selectTreeItem(pathParts?.join("/"));
        }
    }

    return (
        <div className="file-system-browser">
            <div className="toolbar">
                <div className="actions">
                    <Button
                        appearance="subtle"
                        size="small"
                        icon={<ArrowUpRegular />}
                        onClick={handleSelectParent}
                        title={intl.formatMessage(daoMessages.daosFileSystemSelectParent)}
                        aria-label={intl.formatMessage(daoMessages.daosFileSystemSelectParent)}
                    />
                </div>
                {generateBreadcrumbs()}
                <div className="filters">
                    <span className="sort-label" title={intl.formatMessage(messages.filterByLinkLabel)}>
                        <FilterRegular fontSize={18} />
                    </span>
                    <select
                        id="filter-by-link-select"
                        className="sort-select"
                        aria-label={intl.formatMessage(messages.filterByLinkLabel)}
                        value={filterByLink}
                        onChange={(e) => onStateChange({ linked: e.target.value as FsItemFilterByLinked })}
                    >
                        <option value={FsItemFilterByLinked.All}>{intl.formatMessage(messages.filterByLinkAll)}</option>
                        <option value={FsItemFilterByLinked.Linked}>{intl.formatMessage(messages.filterByLinkLinked)}</option>
                        <option value={FsItemFilterByLinked.Unlinked}>{intl.formatMessage(messages.filterByLinkUnlinked)}</option>
                    </select>
                    <input
                        type="text"
                        className="file-filter"
                        placeholder={intl.formatMessage(messages.filterPlaceholder)}
                        value={filterInput}
                        onChange={(e) => setFilterInput(e.target.value)}
                    />
                    {filterInput && (
                        <Button
                            appearance="subtle"
                            size="small"
                            icon={<DeleteRegular />}
                            onClick={() => {
                                setFilterInput('');
                                onStateChange({ filter: '' });
                            }}
                            title={intl.formatMessage(messages.filterClear)}
                            aria-label={intl.formatMessage(messages.filterClear)}
                            disabled={!filterInput}
                        />
                    )}
                    <span className="sort-label" title={intl.formatMessage(messages.sortLabel)}>
                        <TextSortAscendingRegular fontSize={18} />
                    </span>
                    <select
                        id="sort-select"
                        className="sort-select"
                        aria-label={intl.formatMessage(messages.sortLabel)}
                        value={sortType}
                        onChange={(e) => onStateChange({ sort: e.target.value as FsItemSortType })}
                    >
                        <option value={FsItemSortType.NameAsc}>{intl.formatMessage(messages.sortNameAsc)}</option>
                        <option value={FsItemSortType.NameDesc}>{intl.formatMessage(messages.sortNameDesc)}</option>
                        <option value={FsItemSortType.SizeAsc}>{intl.formatMessage(messages.sortSizeAsc)}</option>
                        <option value={FsItemSortType.SizeDesc}>{intl.formatMessage(messages.sortSizeDesc)}</option>
                        <option value={FsItemSortType.LastChangeAsc}>{intl.formatMessage(messages.sortLastChangeAsc)}</option>
                        <option value={FsItemSortType.LastChangeDesc}>{intl.formatMessage(messages.sortLastChangeDesc)}</option>
                    </select>
                </div>
                <div className="actions actions--end">
                    <Button
                        appearance="subtle"
                        size="small"
                        icon={<ArrowClockwiseFilled />}
                        onClick={() => setLocalRefreshTick((tick) => tick + 1)}
                        title={intl.formatMessage(messages.refresh)}
                        aria-label={intl.formatMessage(messages.refresh)}
                    />
                </div>
            </div>
            <div className="main-container">
                <Splitter
                    leftSize={treeSize}
                    onChange={({ leftSize }: { leftSize: number; rightSize: number }) => setTreeSize(leftSize)}
                    left={
                        reposError ? (
                            <div className="repo-unavailable">
                                <Icon glyph="fa-exclamation-triangle" className="fa-lg" />
                                <div className="repo-unavailable__title">
                                    {intl.formatMessage(messages.reposLoadErrorTitle)}
                                </div>
                                <div className="repo-unavailable__detail">
                                    {intl.formatMessage(messages.reposLoadErrorDetail)}
                                </div>
                            </div>
                        ) : (
                            <Tree
                                ref={treeRef}
                                fundId={fundId}
                                selectedItemPath={selectedTreeItemPath}
                                onSelect={(item) => { selectTreeItem(item.fullPath) }}
                                expandedItems={expandedItems}
                                onExpandChange={(itemFullPath, expanded) =>
                                    setExpandedItems((prev) => ({ ...prev, [itemFullPath]: expanded }))
                                }
                                childrenMap={childrenMap}
                                repos={repos}
                                refreshKey={(refreshCounter ?? 0) + localRefreshTick}
                            />
                        )
                    }
                    center={
                        isSelectedRepoUnavailable ? (
                            <div className="repo-unavailable">
                                <Icon glyph="fa-exclamation-triangle" className="fa-lg" />
                                <div className="repo-unavailable__title">
                                    {intl.formatMessage(messages.repoUnavailableTitle)}
                                </div>
                                <div className="repo-unavailable__detail">
                                    {intl.formatMessage(messages.repoUnavailableDetail, { path: selectedRepo?.path })}
                                </div>
                            </div>
                        ) : itemsError ? (
                            <div className="repo-unavailable">
                                <Icon glyph="fa-exclamation-triangle" className="fa-lg" />
                                <div className="repo-unavailable__title">
                                    {intl.formatMessage(messages.itemsLoadErrorTitle)}
                                </div>
                                <div className="repo-unavailable__detail">
                                    {intl.formatMessage(messages.itemsLoadErrorDetail)}
                                </div>
                            </div>
                        ) : itemsLoading ? (
                            <div className="repo-unavailable repo-unavailable--loading">
                                <Icon glyph="fa-spinner fa-spin" className="fa-lg" />
                                <div className="repo-unavailable__title">
                                    {intl.formatMessage(messages.itemsLoading)}
                                </div>
                            </div>
                        ) : (
                            <div className="file-list">
                                {levelList.map((item) => (
                                    <Fragment key={item.fullPath}>
                                        {renderListItem(item)}
                                    </Fragment>
                                ))}
                            </div>
                        )
                    }
                />
            </div>
            {/* {selectedListItem && <div style={{ border: "var(--primary-border)", display: "flex", justifyContent: "center" }}> */}
            {/*     <img style={{ maxHeight: "200px" }} src={getImageUrl()} /> */}
            {/* </div>} */}
        </div>
    );
}
