import {RulItemTypeType} from '../api/RulItemTypeType';
import { ApAccessPointVO } from 'api';

export interface DescItem<T = unknown> {
    "@class": string;
    id?: number;
    value?: T;
    descItemObjectId?: number;
    position?: number;

    /** CLIENT ATTRS */
    formKey?: string;
    _uid?: string | number;
    undefined?: boolean;
    prevValue?: T;
    hasFocus: boolean;
    touched: boolean;
    visited: boolean;
    saving: boolean;
    error: {hasError: boolean; value?: string};
    addedByUser: boolean;

    //
    itemType: number;
    prevDescItemSpecId?: number;
    descItemSpecId?: number;
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

export interface DescItemType {
    cal: number;
    calSt: number;
    descItemSpecsMap: {[key: number]: any};
    descItems: DescItem[];
    favoriteSpecIds: number[];
    group: string;
    hasFocus: boolean;
    id: number;
    ind: number;
    rep: number;
    specs: any[];
    type: RulItemTypeType;
    width: 1;
}

export interface DescItemGroup {
    code: string;
    descItemTypes: DescItemType[];
}

export interface ItemSpec {
    itemType: number;
    type: RulItemTypeType;
}
