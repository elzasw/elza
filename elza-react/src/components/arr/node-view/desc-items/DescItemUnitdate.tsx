import { DataType, DataUnitdate } from "elza-api";
import { DescItemProps } from "./types";

export function DescItemUnitdate({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Unitdate) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataUnitdate;

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? "Výjimka" : data.value}
    </div>
  );
}
