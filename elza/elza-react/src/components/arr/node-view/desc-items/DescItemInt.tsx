import { DataInteger, DataType } from "elza-api";
import { DescItemProps } from "./types";
import { toDuration } from "components/validate";

export function DescItemInt({ item, nodeId, typeRef }: DescItemProps) {
  if (item.data?.dataType !== DataType.Int) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataInteger;

  const isDuration = typeRef.viewDefinition === "DURATION";
  const value = isDuration && data?.integerValue != undefined ? toDuration(data?.integerValue) : data?.integerValue;

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
