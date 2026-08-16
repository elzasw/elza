import { FormItemType, NodeItem } from "elza-api";
import { DescItemTypeRef } from "typings/store";

export interface DescItemProps {
  item: NodeItem;
  nodeId: number;
  onChange: (item: NodeItem, specId?: number) => Promise<void>;
  isDisabled?: boolean;
  typeForm?: FormItemType;
  typeRef: DescItemTypeRef;
  selectedSpecId?: number;
  typeWidth?: number;
  compact?: boolean;
  /** CSV export of the whole table value; provided by the form only for JSON_TABLE items. */
  onExportCsv?: (item: NodeItem) => void;
  /** CSV import replacing the whole table value; provided by the form only for JSON_TABLE items. */
  onImportCsv?: (item: NodeItem, file: File) => Promise<void>;
}
