import { FormItemType, ItemData, NodeItem } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { FormItem } from "./formItems";

export interface EditItem {
    itemTypeId?: number;
    itemSpecId?: number;
    itemObjectId?: number;
    position?: number;
    data?: ItemData;
    undefined?: boolean;
    readOnly?: boolean;
    nodeId?: number;
    nodeVersion?: number;
    inhibited?: boolean;
}

export interface ViewDescItemGroups {
  group: DescItemGroup;
  descItemTypes: {
    typeRef: DescItemTypeRef;
    typeForm?: FormItemType;
    typeWidth: number;
    // type: DescItemTypeMix;
    descItems: NodeItem[];
  }[];
}

export interface ViewDescItemGroupsLocal {
  group: DescItemGroup;
  descItemTypes: {
    typeRef: DescItemTypeRef;
    typeForm?: FormItemType;
    typeWidth: number;
    // type: DescItemTypeMix;
    descItems: FormItem[];
  }[];
}
