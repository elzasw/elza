import { Input } from "@fluentui/react-components";
import { DataType, DataUnitid, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useStyles } from "./styles";

interface Props extends DescItemProps {
  onChange: (item: NodeItemUnitid) => Promise<void>;
}

interface NodeItemUnitid extends NodeItem {
  data: DataUnitid;
}

export function DescItemUnitid({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Unitid && !item.undefined) {
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
  const data = item.data as DataUnitid;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data?.unitId, item);

  async function handleChange(force?: boolean) {
    if (initialValue !== value && (!conflictValue || force)) {
      await onChange({
        ...item,
        data: {
          ...item.data,
          unitId: value,
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
        size={compact ? "small" : "medium"}
        disabled={isDisabled}
        value={item.undefined ? formatMessage(commonMessages.undefined) : (value || "").toString()}
        onChange={handleInputChange}
        onBlur={() => handleChange()}
        style={{
          flex: 1,
          minWidth: "60px",
          fontSize: "1em",
          textDecoration: item.inhibited ? "line-through" : undefined,
          // borderColor: isDirty ? "red" : undefined,
        }}
        // placeholder={item.undefined ? "Výjimka" : ""}
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
