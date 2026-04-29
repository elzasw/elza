import { Combobox, Divider, Option, OptionOnSelectData, SelectionEvents } from '@fluentui/react-components';
import { WebApi } from 'actions';
import { Fragment, useCallback, useEffect, useRef, useState } from 'react';
import { useInitialFocus, formatOperation } from './utils';
import { FieldType, FilterType, OperationCompareType, OperationLogicalType } from 'elza-api';
import { FormattedMessage, useIntl } from 'react-intl';
import { messages } from './messages';
import { MultiFilterWindow } from './MultiFilterWindow';
import { FilterItem } from './FilterItem';
import { FilterEntry, FilterFormProps } from './types';

export interface FondsData {
    id: number;
    name: string;
}

interface FondsRefFilterItem {
    id?: number;
    name: string;
    operation: OperationCompareType;
}

export function FilterFondsRef({
    filterName,
    onFilterChange,
    onClose = () => {
        console.warn("'onClose' not defined");
    },
    initialValue,
}: FilterFormProps<FondsData>) {
    const availableOperations = [OperationCompareType.Eq, OperationCompareType.Neq];
    const defaultOperation = availableOperations[0];

    const initialItems: FondsRefFilterItem[] = (() => {
        const data = initialValue?.data;
        if (!Array.isArray(data) || data.length === 0) {
            return [{ name: '', operation: defaultOperation }];
        }
        return data.map(({ value, operation }) => ({
            id: value.id,
            name: value.name,
            operation: operation ?? defaultOperation,
        }));
    })();

    const [items, setItems] = useState<FondsRefFilterItem[]>(initialItems);
    const [funds, setFunds] = useState<FondsData[]>([]);
    const [activeQuery, setActiveQuery] = useState<string>('');

    const { formatMessage } = useIntl();
    const firstInputRef = useRef(null);
    useInitialFocus(firstInputRef);

    useEffect(() => {
        (async () => {
            const result = await WebApi.findFunds(activeQuery);
            setFunds(result.funds);
        })();
    }, [activeQuery]);

    const updateItem = useCallback((index: number, patch: Partial<FondsRefFilterItem>) => {
        setItems((prev) => prev.map((item, i) => (i === index ? { ...item, ...patch } : item)));
    }, []);

    const removeItem = useCallback((index: number) => {
        setItems((prev) => prev.filter((_, i) => i !== index));
    }, []);

    const addItem = useCallback(() => {
        setItems((prev) => [
            ...prev,
            {
                name: '',
                operation: prev[prev.length - 1]?.operation || defaultOperation,
            },
        ]);
    }, [defaultOperation]);

    const validItems = items.filter((item) => item.id !== undefined);
    const isValid = validItems.length > 0;

    const initialSerialized = JSON.stringify(initialItems);
    const currentSerialized = JSON.stringify(items);
    const isDirty = currentSerialized !== initialSerialized;

    const handleFilterChange = useCallback(() => {
        if (!isValid || !isDirty) {
            return;
        }

        const entries: FilterEntry<FondsData>[] = validItems.map((item) => ({
            value: { id: item.id!, name: item.name },
            operation: item.operation,
        }));

        onFilterChange({
            filterType: FilterType.FieldValue,
            name: filterName,
            data: entries,
            getDisplayValue: ({ name, data }) => {
                const displayValues = Array.isArray(data) ? data : [];
                return (
                    <>
                        <b>{formatMessage(messages[name])}</b>
                        {displayValues.map(({ value, operation }, index) => (
                            <Fragment key={index}>
                                {index > 0 && (
                                    <span style={{ padding: '0 5px' }}>
                                        <FormattedMessage {...messages.filter_or} />
                                    </span>
                                )}
                                {formatOperation(operation, undefined, true)}
                                {value.name}
                            </Fragment>
                        ))}
                    </>
                );
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
                    value: value.id.toString(),
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
                return displayValues
                    .map(({ value, operation }) => `${operation}-${value.id}`)
                    .join('|');
            },
        });
    }, [filterName, onFilterChange, validItems, isValid, isDirty, formatMessage]);

    return (
        <MultiFilterWindow
            filterName={formatMessage(messages[filterName])}
            isValid={isValid}
            isDirty={isDirty}
            onClose={onClose}
            onFilterConfirm={handleFilterChange}
            onAddItem={addItem}
            canAddItem={items.every((item) => item.id !== undefined)}
        >
            {items.map((item, index) => (
                <Fragment key={index}>
                    {index > 0 && (
                        <Divider style={{ margin: '4px 0', fontSize: '0.75rem', color: '#666' }}>
                            <FormattedMessage {...messages.filter_or} />
                        </Divider>
                    )}
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
                                updateItem(index, { name: e.target.value, id: undefined });
                                setActiveQuery(e.target.value);
                            }}
                            onOptionSelect={(_e: SelectionEvents, data: OptionOnSelectData) => {
                                updateItem(index, {
                                    name: data.optionText || '',
                                    id: data.optionValue ? parseInt(data.optionValue) : undefined,
                                });
                            }}
                            onFocus={() => setActiveQuery(item.name)}
                            listbox={{
                                style: {
                                    maxHeight: '47vh',
                                },
                            }}
                        >
                            {funds.map(({ id, name }) => (
                                <Option key={id} value={id.toString()}>
                                    {name}
                                </Option>
                            ))}
                        </Combobox>
                    </FilterItem>
                </Fragment>
            ))}
        </MultiFilterWindow>
    );
}
