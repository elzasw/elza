import { FormItemType, NodeItem } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";
import { FormItem } from "./hooks";

export interface ViewDescItemGroups {
  group: DescItemGroup;
  descItemTypes: {
    typeRef: DescItemTypeRef;
    typeForm: FormItemType;
    typeWidth: number;
    // type: DescItemTypeMix;
    descItems: NodeItem[];
  }[];
}

export interface ViewDescItemGroupsLocal {
  group: DescItemGroup;
  descItemTypes: {
    typeRef: DescItemTypeRef;
    typeForm: FormItemType;
    typeWidth: number;
    // type: DescItemTypeMix;
    descItems: FormItem[];
  }[];
}
