import { Input } from "@fluentui/react-components";
import { DataString, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { useValueManager } from "./utils";
import { DescItemProps } from "./types";
import { TextareaAutosize } from "./inputs/TextareaAutosize";

interface Props extends DescItemProps {
  onChange: (item: NodeItemString) => Promise<void>;
}

interface NodeItemString extends NodeItem {
  data: DataString;
}

export function DescItemString({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  typeWidth,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.String && !item.undefined) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined || isInherited || item.inhibited || _isDisabled;
  const data = item.data as DataString;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data?.stringValue, item);

  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
      await onChange({
        ...item,
        data: {
          ...item.data,
          stringValue: value,
        },
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
  }: React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>) {
    const _value = currentTarget.value.replace(/\n/g, "");
    if (_value != value && (value || _value?.length > 0)) {
      setValue(_value);
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
      {typeWidth === 0 ? (
        <TextareaAutosize
          resize="none"
          disabled={isDisabled}
          value={item.undefined ? "Výjimka" : value?.toString() || ""}
          onChange={handleInputChange}
          onBlur={() => handleChange()}
          style={{
            flex: 1,
            minWidth: "60px",
            textDecoration: item.inhibited ? "line-through" : undefined,
          }}
        />
      ) : (
        <Input
          disabled={isDisabled}
          value={item.undefined ? "Výjimka" : value?.toString()}
          onChange={handleInputChange}
          onBlur={() => handleChange()}
          style={{
            flex: 1,
            minWidth: "60px",
            textDecoration: item.inhibited ? "line-through" : undefined,
          }}
        />
      )}
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
