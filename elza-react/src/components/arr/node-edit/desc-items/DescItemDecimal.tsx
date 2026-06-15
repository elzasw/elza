import { Input } from "@fluentui/react-components";
import { DataDecimal, DataType, NodeItem } from "elza-api";
import { useMemo } from "react";
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

  const { formatMessage, formatNumber, locale } = useIntl();
  const styles = useStyles();
  const isInherited = item.nodeId !== nodeId;
  const isDisabled =
    item.undefined ||
    isInherited ||
    item.inhibited ||
    item.readOnly ||
    _isDisabled;
  const data = item.data as DataDecimal;

  // The locale's decimal separator (e.g. "." for en, "," for cs).
  const decimalSeparator = useMemo(
    () => new Intl.NumberFormat(locale).formatToParts(1.1).find(({ type }) => type === "decimal")?.value ?? ".",
    [locale],
  );

  function formatValue(numericValue: number) {
    return formatNumber(numericValue, { maximumFractionDigits: 20, useGrouping: false });
  }

  // State holds the locale-formatted display string; conversion to a number happens only on save.
  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data?.value != null ? formatValue(data.value) : null, item);

  function toNumber(displayValue: string) {
    return parseFloat(displayValue.split(decimalSeparator).join("."));
  }

  async function handleChange(force?: boolean) {
    if (value != null && value !== "" && initialValue !== value && (!conflictValue || force)) {
      const decimalValue = toNumber(value);
      if (isNaN(decimalValue)) {
        return;
      }

      // Normalize to the canonical formatted form (e.g. "0,20" -> "0,2") so it matches the
      // value the server returns, avoiding a spurious dirty/conflict state.
      const normalized = formatValue(decimalValue);
      if (normalized !== value) {
        setValue(normalized);
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
    // Accept a dot as the decimal separator too, normalizing it to the locale one for display.
    const input = decimalSeparator === "." ? currentTarget.value : currentTarget.value.replace(".", decimalSeparator);
    // Allow only a partial decimal in the locale notation: optional leading '-',
    // digits, and at most one decimal separator.
    const separator = decimalSeparator.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const pattern = new RegExp(`^-?\\d*(${separator}\\d*)?$`);
    if (input !== "" && !pattern.test(input)) {
      return;
    }
    setValue(input);
  }

  return (
    <div className={styles.descItemContainerWithWidth}>
      <Input
        size={compact ? "small" : "medium"}
        disabled={isDisabled}
        value={item.undefined ? formatMessage(commonMessages.undefined) : (value ?? "")}
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
