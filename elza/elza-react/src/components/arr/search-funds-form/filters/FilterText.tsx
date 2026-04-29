import { Divider, Input, InputOnChangeData } from "@fluentui/react-components";
import { useInitialFocus, formatOperation } from "./utils";
import { useCallback, useRef, useState, Fragment } from "react";
import { FieldType, FilterType, OperationCompareType, OperationLogicalType } from "elza-api";
import { FormattedMessage, useIntl } from "react-intl";
import { messages } from "./messages";
import { MultiFilterWindow } from "./MultiFilterWindow";
import { FilterItem } from "./FilterItem";
import { FilterEntry, FilterFormProps } from "./types";

const defaultAvailableOperations = [
  OperationCompareType.Contains,
  OperationCompareType.Eq,
  OperationCompareType.Neq,
];

export function FilterText({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialValue,
}: FilterFormProps<string>) {
  const availableOperations = defaultAvailableOperations;
  const defaultOperation = availableOperations[0];

  const initialItems: FilterEntry<string>[] = (() => {
    const data = initialValue?.data;
    if (!Array.isArray(data) || data.length === 0) {
      return [{ value: "", operation: defaultOperation }];
    }
    return data.map(({ value, operation }) => ({ value, operation: operation ?? defaultOperation }));
  })();

  const [items, setItems] = useState<FilterEntry<string>[]>(initialItems);

  const firstInputRef = useRef<HTMLInputElement>(null);
  useInitialFocus(firstInputRef);

  const { formatMessage } = useIntl();

  const updateItem = useCallback((index: number, patch: Partial<FilterEntry<string>>) => {
    setItems((prev) => prev.map((item, i) => i === index ? { ...item, ...patch } : item));
  }, []);

  const removeItem = useCallback((index: number) => {
    setItems((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const addItem = useCallback(() => {
    setItems((prev) => [...prev, { value: "", operation: prev[prev.length - 1]?.operation || defaultOperation }]);
  }, [defaultOperation]);

  const nonEmptyItems = items.filter((item) => item.value !== "");
  const isValid = nonEmptyItems.length > 0;

  const initialSerialized = JSON.stringify(initialItems);
  const currentSerialized = JSON.stringify(items);
  const isDirty = currentSerialized !== initialSerialized;

  const handleFilterChange = useCallback(() => {
    if (!isValid || !isDirty) { return; }

    onFilterChange({
      filterType: FilterType.FieldValue,
      name: filterName,
      data: nonEmptyItems.map(({ value, operation }) => ({ value, operation })),
      getDisplayValue: ({ name, data }) => {
        const displayValues = Array.isArray(data) ? data : [];
        return <>
          <b>{formatMessage(messages[name])}</b>
          {displayValues.map(({ value, operation }, index) => (
            <Fragment key={index}>
              {index > 0 && <span style={{ padding: "0 5px" }}><FormattedMessage {...messages.filter_or} /></span>}
              {formatOperation(operation)}
              {value}
            </Fragment>
          ))}
        </>
      },
      getFilterValue: ({ filterType, name, data }) => {
        const displayValues = Array.isArray(data) ? data : [];
        const fieldValueFilters = displayValues.map(({ value, operation }) => ({
          filterType: FilterType.FieldValue,
          field: {
            fieldType: FieldType.NodeField,
            fieldName: name,
          },
          operation,
          value,
        }));

        if (fieldValueFilters.length === 1) {
          return fieldValueFilters[0];
        }

        return {
          filterType: FilterType.Logical,
          operation: OperationLogicalType.Or,
          filters: fieldValueFilters,
        };
      },
      getSerializedString: ({ name, data }) => {
        const displayValues = Array.isArray(data) ? data : [];
        return displayValues.map(({ value, operation }) => `${name}-${operation}-${value}`).join("|");
      },
    });
  }, [filterName, onFilterChange, nonEmptyItems, isValid, isDirty, formatMessage]);

  return <MultiFilterWindow
    filterName={formatMessage(messages[filterName])}
    isValid={isValid}
    isDirty={isDirty}
    onClose={onClose}
    onFilterConfirm={handleFilterChange}
    onAddItem={addItem}
    canAddItem={items.every((item) => item.value !== "")}
  >
    {items.map((item, index) => (
      <Fragment key={index}>
        {index > 0 && <Divider style={{ margin: "4px 0", fontSize: "0.75rem", color: "#666" }}>
          <FormattedMessage {...messages.filter_or} />
        </Divider>}
        <FilterItem
          operation={item.operation}
          availableOperations={availableOperations}
          onOperationChange={(operation) => updateItem(index, { operation })}
          onRemove={() => removeItem(index)}
          canRemove={items.length > 1}
        >
          <Input
            ref={index === 0 ? firstInputRef : undefined}
            value={item.value}
            onChange={(_e: React.ChangeEvent, data: InputOnChangeData) => updateItem(index, { value: data.value })}
            type="text"
          />
        </FilterItem>
      </Fragment>
    ))}
  </MultiFilterWindow>
}
