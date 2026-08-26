import { Textarea } from "@fluentui/react-components";
import { DataText, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { TextareaAutosize } from "./inputs/TextareaAutosize";
import { DescItemProps } from "./types";
import { useValueManager } from "./utils";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useTextFragmentsContext } from "components/arr/text-fragments";
import { useStyles } from "./styles";

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
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.Text && !item.undefined) {
    throw "Incorrect data type";
  }

  const { formatMessage } = useIntl();
  const styles = useStyles();
  const textFragments = useTextFragmentsContext();
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
    if (initialValue !== value && (!conflictValue || force)) {
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

    function handleFocus(event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) {
        const field = event.currentTarget;
        textFragments?.registerField(field, (text: string) => {
            const start = field.selectionStart;
            const end = field.selectionEnd;
            const newPos = start + text.length;
            setValue(`${field.value.slice(0, start)}${text}${field.value.slice(end)}`);
            requestAnimationFrame(() => {
                field.selectionStart = newPos;
                field.selectionEnd = newPos;
            });
        });
    }

    function handleBlur() {
        textFragments?.unregisterField();
        handleChange();
    }

  return (
    <div className={styles.descItemContainer}>
      <TextareaAutosize
        size={compact ? "small" : "medium"}
        disabled={isDisabled}
        value={item.undefined ? formatMessage(commonMessages.undefined) : (value || "").toString()}
        onChange={handleInputChange}
        onBlur={handleBlur}
        onFocus={handleFocus}
        resize="vertical"
        style={{
          textDecoration: item.inhibited ? "line-through" : undefined,
          fontSize: "1em",
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
