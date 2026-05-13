import { Input } from "@fluentui/react-components";
import { DataString, DataType, NodeItem } from "elza-api";
import { ConflictValue } from "./ConflictValue";
import { EditStateDisplay } from "./EditStateDisplay";
import { useValueManager } from "./utils";
import { DescItemProps } from "./types";
import { TextareaAutosize } from "./inputs/TextareaAutosize";
import { isMaskViewDefinition, maskString, unmaskString } from "./maskUtils";
import { useIntl } from "react-intl";
import { messages as commonMessages } from "./commonMessages";
import { useTextFragmentsContext } from "components/arr/text-fragments";
import { useStyles } from "./styles";

interface Props extends DescItemProps {
  onChange: (item: NodeItemString) => Promise<void>;
}

interface NodeItemString extends NodeItem {
  data: DataString;
}

export function DescItemString({
  item,
  onChange,
  nodeId,
  isDisabled: _isDisabled,
  typeWidth,
  typeRef,
  compact,
}: Props) {
  if (item.data && item.data?.dataType !== DataType.String && !item.undefined) {
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
  const data = item.data as DataString;

  const mask = isMaskViewDefinition(typeRef.viewDefinition) ? typeRef.viewDefinition.mask : undefined;
    const _initialValue = mask ? maskString(data?.stringValue || "", mask) : data?.stringValue;

  const {
    value,
    setValue,
    isDirty,
    conflictValue,
    initialValue,
    resetConflict,
    finishChange,
  } = useValueManager<string>(_initialValue, item);


  async function handleChange(force?: boolean) {
    if (value && initialValue !== value && (!conflictValue || force)) {
        const stringValue = mask ? unmaskString(value, mask) : value;
      await onChange({
        ...item,
        data: {
          ...item.data,
          stringValue,
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

  function handleFocus(event: React.FocusEvent<HTMLInputElement | HTMLTextAreaElement>) {
    if (mask) { return; }
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

  function handleInputChange({
    currentTarget,
  }: React.ChangeEvent<HTMLTextAreaElement | HTMLInputElement>) {
    let _value = currentTarget.value.replace(/\n/g, "");
    if (mask) {
        _value = maskString(unmaskString(_value, mask), mask);
    }

    if (_value != value && (value || _value?.length > 0)) {
      setValue(_value);
    }
  }

  return (
    <div className={styles.descItemContainerWithWidth}>
      {typeWidth === 0 ? (
        <TextareaAutosize
          resize="none"
          size={compact ? "small" : "medium"}
          disabled={isDisabled}
          value={item.undefined ? formatMessage(commonMessages.undefined) : value || ""}
          onChange={handleInputChange}
          onFocus={handleFocus}
          onBlur={handleBlur}
          style={{
            flex: 1,
            minWidth: "60px",
            textDecoration: item.inhibited ? "line-through" : undefined,
          }}
        />
      ) : (
        <Input
          size={compact ? "small" : "medium"}
          disabled={isDisabled}
          value={item.undefined ? formatMessage(commonMessages.undefined) : value || ""}
          onChange={handleInputChange}
          onFocus={handleFocus}
          onBlur={handleBlur}
          style={{
            flex: 1,
            minWidth: "60px",
            textDecoration: item.inhibited ? "line-through" : undefined,
          }}
        />
      )}
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
