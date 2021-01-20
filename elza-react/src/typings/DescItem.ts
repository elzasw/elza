import {ApItemVO} from '../api/ApItemVO';
import {RulItemTypeType} from '../api/RulItemTypeType';

export interface DescItemPlain<T = any> extends ApItemVO {
    prevValue?: T;
    hasFocus: boolean;
    touched: boolean;
    visited: boolean;
    saving: boolean;
    value?: T;
    error: {hasError: boolean; value?: string};
    addedByUser: boolean;
    descItemObjectId?: number;

    /** CLIENT ATTRS */
    formKey?: string;
    _uid?: string | number;
    undefined?: boolean;
    position: number;

    //
    calendarTypeId?: number;
    itemType?: number;
}

export interface DescItemWithSpec<T = any> extends DescItemPlain<T> {
    prevDescItemSpecId?: number;
    descItemSpecId?: number;
}

export type DescItem = DescItemPlain | DescItemWithSpec;

export interface DescItemType<T = DescItem> {
    cal: number;
    calSt: number;
    descItemSpecsMap: {[key: number]: any};
    descItems: T[];
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
