import { Input, InputOnChangeData } from "@fluentui/react-components";
import { useInitialFocus } from "./utils";
import { useCallback, useRef, useState } from "react";
import { FieldType, FilterType, OperationCompareType } from "elza-api";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { FilterWindow } from "./FilterWindow";
import { FilterFormProps } from "./types";

function formatOperation(operation: OperationCompareType) {
  switch (operation) {
    case OperationCompareType.Eq:
      return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>=</div>
    case OperationCompareType.Neq:
      return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>≠</div>
    case OperationCompareType.Contains:
      // return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}><SearchRegular /></div>
      // return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>?</div>
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>*</div>
    // return ": "
    default:
      return operation;
  }
}

export function FilterText({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialValue,
}: FilterFormProps<string>) {
  const availableOperations = [OperationCompareType.Eq, OperationCompareType.Neq, OperationCompareType.Contains];

  const [value, setValue] = useState<string>(initialValue?.data || "");
  const [operation, setOperation] = useState<OperationCompareType>(initialValue.operation || availableOperations?.[0] || OperationCompareType.Eq);

  const inputRef = useRef(null)
  useInitialFocus(inputRef);

  const handleChange = (_e: React.ChangeEvent, data: InputOnChangeData) => {
    setValue(data.value);
  }

  const { formatMessage } = useIntl();

  const isDirty = value != initialValue?.data || (initialValue.operation && operation != initialValue.operation);

  const handleFilterChange = useCallback(() => {
    if (value && isDirty) {
      onFilterChange({
        filterType: FilterType.FieldValue,
        name: filterName,
        data: value,
        operation,
        getDisplayValue: ({ operation, data, name }) => <>
          <b>{formatMessage(messages[name])}</b>
          {formatOperation(operation)}
          {data}
        </>,
        getFilterValue: ({ filterType, name, operation, data }) => ({
          filterType,
          field: {
            fieldType: FieldType.NodeField,
            fieldName: name
          },
          operation,
          value: data,
        }),
        getSerializedString: ({ data }) => data,
      });
    }
  }, [filterName, onFilterChange, value, operation, isDirty, formatMessage]);

  return <FilterWindow
    filterName={formatMessage(messages[filterName])}
    isValid={value != ""}
    isDirty={isDirty}
    availableOperations={availableOperations}
    operation={operation}
    onClose={onClose}
    onFilterConfirm={handleFilterChange}
    onOperationChange={setOperation}
  >
    <Input
      ref={inputRef}
      value={value}
      onChange={handleChange}
      type="text"
    />
  </FilterWindow>
}
