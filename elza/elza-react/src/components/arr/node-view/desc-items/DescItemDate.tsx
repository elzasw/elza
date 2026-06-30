import { DataDate, DataType } from "elza-api";
import { FormattedDate } from "react-intl";
import { DescItemProps } from "./types";

export function DescItemDate({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Date) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataDate;
  const date = data.value ? new Date(data.value) : undefined;

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? (
        "Výjimka"
      ) : date ? (
        <FormattedDate value={date} day="2-digit" month="2-digit" year="numeric" />
      ) : null}
    </div>
  );
}
