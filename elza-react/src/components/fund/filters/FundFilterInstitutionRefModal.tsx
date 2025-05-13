import { Combobox, OptionOnSelectData, SelectionEvents, Option } from "@fluentui/react-components";
import { WebApi } from "actions";
import { useCallback, useEffect, useRef, useState } from "react";
import { useInitialFocus } from "./utils";
import { useSelector } from "react-redux";
import { AppState, Institution } from "typings/store";
import { messages } from "./messages";
import { FieldType, FilterType, OperationCompareType } from "elza-api";
import { useIntl } from "react-intl";
import { FilterWindow } from "./FilterWindow";
import { FilterFormProps } from "./types";

function formatOperation(operation: OperationCompareType) {
  switch (operation) {
    case OperationCompareType.Eq:
      return ": "
    case OperationCompareType.Neq:
      return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>≠</div>
    default:
      return operation;
  }
}

export function FundFilterInstitutionRefModal({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialValue,
}: FilterFormProps<Institution>) {
  const availableOperations = [OperationCompareType.Eq, OperationCompareType.Neq];

  const [value, setValue] = useState<string>(initialValue?.data?.code);
  const [query, setQuery] = useState<string>("");
  const [operation, setOperation] = useState<OperationCompareType>(initialValue.operation || availableOperations?.[0] || OperationCompareType.Eq);
  const [institutions, setInstitutions] = useState<Institution[]>([]);
  const allInstitutions = useSelector(({ refTables }: AppState) => refTables.institutions.items)

  const { formatMessage } = useIntl();

  const isDirty = value != initialValue?.data?.code || (initialValue.operation && operation != initialValue.operation);
  const inputRef = useRef(null)
  useInitialFocus(inputRef);

  useEffect(() => {
    (async () => {
      const _institutions: Institution[] = await WebApi.getInstitutions(true);
      const initialInstitution = allInstitutions.find(({ code }) => code === initialValue?.data?.code);
      if (initialInstitution) {
        setQuery(initialInstitution.name);
      }
      setInstitutions(_institutions)
    })()
  }, [initialValue, allInstitutions])

  const handleInstitutionSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    setQuery(data.optionText || "");
    setValue(data.optionValue || "");
  }

  const handleFilterChange = useCallback(() => {
    const institution = institutions.find(({ code }) => code === value);

    if (institution && isDirty) {
      onFilterChange({
        filterType: FilterType.FieldValue,
        name: filterName,
        data: institution,
        operation,
        getDisplayValue: ({ operation, data, name }) => <>
          <b>{formatMessage(messages[name])}</b>
          {formatOperation(operation)}
          {data.name}
        </>,
        getFilterValue: ({ filterType, name, operation, data }) => ({
          filterType,
          field: {
            fieldType: FieldType.Fund,
            fieldName: name,
          },
          operation,
          value: data.code,
        }),
        getSerializedString: ({ data }) => data.name,
      });
    }
  }, [filterName, onFilterChange, value, operation, isDirty, institutions, formatMessage]);

  const filteredInstitutions = isDirty ? institutions.filter((institution) => institution.name.toLowerCase().indexOf((query || "").toLowerCase()) >= 0) : institutions;

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
    <Combobox
      ref={inputRef}
      clearable={true}
      value={query}
      defaultValue={query}
      onChange={(e) => {
        setQuery(e.target.value);
      }}
      onOptionSelect={handleInstitutionSelect}
    >
      {filteredInstitutions.map(({ name, id, code }) => {
        return <Option key={id} value={code.toString()}>{name}</Option>
      })}
    </Combobox>
  </FilterWindow>
}

