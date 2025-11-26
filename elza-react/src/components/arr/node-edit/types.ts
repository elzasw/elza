import { FormItemType, NodeItem } from "elza-api";
import { DescItemGroup, DescItemTypeRef } from "typings/store";

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