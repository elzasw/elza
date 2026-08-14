import { FormItemType, NodeItem } from "elza-api";
import { DescItemTypeRef } from "typings/store";

export interface DescItemProps {
  item: NodeItem;
  nodeId: number;
  // isDisabled?: boolean;
  typeForm?: FormItemType;
  typeRef: DescItemTypeRef;
  selectedSpecId?: number;
  /** CSV export of the whole table value; provided only for JSON_TABLE items in read-only views. */
  onExportCsv?: (item: NodeItem) => void;
}
