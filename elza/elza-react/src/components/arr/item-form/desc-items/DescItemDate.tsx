import { Input } from "@fluentui/react-components";
import { DataDate, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useStyles } from "./styles";

interface Props extends DescItemProps {
  onChange: (item: NodeItemDate) => Promise<void>;
}

interface NodeItemDate extends NodeItem {
  data: DataDate;
}

export function DescItemDate({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Date && !item.undefined) {
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

  const data = item.data as DataDate;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data?.value, item);

  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
      await onChange({
        ...item,
        data: {
          ...item.data,
          value,
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
    <div className={styles.descItemContainer}>
      <Input
        type="date"
        size={compact ? "small" : "medium"}
        disabled={isDisabled}
        value={item.undefined ? "" : (value || "").toString()}
        placeholder={item.undefined ? formatMessage(commonMessages.undefined) : undefined}
        onChange={handleInputChange}
        onBlur={() => handleChange()}
        style={{
          flex: 1,
          minWidth: "60px",
          textDecoration: item.inhibited ? "line-through" : undefined,
        }}
      />
      <ConflictValue
        value={value?.toString()}
        conflictValue={conflictValue?.toString()}
        isDirty={isDirty}
        isValid={true}
        onResolve={resolveConflict}
      >
        {(conflictValue) => (
          <Input
            type="date"
            size={compact ? "small" : "medium"}
            className={styles.conflictTextarea}
            value={conflictValue}
            readOnly={true}
          />
        )}
      </ConflictValue>
      {isDirty && <EditStateDisplay />}
    </div>
  );
}
