import { useState, useEffect, useMemo, forwardRef, useImperativeHandle } from 'react';
import { VirtualList } from 'components/shared';
import { Api } from 'api';
import classNames from 'classnames';
import { FsRepo, FsItem, FsItemType } from 'elza-api';
import { defineMessages, useIntl } from 'react-intl';
import { i18n, Icon } from 'components/shared';
import "./FileSystemBrowser.scss"
import {
    RenderItem,
    RenderItemType,
    isLastKeyItem,
    isRepoItem,
    isListItem,
    isErrorItem,
    isLoadingItem,
} from './types';
import { extractRepoIdFromFullPath } from './extractRepoIdFromFullPath';

const messages = defineMessages({
    repoUnavailable: {
        id: 'arr.daos.fileSystem.repo.unavailable',
        defaultMessage: 'Repozitář není dostupný — cesta {path} na serveru neexistuje nebo ji nelze číst. Zkontrolujte nastavení externího systému.',
    },
    loadError: {
        id: 'arr.daos.fileSystem.tree.loadError',
        defaultMessage: 'Nelze načíst — klikněte pro opakování',
    },
    loading: {
        id: 'arr.daos.fileSystem.tree.loading',
        defaultMessage: 'Načítání…',
    },
});

interface TreeProps {
    onSelect: (item: RenderItem) => void;
    onExpandChange?: (itemFullPath: string, expand: boolean) => void;
    expandedItems?: Record<string, boolean>;
    fundId: number;
    selectedItemPath?: string;
    childrenMap?: Record<string, boolean>;
    repos?: FsRepo[];
    /** Any change wipes the children cache — used by refresh from the browser. */
    refreshKey?: number;
}

export interface TreeExposedFunctions {
    toggleExpand: (fullPath: string, expandState?: boolean) => void;
}

type ChildrenState = 'loading' | { error: true } | RenderItem[];

const TREE_INDENT_PX = 10;

