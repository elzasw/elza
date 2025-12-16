import { DataInteger, DataType } from "elza-api";
import { DescItemProps } from "./types";

export function DescItemInt({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Int) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataInteger;

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? "Výjimka" : data.integerValue}
    </div>
  );
}
