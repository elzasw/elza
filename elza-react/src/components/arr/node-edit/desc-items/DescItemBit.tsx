import { Switch } from "@fluentui/react-components";
import { DataBit, DataType, NodeItem } from "elza-api";
import { useState } from "react";
import { DescItemProps } from "./types";

interface Props extends DescItemProps {
  onChange: (item: NodeItemBit) => Promise<void>;
}

interface NodeItemBit extends NodeItem {
  data: DataBit;
}

export function DescItemBit({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Bit && !item.undefined) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;
  const data = item.data as DataBit;

  const [value, setValue] = useState<boolean>(data.bitValue);

  async function handleChange(value: boolean) {
    setValue(value);

    if (value != undefined) {
      await onChange({
        ...item,
        data: {
          ...item.data,
          bitValue: value,
        },
      });
    }
  }

  return (
    <div
      style={{
        display: "flex",
        flex: 1,
        position: "relative",
        flexDirection: "column",
      }}
    >
      <Switch
        disabled={isDisabled}
        checked={value}
        onChange={(_e, data) => handleChange(data.checked)}
      />
    </div>
  );
}
