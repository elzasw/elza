import { DataDecimal, DataType } from "elza-api";
import { DescItemProps } from "./types";

export function DescItemDecimal({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Decimal) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataDecimal;

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
