import { Fragment, useRef, useState, useEffect, useLayoutEffect } from 'react';
import { Api } from 'api';
import classNames from 'classnames';
import { Button, Popover, PopoverSurface, PopoverTrigger } from '@fluentui/react-components';
import { ArrowClockwiseFilled, ArrowUpRegular, DeleteRegular, FilterRegular, TextSortAscendingRegular } from '@fluentui/react-icons';
import { FsRepo, FsItem, FsItemType, FsItemSortType, FsItemFilterByLinked, FsLink } from 'elza-api';
import { useDebouncedEffect } from 'utils/hooks/hooks';
import { useAppThunkDispatch } from 'utils/hooks';
import { routerNavigate } from 'actions/router.jsx';
import { urlFundNode } from '../../../../constants';
import { defineMessages, useIntl } from 'react-intl';
import { i18n, Icon, Splitter } from 'components/shared';
import { humanFileSize } from 'components/Utils.jsx';
import "./FileSystemBrowser.scss"
import { Tree, TreeExposedFunctions } from './Tree';
import { RenderItem, RenderItemType, isListItem, isLastKeyItem } from './types';
import { extractRepoIdFromFullPath } from './extractRepoIdFromFullPath';

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
        defaultMessage: 'Propojené',
    },
    filterByLinkUnlinked: {
        id: 'arr.daos.fileSystem.filterByLink.unlinked',
        defaultMessage: 'Nepropojené',
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
});

interface Props {
    fundId: number;
    onSelect?: (item?: FsItem, fullPath?: string) => void;
    refreshCounter?: number;
}

export const FileSystemBrowser = ({
    fundId,
    onSelect = () => { return; },
    refreshCounter,
}: Props) => {
    const treeRef = useRef<TreeExposedFunctions>(null);
    const breadcrumbsRef = useRef<HTMLDivElement>(null);

    const [levelList, setLevelList] = useState<RenderItem[]>([]);
    const [selectedTreeItemPath, setSelectedTreeItem] = useState<string>();
    const [selectedListItem, setSelectedListItem] = useState<string>();
    const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({});
    const [childrenMap, setChildrenMap] = useState<Record<string, boolean>>({});
    const [repos, setRepos] = useState<FsRepo[]>([]);

    const intl = useIntl();
    const dispatch = useAppThunkDispatch();

    const [sortType, setSortType] = useState<FsItemSortType>(FsItemSortType.NameAsc);
    const [filterByLink, setFilterByLink] = useState<FsItemFilterByLinked>(FsItemFilterByLinked.All);
    const [filterInput, setFilterInput] = useState('');
    const [treeSize, setTreeSize] = useState<number>(100);
    const [debouncedFilter, setDebouncedFilter] = useState('');
    const [localRefreshTick, setLocalRefreshTick] = useState(0);
    const [reposError, setReposError] = useState<boolean>(false);
    const [itemsError, setItemsError] = useState<boolean>(false);
    const [itemsLoading, setItemsLoading] = useState<boolean>(false);

    // On refresh, collapse every previously expanded node so the "[-]" icon
    // and the visible content stay in sync while the Tree wipes its cache.
    useEffect(() => {
        setExpandedItems({});
    }, [refreshCounter, localRefreshTick]);

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

    useDebouncedEffect(() => {
        setDebouncedFilter(filterInput);
    }, 300, [filterInput]);

    // Repository of the selected tree item; unavailable ones cannot be browsed and
    // the file list is replaced by an explanation instead.
    const selectedRepo = selectedTreeItemPath
        ? repos.find((repo) => repo.fsRepoId === extractRepoIdFromFullPath(selectedTreeItemPath)[0])
        : undefined;
    const isSelectedRepoUnavailable = selectedRepo != undefined && !selectedRepo.available;

    const loadLevel = async (fullPath: string, lastKey: string | undefined, depth: number = 0, filter?: FsItemType) => {
        const [repoId, path] = extractRepoIdFromFullPath(fullPath)
        const { data: items } = await Api.funds.fundFsRepoItems(fundId, repoId, filter, path, lastKey, filterByLink, sortType, debouncedFilter || undefined);
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
                <span className="item-part left" title={i18n("arr.daos.fileSystem.loadMore")}>
                    {i18n("arr.daos.fileSystem.loadMore")}
                </span>
            </div>
        }

        const buildDateString = (date: Date) => {
            return `${date.toLocaleDateString([], { year: "2-digit", month: "numeric", day: "numeric" })}  ${date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}`
        }

        if (isListItem(item)) {
            const isSelected = item.fullPath === selectedListItem;
            const lastChangeDate = new Date(item.data.lastChange);
            return <div
                className={classNames("list-item", { "selected": isSelected })}
                onDoubleClick={(e) => {
                    e.preventDefault();
                    if (item.data.itemType == FsItemType.Folder && item.parentFullPath) {
                        if (treeRef.current) { treeRef.current.toggleExpand(item.parentFullPath, true) }
                        setSelectedTreeItem(item.fullPath);
                    }
                }}
                onClick={() => {
                    setSelectedListItem(item.fullPath);
                    onSelect(item.data, item.fullPath);
                }}
            >
                <span className="item-part left no-shrink" title={item.data.name}>
                    {item.data.itemType === FsItemType.Folder ? <Icon glyph="fa-folder" /> : <Icon glyph="fa-file" />}
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
                                <Icon glyph="fa-link" />
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
            </div>
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
                        setItemsError(false);
                    }
                } catch (e) {
                    console.error('Failed to load fs items', e);
                    if (!cancelled) {
                        setLevelList([]);
                        setItemsError(true);
                    }
                } finally {
                    if (!cancelled) setItemsLoading(false);
                }
            }
        })();
        return () => { cancelled = true; };
    }, [selectedTreeItemPath, isSelectedRepoUnavailable, sortType, filterByLink, debouncedFilter, refreshCounter, localRefreshTick])

    useEffect(() => {
        return () => {
            onSelect(undefined, undefined);
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

    useEffect(() => {
        if (repos.length > 0 && !selectedTreeItemPath) {
            setSelectedTreeItem(repos[0].fsRepoId.toString());
        }
    }, [repos, selectedTreeItemPath])


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
                    <div className="btn" title={breadcrumb} onClick={() => { setSelectedTreeItem(breadcrumb) }}>
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
            setSelectedTreeItem(pathParts?.join("/"));
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
                        title={i18n("arr.daos.fileSystem.selectParent")}
                        aria-label={i18n("arr.daos.fileSystem.selectParent")}
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
                        onChange={(e) => setFilterByLink(e.target.value as FsItemFilterByLinked)}
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
                                setDebouncedFilter('');
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
                        onChange={(e) => setSortType(e.target.value as FsItemSortType)}
                    >
                        <option value={FsItemSortType.NameAsc}>{intl.formatMessage(messages.sortNameAsc)}</option>
                        <option value={FsItemSortType.NameDesc}>{intl.formatMessage(messages.sortNameDesc)}</option>
                        <option value={FsItemSortType.SizeAsc}>{intl.formatMessage(messages.sortSizeAsc)}</option>
                        <option value={FsItemSortType.SizeDesc}>{intl.formatMessage(messages.sortSizeDesc)}</option>
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
                                onSelect={(item) => { setSelectedTreeItem(item.fullPath) }}
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
