import { ItemAvailability } from 'stores/app/accesspoint/itemFormUtils';
import { RulItemTypeType } from '../api/RulItemTypeType';
import { ApAccessPointVO } from 'api';

export interface DescItemFromServer<T> {
    "@class": string;
    id?: number;
    descItemObjectId?: number;
    position?: number;
    undefined?: boolean;
    itemTypeId: number;
    descItemSpecId?: number | null;
    readOnly?: boolean;
    fromNodeId?: number | null;
    inhibited?: boolean | null;
    record?: ApAccessPointVO;
    value?: T;
    description?: string;
    refTemplateId?: number | string | null;
    nodeId?: number;
}

export interface DescItem<T = unknown> extends DescItemFromServer<T> {
    /** CLIENT ATTRS */
    formKey?: string;
    _uid?: string | number;
    prevValue?: T;
    hasFocus: boolean;
    touched: boolean;
    visited: boolean;
    saving: boolean;
    error: { hasError: boolean; value?: string };
    addedByUser: boolean;
    prevDescItemSpecId?: number;
}

// export interface DescItemRecordRef extends DescItem<number>{
//     record: ApAccessPointVO;
// }
//
// export function isRecordRef(descItem: any): descItem is DescItemRecordRef {
//     return descItem.record != undefined;
// }
//
// export interface DescItemUriRef extends DescItem<string>{
//     nodeId?: number;
//     description?: string;
//     refTemplateId?: number;
// }
//
// export function isUriRef(descItem: any): descItem is DescItemUriRef {
//     return descItem.nodeId != undefined || descItem.description != undefined || descItem.refTemplateId != undefined;
// }

// export interface DescItemWithSpec<T = any> extends DescItemPlain<T> {
//     prevDescItemSpecId?: number;
//     descItemSpecId?: number;
// }

// export type DescItem = DescItemPlain;

export interface DescItemSpecFromServer {
    id: number;
    rep: number;
    type: number;
}

export interface ItemSpec {
    id: number;
    rep: number;
    type: ItemAvailability;
    itemType: number;
}

export interface DescItemTypeFromServer {
    cal: number;
    calSt: number;
    favoriteSpecIds: number[];
    id: number;
    ind: number;
    rep: number;
    specs: DescItemSpecFromServer[];
    type: number;
    width: number;
}

export interface DescItemType extends DescItemTypeFromServer {
    descItemSpecsMap: { [key: number]: any };
    descItems: DescItem[];
    favoriteSpecIds: number[];
    group: string;
    hasFocus: boolean;
    specs: any[];
    itemType: ItemAvailability;
}

export interface DescItemGroup {
    code: string;
    descItemTypes: DescItemType[];
    hasFocus: boolean;
    name: string;
    position: number;
    types: unknown[];
}

