import { Textarea } from "@fluentui/react-components";
import { DataText, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { TextareaAutosize } from "./inputs/TextareaAutosize";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";

interface Props extends DescItemProps {
  onChange: (item: NodeItemText) => Promise<void>;
}

interface NodeItemText extends NodeItem {
  data: DataText;
}

export function DescItemText({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Text && !item.undefined) {
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

  const data = item.data as DataText;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(data?.textValue, item);

  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
      await onChange({
        ...item,
        data: {
          ...item.data,
          textValue: value,
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
  }: React.ChangeEvent<HTMLTextAreaElement>) {
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
      <TextareaAutosize
        disabled={isDisabled}
        value={item.undefined ? formatMessage(commonMessages.undefined) : (value || "").toString()}
        onChange={handleInputChange}
        onBlur={() => handleChange()}
        resize="vertical"
        style={{
          textDecoration: item.inhibited ? "line-through" : undefined,
        }}
      />
      <ConflictValue
        value={value?.toString()}
        conflictValue={conflictValue?.toString()}
        isDirty={isDirty}
        onResolve={resolveConflict}
      >
        {(conflictValue) => (
          <Textarea
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
