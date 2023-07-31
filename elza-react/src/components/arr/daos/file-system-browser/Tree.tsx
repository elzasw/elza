import React, { useRef, useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import { VirtualList } from 'components/shared';
import { Api } from 'api';
import classNames from 'classnames';
import { FsRepo, FsItem, FsItemType } from 'elza-api';
import { i18n, Icon } from 'components/shared';
import "./FileSystemBrowser.scss"
import { RenderItem, isLastKeyItem, isRepoItem, isListItem, RenderItemType } from './types';
import { extractRepoIdFromFullPath } from './extractRepoIdFromFullPath';

interface TreeProps {
    onSelect: (item: RenderItem) => void;
    onExpandChange?: (itemFullPath: string, expand: boolean) => void;
    expandedItems?: Record<string, boolean>;
    fundId: number;
    selectedItemPath?: string;
    childrenMap?: Record<string, boolean>;
    repos?: FsRepo[];
}

export interface TreeExposedFunctions {
    toggleExpand: (fullPath: string, expandState?: boolean) => void;
}

const TREE_INDENT_PX = 10;

export const Tree = forwardRef<TreeExposedFunctions, TreeProps>(({
    fundId,
    onSelect,
    onExpandChange = () => { return; },
    selectedItemPath: _selectedItemPath,
    childrenMap = {},
    repos = [],
}: TreeProps, ref) => {
    const treeContainerRef = useRef<HTMLDivElement>(null);
    const [workingTree, setWorkingTree] = useState<RenderItem[]>(repos.map((dataItem) => ({
        type: RenderItemType.Repo,
        data: dataItem,
        depth: 0,
        parentFullPath: null,
        fullPath: dataItem.fsRepoId.toString(),
    })));
    const [renderedTree, setRenderedTree] = useState<RenderItem[]>([]);
    const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({});
    const [selectedItemPath, setSelectedItemPath] = useState<string | undefined>(_selectedItemPath);

    useImperativeHandle(ref, () => ({
        toggleExpand: (fullPath, expandState) => {
            const item = workingTree.find((item) => item.fullPath === fullPath);
            if (item) {
                toggleItem(item, expandState)
            }
        }
    }))

    const loadLevel = async (fullPath: string, lastKey: string | undefined, depth: number = 0, filter?: FsItemType) => {
        const [repoId, path] = extractRepoIdFromFullPath(fullPath);
        const { data: items } = await Api.funds.fundFsRepoItems(fundId, parseInt(repoId, 10), filter, path, lastKey);

        const itemLevel: RenderItem[] = items.items.map((item) => {
            const extendedItemBase: FsItem = {
                ...item,
            }
            return {
                type: RenderItemType.Item,
                data: extendedItemBase,
                depth,
                parentFullPath: fullPath || null,
                fullPath: `${fullPath}/${item.name}`,
            }
        })
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
            })
        }
        return itemLevel;
    }

    const loadMoreItems = async (path: string, lastKey: string, index: number, depth: number) => {
        const itemsEx: RenderItem[] = await loadLevel(path, lastKey, depth, FsItemType.Folder);

        const newRepoItems = [...workingTree];
        newRepoItems.splice(index, 1, ...itemsEx);

        setWorkingTree(newRepoItems);
    }

    const toggleItem = (item: RenderItem, forcedExpandState?: boolean) => {
        const expandState = forcedExpandState == undefined ? !expandedItems[item.fullPath] : forcedExpandState;
        if (expandState) {
            expandItem(item);
        }
        else {
            collapseItem(item);
        }
    }

    const expandItem = async (item: RenderItem) => {
        onExpandChange(item.fullPath, true);

        if (expandedItems[item.fullPath] != undefined) {
            setExpandedItems({ ...expandedItems, [item.fullPath]: true });
            return;
        }

        const selectedItemIndex = workingTree.findIndex((_item) => { return item.fullPath === _item.fullPath })

        if (isListItem(item) || isRepoItem(item)) {
            const itemsEx = await loadLevel(item.fullPath, undefined, item.depth + 1, FsItemType.Folder);
            const newWorkingTree = [...workingTree];
            newWorkingTree.splice(selectedItemIndex + 1, 0, ...itemsEx)

            setWorkingTree(newWorkingTree);
            setExpandedItems({ ...expandedItems, [item.fullPath]: true })
        }
    }

    const collapseItem = (item: RenderItem) => {
        onExpandChange(item.fullPath, false);
        setExpandedItems({ ...expandedItems, [item.fullPath]: false })
    }

    const renderItem = (item: RenderItem) => {
        if (isLastKeyItem(item)) {
            return <div
                className="list-item"
                onClick={() => {
                    const index = workingTree.findIndex((workingTreeItem) => {
                        return workingTreeItem.fullPath === item.fullPath;
                    })
                    loadMoreItems(item.parentFullPath || "", item.data.lastKey, index, item.depth)
                }} // needs proper index
            >
                <span style={{
                    // paddingLeft: `${item.depth * TREE_INDENT_PX}px` ,
                    width: `${(item.depth + 1) * TREE_INDENT_PX}px`,
                    display: "inline-block",
                }} />
                {i18n("arr.daos.fileSystem.loadMore")}
            </div>
        }
        if (isRepoItem(item) || isListItem(item)) {
            const isExpanded = expandedItems[item.fullPath];
            const isSelected = item.fullPath === selectedItemPath;

            return <div
                className={classNames(
                    "list-item", {
                    "selected": isSelected,
                    "repo": isRepoItem(item),
                })}
                onClick={() => {
                    setSelectedItemPath(item.fullPath);
                    onSelect(item);
                }}
            >
                <span
                    style={{
                        // paddingLeft: `${item.depth * TREE_INDENT_PX}px`,
                        visibility: childrenMap[item.fullPath] ? "visible" : "hidden",
                        width: `${(item.depth + 1) * TREE_INDENT_PX}px`,
                        display: "inline-flex",
                        justifyContent: "flex-end",
                    }}
                    onClick={(e) => {
                        if (childrenMap[item.fullPath]) {
                            e.stopPropagation();
                            toggleItem(item);
                        }
                    }}
                >
                    {isExpanded ? <Icon glyph="fa-minus-square-o" /> : <Icon glyph="fa-plus-square-o" />}
                </span>
                &nbsp;
                {item.data.name}
            </div>
        }
    }

    useEffect(() => {
        setWorkingTree(repos.map((dataItem) => ({
            type: RenderItemType.Repo,
            data: dataItem,
            depth: 0,
            parentFullPath: null,
            fullPath: dataItem.fsRepoId.toString(),
        })));
        setExpandedItems({});
    }, [repos.length])

    // Working tree/expanded change effect
    useEffect(() => {
        const renderedTree: RenderItem[] = [];
        let depthToBeClosed: number | undefined = undefined;

        workingTree.forEach((item) => {
            if (depthToBeClosed != undefined && item.depth >= depthToBeClosed) {
                return;
            } else {
                depthToBeClosed = undefined;
            }

            if (isListItem(item) || isRepoItem(item)) {
                if (expandedItems[item.fullPath] != undefined && !expandedItems[item.fullPath]) {
                    depthToBeClosed = item.depth + 1;
                }

                renderedTree.push(item)
            }
            else {
                renderedTree.push(item)
            }
        })

        setRenderedTree(renderedTree);
    }, [workingTree, expandedItems])

    useEffect(() => {
        setSelectedItemPath(_selectedItemPath);
    }, [_selectedItemPath])

    return <div className="tree" ref={treeContainerRef}>
        <VirtualList
            container={treeContainerRef.current || undefined}
            items={renderedTree}
            renderItem={(item: RenderItem) => {
                return renderItem(item);
            }}
            scrollToIndex={0}
        />
    </div>
})
