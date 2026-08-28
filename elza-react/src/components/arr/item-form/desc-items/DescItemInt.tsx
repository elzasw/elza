import { Input } from "@fluentui/react-components";
import { DataInteger, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";
import { fromDuration, normalizeDuration, normalizeDurationLength, toDuration } from "components/validate";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useStyles } from "./styles";

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
  const styles = useStyles();
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
    if (initialValue !== value && (!conflictValue || force)) {
        const isEmpty = value == null || value === "";
        let integerValue: number | undefined = undefined;
        if (!isEmpty) {
            // The non-duration input already stores a number; DURATION keeps the formatted text.
            integerValue = isDuration
                ? parseInt(fromDuration(normalizeDurationLength(value)))
                : (value as number);
        }
        if (isEmpty || typeof integerValue === 'number') {
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
    <div className={styles.descItemContainerWithWidth}>
      <Input
        size={compact ? "small" : "medium"}
        disabled={isDisabled}
        value={item.undefined ? formatMessage(commonMessages.undefined) : (value || "").toString()}
        style={{
          flex: 1,
          minWidth: "60px",
          fontSize: "1em",
          textDecoration: item.inhibited ? "line-through" : undefined,
        }}
        onChange={handleInputChange}
        onBlur={() => handleChange()}
      />
      <ConflictValue
        conflictValue={conflictValue?.toString()}
        onResolve={resolveConflict}
      >
        {(conflictValue) => <Input size={compact ? "small" : "medium"} value={conflictValue} readOnly={true} style={{ fontSize: "1em" }} />}
      </ConflictValue>
      {isDirty && <EditStateDisplay />}
    </div>
  );
}
