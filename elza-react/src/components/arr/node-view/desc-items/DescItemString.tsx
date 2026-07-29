import { DataString, DataType } from "elza-api";
import { DescItemProps } from "./types";
import { isMaskViewDefinition, maskString } from "components/arr/item-form/desc-items/maskUtils";

export function DescItemString({ item, nodeId, typeRef }: DescItemProps) {
  if (item.data?.dataType !== DataType.String) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataString;

  const mask = isMaskViewDefinition(typeRef.viewDefinition) ? typeRef.viewDefinition.mask : undefined;
    const value = mask ? maskString(data?.stringValue, mask) : data?.stringValue;

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? "Výjimka" : value}
    </div>
  );
}
