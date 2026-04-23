import { Combobox, Option, OptionOnSelectData, SelectionEvents } from "@fluentui/react-components";
import { WebApi } from "actions";
import { useCallback, useEffect, useRef, useState } from "react";
import { useInitialFocus } from "./utils";
import { FieldType, FilterType, FondsFieldName, OperationCompareType } from "elza-api";
import { useIntl } from "react-intl";
import { messages } from "./messages";
import { FilterWindow } from "./FilterWindow";
import { FilterFormProps } from "./types";

export interface FondsData {
    id: number;
    name: string;
}

export function FilterFondsRef({
    filterName,
    onFilterChange,
    onClose = () => { console.warn("'onClose' not defined") },
    initialValue,
}: FilterFormProps<FondsData>) {
    const availableOperations = [OperationCompareType.Eq, OperationCompareType.Neq];

    const [value, setValue] = useState<number | undefined>(initialValue?.data?.id);
    const [query, setQuery] = useState<string>(initialValue?.data?.name || "");
    const [funds, setFunds] = useState<FondsData[]>([]);
    const [operation, setOperation] = useState<OperationCompareType>(initialValue?.operation || availableOperations[0]);

    const { formatMessage } = useIntl();
    const inputRef = useRef(null);
    useInitialFocus(inputRef);

    const isDirty = value !== initialValue?.data?.id || (initialValue.operation && operation !== initialValue.operation);

    useEffect(() => {
        (async () => {
            const result = await WebApi.findFunds(query);
            setFunds(result.funds);
        })();
    }, [query]);

    const handleSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
        setQuery(data.optionText || "");
        setValue(parseInt(data.optionValue));
    };

    const handleFilterChange = useCallback(() => {
        const fund = funds.find(({ id }) => id === value);
        if (fund && isDirty) {
            onFilterChange({
                filterType: FilterType.FieldValue,
                name: filterName,
                data: { id: fund.id, name: fund.name },
                operation,
                getDisplayValue: ({ data, name }) => <>
                    <b>{formatMessage(messages[name])}</b>
                    {": "}
                    {data.name}
                </>,
                getFilterValue: ({ filterType, name, operation, data }) => ({
                    filterType,
                    field: {
                        fieldType: FieldType.FondsField,
                        fieldName: name,
                    },
                    operation,
                    value: data.id.toString(),
                }),
                getSerializedString: ({ data }) => data.name,
            });
        }
    }, [filterName, onFilterChange, value, operation, isDirty, funds, formatMessage]);

    return <FilterWindow
        filterName={formatMessage(messages[filterName])}
        isValid={value !== undefined}
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
            onChange={(e) => setQuery(e.target.value)}
            onOptionSelect={handleSelect}
            listbox={{
                style: {
                    maxHeight: "47vh",
                },
            }}
        >
            {funds.map(({ id, name }) => (
                <Option key={id} value={id.toString()}>{name}</Option>
            ))}
        </Combobox>
    </FilterWindow>;
}
