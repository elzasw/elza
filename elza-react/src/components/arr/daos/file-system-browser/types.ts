import { FsRepo, FsItem } from 'elza-api';

export enum RenderItemType {
    Item,
    LastKey,
    Repo,
    Error,
}

export interface RenderItemBase<T> {
    type: RenderItemType;
    data: T;
    depth: number;
    parentFullPath: string | null;
    fullPath: string;
}

export interface LastKeyItem {
    lastKey: string;
    path: string;
}

/** Synthetic placeholder rendered below a tree node whose expand failed. */
export interface ErrorItem {
    /** fullPath of the node whose expand failed; clicking retries it. */
    retryPath: string;
}

export type RenderListItem = RenderItemBase<FsItem>;
export type RenderLastKeyItem = RenderItemBase<LastKeyItem>;
export type RenderRepoItem = RenderItemBase<FsRepo>;
export type RenderErrorItem = RenderItemBase<ErrorItem>;

export type RenderItem = RenderListItem | RenderLastKeyItem | RenderRepoItem | RenderErrorItem;

export const isLastKeyItem = (item: RenderItem): item is RenderLastKeyItem => {
    return item.type === RenderItemType.LastKey;
}

export const isListItem = (item: RenderItem): item is RenderListItem => {
    return item.type === RenderItemType.Item;
}

export const isRepoItem = (item: RenderItem): item is RenderRepoItem => {
    return item.type === RenderItemType.Repo;
}

export const isErrorItem = (item: RenderItem): item is RenderErrorItem => {
    return item.type === RenderItemType.Error;
}
