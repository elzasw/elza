import { Input } from "@fluentui/react-components";
import { DataDecimal, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";

interface Props extends DescItemProps {
  onChange: (item: NodeItemDecimal) => Promise<void>;
}

interface NodeItemDecimal extends NodeItem {
  data: DataDecimal;
}

export function DescItemDecimal({ item, onChange, nodeId }: Props) {
  if (item.data?.dataType !== DataType.Decimal) {
    throw "Incorrect data type";
  }

  const isInherited = item.nodeId !== nodeId;
  const isDisabled = item.undefined || isInherited || item.inhibited;
  const data = item.data as DataDecimal;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<number | string>(data?.value, item);

  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
      const decimalValue = parseFloat(value.toString());
      if (isNaN(decimalValue)) {
        return;
      }

      await onChange({
        ...item,
        data: { ...item.data, value: decimalValue },
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
    const normalizedValue = currentTarget.value.replace(",", ".");

    const skipParse = normalizedValue.endsWith(".");
    if (skipParse) {
      setValue(normalizedValue);
      return;
    }

    const decimal = parseFloat(normalizedValue);
    if (isNaN(decimal) && currentTarget.value !== "") {
      return;
    }

    const _decimal = isNaN(decimal) ? null : decimal;
    setValue(_decimal);
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
