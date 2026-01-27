import { Input } from "@fluentui/react-components";
import { DataInteger, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";

interface Props extends DescItemProps {
  onChange: (item: NodeItemInt) => Promise<void>;
}

interface NodeItemInt extends NodeItem {
  data: DataInteger;
}

export function DescItemInt({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
}: Props) {
  if (item.data?.dataType !== DataType.Int) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;
  const data = item.data as DataInteger;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<number>(data?.integerValue, item);

  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
      await onChange({
        ...item,
        data: { ...item.data, integerValue: value },
      });
      finishChange();
    }
  }

  async function resolveConflict(resetValue?: boolean) {
    if (!resetValue) {
      await handleChange(true);
    }
    resetConflict();
  }

  function handleInputChange({
    currentTarget,
  }: React.ChangeEvent<HTMLInputElement>) {
    const int = parseInt(currentTarget.value);
    if (isNaN(int) && currentTarget.value !== "") {
      return;
    }

    const _int = isNaN(int) ? null : int;
    setValue(_int);
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
      <Input
        disabled={isDisabled}
        value={item.undefined ? "Výjimka" : value?.toString()}
        style={{
          flex: 1,
          minWidth: "60px",
          textDecoration: item.inhibited ? "line-through" : undefined,
        }}
        onChange={handleInputChange}
        onBlur={() => handleChange()}
      />
      <ConflictValue
        value={value?.toString()}
        conflictValue={conflictValue?.toString()}
        isDirty={isDirty}
        onResolve={resolveConflict}
      >
        {(conflictValue) => <Input value={conflictValue} readOnly={true} />}
      </ConflictValue>
      {isDirty && <EditStateDisplay />}
    </div>
  );
}
