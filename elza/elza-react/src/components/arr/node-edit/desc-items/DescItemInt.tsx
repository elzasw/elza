import { Input } from "@fluentui/react-components";
import { DataInteger, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";
import { fromDuration, normalizeDuration, normalizeDurationLength, toDuration } from "components/validate";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";

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
  typeRef,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Int && !item.undefined) {
    throw "Incorrect data type";
  }

  const { formatMessage } = useIntl();
  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;
  const data = item.data as DataInteger;

  const isDuration = typeRef.viewDefinition === "DURATION";
    const _initialValue = isDuration && data?.integerValue != undefined ? toDuration(data?.integerValue) : data?.integerValue;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<number | string>(_initialValue, item);

  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
        let integerValue = value;
        if(isDuration){
            integerValue = parseInt(fromDuration(normalizeDurationLength(value)));
        }
        if (typeof integerValue === 'number') {
            await onChange({
                ...item,
                data: { ...item.data, integerValue },
            });
        }
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
      if(isDuration){
          const _value = normalizeDuration(currentTarget.value);
          setValue(_value);
      }
      else {
          const int = parseInt(currentTarget.value);
          if (isNaN(int) && currentTarget.value !== "") {
            return;
          }

          const _int = isNaN(int) ? null : int;
          setValue(_int);
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
      <Input
        size={compact ? "small" : "medium"}
        disabled={isDisabled}
        value={item.undefined ? formatMessage(commonMessages.undefined) : (value || "").toString()}
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
        {(conflictValue) => <Input size={compact ? "small" : "medium"} value={conflictValue} readOnly={true} />}
      </ConflictValue>
      {isDirty && <EditStateDisplay />}
    </div>
  );
}
