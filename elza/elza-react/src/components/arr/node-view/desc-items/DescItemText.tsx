import { DataText, DataType } from "elza-api";
import { DescItemProps } from "./types";

export function DescItemText({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Text) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataText;

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? "Výjimka" : data.textValue}
    </div>
  );
}
