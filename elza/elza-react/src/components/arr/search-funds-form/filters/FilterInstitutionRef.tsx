import { Combobox, Divider, Option, OptionOnSelectData, SelectionEvents } from "@fluentui/react-components";
import { WebApi } from "actions";
import { Fragment, useCallback, useEffect, useRef, useState } from "react";
import { useInitialFocus, formatOperation } from "./utils";
import { FieldType, FilterType, OperationCompareType, OperationLogicalType } from "elza-api";
import { FormattedMessage, useIntl } from "react-intl";
import { messages } from "./messages";
import { MultiFilterWindow } from "./MultiFilterWindow";
import { FilterItem } from "./FilterItem";
import { FilterEntry, FilterFormProps } from "./types";
import { Institution } from "typings/store";

export interface InstitutionData {
    id: number;
    code: string;
    name: string;
}

interface InstitutionRefFilterItem {
    id?: number;
    code?: string;
    name: string;
    operation: OperationCompareType;
}

interface Props extends FilterFormProps<InstitutionData> {
    valueSource?: "code" | "id";
}

export function FilterInstitutionRef({
    filterName,
    onFilterChange,
    onClose = () => { console.warn("'onClose' not defined") },
    initialValue,
    valueSource = "code",
}: Props) {
    const availableOperations = [OperationCompareType.Eq, OperationCompareType.Neq];
    const defaultOperation = availableOperations[0];

    const getInstitutionValue = (institution: Institution) =>
        valueSource === "id" ? institution.id.toString() : institution.code;

    const initialItems: InstitutionRefFilterItem[] = (() => {
        const data = initialValue?.data;
        if (!Array.isArray(data) || data.length === 0) {
            return [{ name: "", operation: defaultOperation }];
        }
        return data.map(({ value, operation }) => ({
            id: value.id,
            code: value.code,
            name: value.name,
            operation: operation ?? defaultOperation,
        }));
    })();

    const [items, setItems] = useState<InstitutionRefFilterItem[]>(initialItems);
    const [institutions, setInstitutions] = useState<Institution[]>([]);
    const [activeQuery, setActiveQuery] = useState<string>("");

    const { formatMessage } = useIntl();
    const firstInputRef = useRef(null);
    useInitialFocus(firstInputRef);

    useEffect(() => {
        (async () => {
            const result: Institution[] = await WebApi.getInstitutions(true);
            setInstitutions(result);
        })();
    }, []);

    const updateItem = useCallback((index: number, patch: Partial<InstitutionRefFilterItem>) => {
        setItems((prev) => prev.map((item, i) => i === index ? { ...item, ...patch } : item));
    }, []);

    const removeItem = useCallback((index: number) => {
        setItems((prev) => prev.filter((_, i) => i !== index));
    }, []);

    const addItem = useCallback(() => {
        setItems((prev) => [...prev, {
            name: "",
            operation: prev[prev.length - 1]?.operation || defaultOperation,
        }]);
    }, [defaultOperation]);

    const filteredInstitutions = institutions.filter((institution) =>
        institution.name.toLowerCase().indexOf(activeQuery.toLowerCase()) >= 0
    );

    const validItems = items.filter((item) => item.id !== undefined || item.code !== undefined);
    const isValid = validItems.length > 0;

    const initialSerialized = JSON.stringify(initialItems);
    const currentSerialized = JSON.stringify(items);
    const isDirty = currentSerialized !== initialSerialized;

    const handleFilterChange = useCallback(() => {
        if (!isValid || !isDirty) { return; }

        const entries: FilterEntry<InstitutionData>[] = validItems.map((item) => ({
            value: {
                id: item.id!,
                code: item.code!,
                name: item.name,
            },
            operation: item.operation,
        }));

        onFilterChange({
            filterType: FilterType.FieldValue,
            name: filterName,
            data: entries,
            getDisplayValue: ({ name, data }) => {
                const displayValues = Array.isArray(data) ? data : [];
                return <>
                    <b>{formatMessage(messages[name])}</b>
                    {displayValues.map(({ value, operation }, index) => (
                        <Fragment key={index}>
                            {index > 0 && <span style={{ padding: "0 5px" }}><FormattedMessage {...messages.filter_or} /></span>}
                            {formatOperation(operation, undefined, true)}
                            {value.name}
                        </Fragment>
                    ))}
                </>
            },
            getFilterValue: ({ filterType, name, data }) => {
                const displayValues = Array.isArray(data) ? data : [];
                const fieldValueFilters = displayValues.map(({ value, operation }) => ({
                    filterType: FilterType.FieldValue,
                    field: {
                        fieldType: FieldType.FondsField,
                        fieldName: name,
                    },
                    operation,
                    value: valueSource === "id" ? value.id.toString() : value.code,
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
            getSerializedString: ({ data }) => {
                const displayValues = Array.isArray(data) ? data : [];
                return displayValues.map(({ value, operation }) => {
                    const key = valueSource === "id" ? value.id : value.code;
                    return `${operation}-${key}`;
                }).join("|");
            },
        });
    }, [filterName, onFilterChange, validItems, isValid, isDirty, formatMessage, valueSource]);

    return <MultiFilterWindow
        filterName={formatMessage(messages[filterName])}
        isValid={isValid}
        isDirty={isDirty}
        onClose={onClose}
        onFilterConfirm={handleFilterChange}
        onAddItem={addItem}
        canAddItem={items.every((item) => item.id !== undefined || item.code !== undefined)}
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
                    <Combobox
                        ref={index === 0 ? firstInputRef : undefined}
                        clearable={true}
                        value={item.name}
                        onChange={(e) => {
                            updateItem(index, { name: e.target.value, id: undefined, code: undefined });
                            setActiveQuery(e.target.value);
                        }}
                        onOptionSelect={(_e: SelectionEvents, data: OptionOnSelectData) => {
                            const selected = institutions.find((i) => getInstitutionValue(i) === data.optionValue);
                            updateItem(index, {
                                name: data.optionText || "",
                                id: selected?.id,
                                code: selected?.code,
                            });
                        }}
                        onFocus={() => setActiveQuery(item.name)}
                        listbox={{ style: { maxHeight: "47vh" } }}
                    >
                        {filteredInstitutions.map((institution) => (
                            <Option key={institution.id} value={getInstitutionValue(institution)}>
                                {institution.name}
                            </Option>
                        ))}
                    </Combobox>
                </FilterItem>
            </Fragment>
        ))}
    </MultiFilterWindow>;
}
