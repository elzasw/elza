import { DataDecimal, DataType } from "elza-api";
import { FormattedNumber } from "react-intl";
import { DescItemProps } from "./types";
import { FormattedMessage } from "react-intl";
import { messages as commonMessages } from "components/arr/item-form/desc-items/commonMessages";

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
      {item.undefined ? (
        <FormattedMessage {...commonMessages.undefined} />
      ) : data.value != null ? (
        <FormattedNumber value={data.value} maximumFractionDigits={20} useGrouping={false} />
      ) : null}
    </div>
  );
}
