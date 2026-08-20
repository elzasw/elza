import { DataType, NodeItem } from "elza-api";
import { useAppSelector } from "utils/hooks/useAppSelector";
import { DescItemSpec } from "./DescItemSpec";
import { DescItemProps } from "./types";

interface Props extends DescItemProps {
  onChange: (item: NodeItem, specId: number) => Promise<void>;
}

export function DescItemEnum({
  item,
  onChange,
  typeForm,
  nodeId,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Enum && !item.undefined) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;

  const typeRef = useAppSelector(
    ({ refTables }) => refTables.descItemTypes.itemsMap[item.itemTypeId],
  );

  function handleChange(itemSpecId: number) {
    onChange(item, itemSpecId);
  }

  return (
    <DescItemSpec
      isDisabled={isDisabled}
      isInhibited={item.inhibited}
      isUndefined={item.undefined}
      typeRef={typeRef}
      typeForm={typeForm}
      value={item.itemSpecId}
      onChange={handleChange}
      autoSize={false}
      isSpec={false}
      labelSource="name"
      compact={compact}
    />
  );
}
