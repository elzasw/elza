import { Input } from "@fluentui/react-components";
import { DataDecimal, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useStyles } from "./styles";

interface Props extends DescItemProps {
  onChange: (item: NodeItemDecimal) => Promise<void>;
}

interface NodeItemDecimal extends NodeItem {
  data: DataDecimal;
}

export function DescItemDecimal({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (item.data && item.data.dataType !== DataType.Decimal && !item.undefined) {
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
        value={value?.toString()}
        conflictValue={conflictValue?.toString()}
        isDirty={isDirty}
        onResolve={resolveConflict}
      >
        {(conflictValue) => <Input size={compact ? "small" : "medium"} value={conflictValue} readOnly={true} style={{ fontSize: "1em" }} />}
      </ConflictValue>
      {isDirty && <EditStateDisplay />}
    </div>
  );
}
