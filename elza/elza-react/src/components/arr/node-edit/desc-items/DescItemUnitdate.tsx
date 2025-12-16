import { Input } from "@fluentui/react-components";
import {
  convertToEstimateWithConfirmation,
  validateUnitDate,
} from "components/registry/field/UnitdateField";
import { DataType, DataUnitdate, NodeItem } from "elza-api";
import { useMemo } from "react";
import { useAppThunkDispatch } from "utils/hooks";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";

interface Props extends DescItemProps {
  onChange: (item: NodeItemUnitdate) => Promise<void>;
}

interface NodeItemUnitdate extends NodeItem {
  data: DataUnitdate;
}

export function DescItemUnitdate({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
}: Props) {
  if (
    item.data &&
    item.data?.dataType !== DataType.Unitdate &&
    !item.undefined
  ) {
    throw "Incorrect data type";
  }

  const dispatch = useAppThunkDispatch();

  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined || isInherited || item.inhibited || _isDisabled;

  const data = item.data as DataUnitdate;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data?.value, item);

  const {valid: isValid, message: validationMessage} = useMemo(() => {
    return validateUnitDate(value);
  }, [value])

  async function handleChange(force?: boolean) {
    if (
      value &&
      initialValue !== value &&
      (!conflictValue || force) &&
      isValid
    ) {
      const estimate = await convertToEstimateWithConfirmation(value, dispatch);
      console.log("#diu - estimate", estimate, value);
      await onChange({
        ...item,
        data: {
          ...item.data,
          value: estimate,
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
  }: React.ChangeEvent<HTMLInputElement>) {
    setValue(currentTarget.value);
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
        onChange={handleInputChange}
        onBlur={() => handleChange()}
        style={{
          flex: 1,
          minWidth: "60px",
          borderColor: isValid ? undefined : "var(--color-red)",
          textDecoration: item.inhibited ? "line-through" : undefined,
        }}
      />
      <div style={{ color: "var(--color-red)" }}>{validationMessage}</div>
      <ConflictValue
        value={value?.toString()}
        conflictValue={conflictValue?.toString()}
        isDirty={isDirty}
        isValid={isValid}
        onResolve={resolveConflict}
      >
        {(conflictValue) => (
          <Input
            style={{ borderColor: "var(--color-red)" }}
            value={conflictValue}
            readOnly={true}
          />
        )}
      </ConflictValue>
      {isDirty && <EditStateDisplay />}
    </div>
  );
}