export const Tree = forwardRef<TreeExposedFunctions, TreeProps>(({
    fundId,
    onSelect,
    onExpandChange = () => { return; },
    selectedItemPath: _selectedItemPath,
    expandedItems = {},
    childrenMap = {},
    repos = [],
    refreshKey = 0,
}: TreeProps, ref) => {
    const intl = useIntl();
    // Callback ref via state — see the matching comment in FileSystemBrowser.tsx.
    const [treeContainer, setTreeContainer] = useState<HTMLDivElement | null>(null);

    // Keyed cache: fullPath → 'loading' | error | RenderItem[]. The single source
    // of truth for tree children — nothing else stores loaded items.
    const [childrenCache, setChildrenCache] = useState<Record<string, ChildrenState>>({});
    const [selectedItemPath, setSelectedItemPath] = useState<string | undefined>(_selectedItemPath);

    // Wipe cache when the repo list identity changes or the parent asks for refresh.
    const reposSignature = repos.map((r) => `${r.fsRepoId}:${r.available ? 1 : 0}:${r.name}`).join('|');
    useEffect(() => {
        setChildrenCache({});
    }, [reposSignature, refreshKey]);

    useEffect(() => {
        setSelectedItemPath(_selectedItemPath);
    }, [_selectedItemPath]);

    const loadLevel = async (fullPath: string, lastKey: string | undefined, depth: number, filter?: FsItemType): Promise<RenderItem[]> => {
        const [repoId, path] = extractRepoIdFromFullPath(fullPath);
        const { data: items } = await Api.funds.fundFsRepoItems(fundId, repoId, filter, path, lastKey);

        const itemLevel: RenderItem[] = items.items.map((item) => {
            const extendedItemBase: FsItem = { ...item };
            return {
                type: RenderItemType.Item,
                data: extendedItemBase,
                depth,
                parentFullPath: fullPath || null,
                fullPath: `${fullPath}/${item.name}`,
            };
        });
        if (items.lastKey != undefined) {
            itemLevel.push({
                type: RenderItemType.LastKey,
                data: {
                    lastKey: items.lastKey,
                    path: fullPath || "",
                },
                parentFullPath: fullPath || null,
                fullPath: `${fullPath}/?lastKey`,
                depth,
            });
        }
        return itemLevel;
    };

    const fetchChildren = async (parentPath: string, childDepth: number) => {
        setChildrenCache((prev) => ({ ...prev, [parentPath]: 'loading' }));
        try {
            const items = await loadLevel(parentPath, undefined, childDepth, FsItemType.Folder);
            setChildrenCache((prev) => ({ ...prev, [parentPath]: items }));
        } catch (e) {
            console.error('Failed to load tree level', e);
            setChildrenCache((prev) => ({ ...prev, [parentPath]: { error: true } }));
        }
    };

    const expandItem = async (item: RenderItem) => {
        if (!isListItem(item) && !isRepoItem(item)) return;
        onExpandChange(item.fullPath, true);

        const cached = childrenCache[item.fullPath];
        if (Array.isArray(cached)) {
            // already loaded — just marking expanded is enough
            return;
        }
        await fetchChildren(item.fullPath, item.depth + 1);
    };

    const collapseItem = (item: RenderItem) => {
        onExpandChange(item.fullPath, false);
        // Cache is preserved on collapse — re-expand stays cheap.
    };

    const toggleItem = (item: RenderItem, forcedExpandState?: boolean) => {
        const expandState = forcedExpandState == undefined ? !expandedItems[item.fullPath] : forcedExpandState;
        if (expandState) {
            expandItem(item);
        } else {
            collapseItem(item);
        }
    };

    const retryExpand = async (retryPath: string) => {
        // parentDepth is the depth of the node whose children we're retrying;
        // its children live at parentDepth + 1. The repo root has depth 0, so
        // "42" (one segment) parents children at depth 1, "42/foo" (two segments)
        // parents children at depth 2, and so on.
        const parentDepth = retryPath.split('/').length - 1;
        await fetchChildren(retryPath, parentDepth + 1);
    };

    const loadMoreItems = async (parentPath: string, lastKey: string, depth: number) => {
        try {
            const more = await loadLevel(parentPath, lastKey, depth, FsItemType.Folder);
            setChildrenCache((prev) => {
                const existing = prev[parentPath];
                if (!Array.isArray(existing)) return prev;
                const withoutLastKey = existing.filter((i) => !isLastKeyItem(i));
                return { ...prev, [parentPath]: [...withoutLastKey, ...more] };
            });
        } catch (e) {
            console.error('Failed to load more tree items', e);
            // leave last-key in place so the user can retry
        }
    };

    // Flatten the (repos → cache → expandedItems) into the ordered visible list
    // consumed by VirtualList. Recomputed on every relevant change; nothing
    // stored — no state to drift from the cache.
    const renderedTree = useMemo<RenderItem[]>(() => {
        const out: RenderItem[] = [];

        const appendChildren = (parentPath: string, childDepth: number) => {
            const state = childrenCache[parentPath];
            if (state === undefined) return; // marked expanded but fetch hasn't started yet
            if (state === 'loading') {
                out.push({
                    type: RenderItemType.Loading,
                    data: { forPath: parentPath },
                    parentFullPath: parentPath,
                    fullPath: `${parentPath}/?loading`,
                    depth: childDepth,
                });
                return;
            }
            if (!Array.isArray(state)) {
                out.push({
                    type: RenderItemType.Error,
                    data: { retryPath: parentPath },
                    parentFullPath: parentPath,
                    fullPath: `${parentPath}/?loadError`,
                    depth: childDepth,
                });
                return;
            }
            for (const child of state) {
                out.push(child);
                if (isListItem(child) && expandedItems[child.fullPath]) {
                    appendChildren(child.fullPath, child.depth + 1);
                }
            }
        };

        for (const repo of repos) {
            const repoItem: RenderItem = {
                type: RenderItemType.Repo,
                data: repo,
                depth: 0,
                parentFullPath: null,
                fullPath: repo.fsRepoId.toString(),
            };
            out.push(repoItem);
            if (expandedItems[repoItem.fullPath]) {
                appendChildren(repoItem.fullPath, 1);
            }
        }
        return out;
    }, [repos, expandedItems, childrenCache]);

    useImperativeHandle(ref, () => ({
        toggleExpand: (fullPath, expandState) => {
            const item = renderedTree.find((i) => i.fullPath === fullPath);
            if (item) {
                toggleItem(item, expandState);
            }
        }
    }));

    const renderItem = (item: RenderItem) => {
        if (isLoadingItem(item)) {
            return <div
                className="list-item loading"
                title={intl.formatMessage(messages.loading)}
            >
                <span className="item-part no-shrink">
                    <span
                        style={{
                            width: `${(item.depth + 1) * TREE_INDENT_PX}px`,
                            display: "inline-flex",
                            justifyContent: "flex-end",
                        }}
                    >
                        <Icon glyph="fa-spinner fa-spin" />
                    </span>
                </span>
                <span className="item-part">
                    {intl.formatMessage(messages.loading)}
                </span>
            </div>;
        }
        if (isErrorItem(item)) {
            return <div
                className="list-item error"
                title={intl.formatMessage(messages.loadError)}
                onClick={() => retryExpand(item.data.retryPath)}
            >
                <span className="item-part no-shrink">
                    <span
                        style={{
                            width: `${(item.depth + 1) * TREE_INDENT_PX}px`,
                            display: "inline-flex",
                            justifyContent: "flex-end",
                        }}
                    >
                        <Icon glyph="fa-exclamation-triangle" />
                    </span>
                </span>
                <span className="item-part">
                    {intl.formatMessage(messages.loadError)}
                </span>
            </div>;
        }
        if (isLastKeyItem(item)) {
            return <div
                className="list-item"
                onClick={() => {
                    loadMoreItems(item.parentFullPath || "", item.data.lastKey, item.depth);
                }}
            >
                <span className="item-part no-shrink">
                    <span
                        style={{
                            visibility: childrenMap[item.fullPath] ? "visible" : "hidden",
                            width: `${(item.depth + 1) * TREE_INDENT_PX}px`,
                            display: "inline-flex",
                            justifyContent: "flex-end",
                        }}
                    >
                    </span>
                </span>
                <span className="item-part">
                    {i18n("arr.daos.fileSystem.loadMore")}
                </span>
            </div>;
        }
        if (isRepoItem(item) || isListItem(item)) {
            const isExpanded = expandedItems[item.fullPath];
            const isSelected = item.fullPath === selectedItemPath;
            const isUnavailable = isRepoItem(item) && !item.data.available;
            const unavailableTitle = isUnavailable
                ? intl.formatMessage(messages.repoUnavailable, { path: item.data.path })
                : undefined;

            return <div
                title={unavailableTitle || item.data.name}
                className={classNames(
                    "list-item", {
                    "selected": isSelected,
                    "repo": isRepoItem(item),
                    "unavailable": isUnavailable,
                })}
                onClick={() => {
                    setSelectedItemPath(item.fullPath);
                    onSelect(item);
                }}
            >
                <span className="item-part no-shrink">
                    <span
                        style={{
                            visibility: (childrenMap[item.fullPath] || (isListItem(item) && item.data.hasChildren)) && !isUnavailable ? "visible" : "hidden",
                            width: `${(item.depth + 1) * TREE_INDENT_PX}px`,
                            display: "inline-flex",
                            justifyContent: "flex-end",
                        }}
                        onClick={(e) => {
                            if ((childrenMap[item.fullPath] || (isListItem(item) && item.data.hasChildren)) && !isUnavailable) {
                                e.stopPropagation();
                                toggleItem(item);
                            }
                        }}
                    >
                        {isExpanded ? <Icon glyph="fa-minus-square-o" /> : <Icon glyph="fa-plus-square-o" />}
                    </span>
                </span>
                {isUnavailable && (
                    <span className="item-part no-shrink unavailable-icon">
                        <Icon glyph="fa-exclamation-triangle" />
                    </span>
                )}
                <span className="item-part">
                    {item.data.name}
                </span>
            </div>;
        }
    };

    return <div className="tree" ref={setTreeContainer}>
        <VirtualList
            container={treeContainer || undefined}
            items={renderedTree}
            renderItem={(item: RenderItem) => {
                return renderItem(item);
            }}
            scrollToIndex={0}
        />
    </div>;
});
