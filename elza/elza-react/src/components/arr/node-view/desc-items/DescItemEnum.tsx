import { DataType } from "elza-api";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { DescItemProps } from "./types";
import { FormattedMessage } from "react-intl";
import { messages as commonMessages } from "components/arr/item-form/desc-items/commonMessages";

export function DescItemEnum({ item, nodeId }: DescItemProps) {
  if (item.data?.dataType !== DataType.Enum) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;

  const specs = useAppSelector(
    ({ refTables }) =>
      refTables.descItemTypes.itemsMap[item.itemTypeId].descItemSpecs,
  );
  const spec = specs.find(({ id }) => item.itemSpecId === id);

  return (
    <div
      style={{
        textDecoration: item.inhibited ? "line-through" : undefined,
        opacity: isInherited ? 0.5 : undefined,
      }}
    >
      {item.undefined ? <FormattedMessage {...commonMessages.undefined} /> : spec?.name}
    </div>
  );
}
