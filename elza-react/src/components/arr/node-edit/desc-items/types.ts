import { FormItemType, NodeItem } from "elza-api";
import { DescItemTypeRef } from "typings/store";

export interface DescItemProps {
  item: NodeItem;
  nodeId: number;
  onChange: (item: NodeItem, specId?: number) => Promise<void>;
  isDisabled?: boolean;
  typeForm: FormItemType;
  typeRef: DescItemTypeRef;
  selectedSpecId?: number;
}
