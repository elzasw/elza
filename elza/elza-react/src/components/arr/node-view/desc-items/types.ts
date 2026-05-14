import { FormItemType, NodeItem } from "elza-api";
import { DescItemTypeRef } from "typings/store";

export interface DescItemProps {
  item: NodeItem;
  nodeId: number;
  // isDisabled?: boolean;
  typeForm?: FormItemType;
  typeRef: DescItemTypeRef;
  selectedSpecId?: number;
}
