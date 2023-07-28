import React, { useRef, useState, useEffect } from 'react';
import { VirtualList } from 'components/shared';
import { Api } from 'api';
import classNames from 'classnames';
import { FsRepo, FsItem, FsItemType } from 'elza-api';
import { i18n, Icon } from 'components/shared';
import { humanFileSize } from 'components/Utils.jsx';
import "./FileSystemBrowser.scss"
import { Tree, TreeExposedFunctions } from './Tree';
import { RenderItem, RenderItemType, isListItem, isLastKeyItem } from './types';
import { extractRepoIdFromFullPath } from './extractRepoIdFromFullPath';

interface Props {
    fundId: number;
    onSelect?: (item?: FsItem, fullPath?: string) => void;
}

export const FileSystemBrowser = ({
    fundId,
    onSelect = () => { return; }
}: Props) => {
    const levelContainerRef = useRef<HTMLDivElement>(null);
    const treeRef = useRef<TreeExposedFunctions>(null);

    const [levelList, setLevelList] = useState<RenderItem[]>([]);
    const [selectedTreeItemPath, setSelectedTreeItem] = useState<string>();
    const [selectedListItem, setSelectedListItem] = useState<string>();
    const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({});
    const [childrenMap, setChildrenMap] = useState<Record<string, boolean>>({});
    const [repos, setRepos] = useState<FsRepo[]>([]);

    const loadLevel = async (fullPath: string, lastKey: string | undefined, depth: number = 0, filter?: FsItemType) => {
        const [repoId, path] = extractRepoIdFromFullPath(fullPath)
        const { data: items } = await Api.funds.fundFsRepoItems(fundId, parseInt(repoId, 10), filter, path, lastKey);
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
            setChildrenMap({ ...childrenMap, [fullPath]: true });
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
                {i18n("arr.daos.fileSystem.loadMore")}
            </div>
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
                {item.data.itemType === FsItemType.Folder ? <Icon glyph="fa-folder" /> : <Icon glyph="fa-file" />}
                <span className="item-part left">
                    {item.data.name}
                </span>
                <span className="spacer" />
                <span className="item-part right">
                    {item.data.size != null && humanFileSize(item.data.size)}
                </span>
                <span className="item-part right">
                    {lastChangeDate.toLocaleDateString([], { year: "2-digit", month: "numeric", day: "numeric" })}
                    &nbsp;&nbsp;
                    {lastChangeDate.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                </span>
            </div>
        }
    }

    const loadMoreListItems = async (_path: string | undefined = undefined, lastKey: string, index: number, depth: number) => {
        if (selectedTreeItemPath) {
            const itemsEx: RenderItem[] = await loadLevel(selectedTreeItemPath, lastKey, depth);

            const newRepoItems = [...levelList];
            newRepoItems.splice(index, 1, ...itemsEx);

            setLevelList(newRepoItems);
        }
    }

    useEffect(() => {
        (async () => {
            if (selectedTreeItemPath) {
                const itemsEx = await loadLevel(selectedTreeItemPath, undefined, 0);
                setLevelList(itemsEx);
            }
        })()
    }, [selectedTreeItemPath])

    useEffect(() => {
        return () => {
            onSelect(undefined, undefined);
        }
    }, [])

    useEffect(() => {
        (async () => {
            const { data } = await Api.funds.fundFsRepos(fundId);
            setRepos(data);
        })()
    }, [])


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
        return <div className="breadcrumbs">
            {breadcrumbParts.map((breadcrumb, index) => {
                const pathParts = breadcrumb.split("/")
                return <>
                    <div className="btn" onClick={() => { setSelectedTreeItem(breadcrumb) }}>
                        {index === 0 ? repoName : pathParts[pathParts.length - 1]}
                    </div>
                    {index < breadcrumbParts.length - 1
                        && <div className="divider">
                            <Icon glyph="fa-angle-right" />
                        </div>}
                </>
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
                    <div
                        title={i18n("arr.daos.fileSystem.selectParent")}
                        className="btn"
                        onClick={handleSelectParent}>
                        <Icon glyph="fa-angle-up" />
                    </div>
                </div>
                {generateBreadcrumbs()}

            </div>
            <div className="main-container">
                <Tree
                    ref={treeRef}
                    fundId={fundId}
                    selectedItemPath={selectedTreeItemPath}
                    onSelect={(item) => { setSelectedTreeItem(item.fullPath) }}
                    expandedItems={expandedItems}
                    onExpandChange={(itemFullPath, expanded) => { setExpandedItems({ ...expandedItems, [itemFullPath]: expanded }) }}
                    childrenMap={childrenMap}
                    repos={repos}
                />
                <div
                    className="file-list"
                    ref={levelContainerRef}
                >
                    <VirtualList
                        container={levelContainerRef.current || undefined}
                        items={levelList}
                        renderItem={(item: RenderItem) => {
                            return renderListItem(item);
                        }}
                        scrollToIndex={0}
                    />
                </div>
            </div>
            {/* {selectedListItem && <div style={{ border: "var(--primary-border)", display: "flex", justifyContent: "center" }}> */}
            {/*     <img style={{ maxHeight: "200px" }} src={getImageUrl()} /> */}
            {/* </div>} */}
        </div>
    );
}
