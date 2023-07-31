import { FsRepo, FsItem } from 'elza-api';

export enum RenderItemType {
    Item,
    LastKey,
    Repo,
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

export type RenderListItem = RenderItemBase<FsItem>;
export type RenderLastKeyItem = RenderItemBase<LastKeyItem>;
export type RenderRepoItem = RenderItemBase<FsRepo>;

export type RenderItem = RenderListItem | RenderLastKeyItem | RenderRepoItem;

export const isLastKeyItem = (item: RenderItem): item is RenderLastKeyItem => {
    return item.type === RenderItemType.LastKey;
}

export const isListItem = (item: RenderItem): item is RenderListItem => {
    return item.type === RenderItemType.Item;
}

export const isRepoItem = (item: RenderItem): item is RenderRepoItem => {
    return item.type === RenderItemType.Repo;
}
