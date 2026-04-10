import { DataType, DataUnitid } from "elza-api";
import { DescItemProps } from "./types";

export function DescItemUnitid({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Unitid) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataUnitid;

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? "Výjimka" : data.unitId}
    </div>
  );
}
