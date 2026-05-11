import { DescItem, DescItemFromServer, DescItemGroup, DescItemType, DescItemTypeFromServer } from "typings/DescItem";
import { DescItemTypeRef } from "typings/store";

export interface ParentInfo {
    id: number;
    uuid: string;
    version: number;
}

export interface NodeData {
    arrPerm?: boolean;
    descItems?: DescItemFromServer<unknown>[];
    itemTypes?: DescItemTypeFromServer[];
    parent?: ParentInfo;
}

export interface FormData {
    descItemGroups?: DescItemGroup[];
}

export interface ValueLocationIndex {
    descItemGroupIndex: number;
    descItemTypeIndex: number;
    descItemIndex: number;
}

export interface ValueLocation {
    descItemGroup: DescItemGroup;
    descItemType: DescItemType;
    descItem: DescItem;
}

export interface InfoGroup {
    code: string;
    name: string;
    position: number;
    types: unknown[];
}

export interface SubNodeForm {
    data?: NodeData;
    dirty?: boolean;
    fetched?: boolean;
    fetchingId?: number;
    formData?: FormData;
    getLoc?: (state: SubNodeForm, valueLocation: ValueLocationIndex) => ValueLocation;
    infoGroups?: InfoGroup[];
    infoGroupsMap?: Record<string, InfoGroup>;
    infoTypesMap?: Record<number, unknown>;
    isFetching?: boolean;
    needClean?: boolean;
    nodeId?: number;
    refTypesMap?: Record<string, DescItemTypeRef>;
    unusedItemTypeIds?: unknown;
    versionId?: number;
}

export interface SubNodeFormCache {
    dataCache?: Record<number, unknown>;
    isFetching?: boolean;
}

