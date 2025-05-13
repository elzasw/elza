import { Input, InputOnChangeData } from "@fluentui/react-components";
import { useInitialFocus } from "./utils";
import { useCallback, useRef, useState } from "react";
import { FilterType, OperationCompareType } from "elza-api";
import { FilterFormProps } from "./FundFilterModal";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { FilterWindow } from "./FilterWindow";

function formatOperation(operation: OperationCompareType) {
  switch (operation) {
    case OperationCompareType.Eq:
      return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>=</div>
    case OperationCompareType.Neq:
      return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>≠</div>
    default:
      return operation;
  }
}

export function FundFilterNumberForm({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialValue,
}: FilterFormProps<string>) {
  const availableOperations = [OperationCompareType.Eq, OperationCompareType.Neq];

  const [value, setValue] = useState<string>(initialValue?.data || "");
  const [operation, setOperation] = useState<OperationCompareType>(initialValue.operation || availableOperations?.[0] || OperationCompareType.Eq);

  const inputRef = useRef(null)
  useInitialFocus(inputRef);

  const { formatMessage } = useIntl();

  const isDirty = value != initialValue?.data || (initialValue.operation && operation != initialValue.operation);

  const handleChange = (_e: React.ChangeEvent, data: InputOnChangeData) => {
    const _value = data.value.replace(/\D/g, '');
    setValue(_value);
  }

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
          field: name,
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
    />
  </FilterWindow>
}
