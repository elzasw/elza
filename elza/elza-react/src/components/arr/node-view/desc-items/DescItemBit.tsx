import { DataBit, DataType } from "elza-api";
import { FormattedMessage } from "react-intl";
import { DescItemProps } from "./types";
import { globalMessages } from "components/shared/lang";

export function DescItemBit({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Bit) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const data = item.data as DataBit;

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? (
        <FormattedMessage {...globalMessages.undefined} />
      ) : data.bitValue ? (
        <FormattedMessage {...globalMessages.yes} />
      ) : (
        <FormattedMessage {...globalMessages.no} />
      )}
    </div>
  );
}
