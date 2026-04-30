import { Combobox, Divider, OptionOnSelectData, SelectionEvents, Option } from "@fluentui/react-components";
import { Fragment, useCallback, useEffect, useRef, useState } from "react";
import { useInitialFocus, formatOperation } from "./utils";
import { useSelector } from "react-redux";
import { AppState, DescItemTypeRef, RuleType } from "typings/store";
import { messages } from "./messages";
import { FieldType, FilterType, OperationCompareType, OperationLogicalType } from "elza-api";
import { FormattedMessage, useIntl } from "react-intl";
import { MultiFilterWindow } from "./MultiFilterWindow";
import { FilterItem } from "./FilterItem";
import { FilterEntry, FilterFormProps } from "./types";
import { descItemTypesFetchIfNeeded } from "actions/refTables/descItemTypes";
import { useThunkDispatch } from "utils/hooks";
import { RulDataTypeCodeEnum } from "api/RulDataTypeCodeEnum";
import { refRulDataTypesFetchIfNeeded } from "actions/refTables/rulDataTypes";
import { FilterFieldNumber } from "./fields/FilterFieldNumber";
import { FilterFieldText } from "./fields/FilterFieldText";
import { FilterFieldUnitdate } from "./fields/FilterFieldUnitdate";
import { WebApi } from "actions";
import { refRuleSetFetchIfNeeded } from "actions/refTables/ruleSet";
import { RulDescItemSpecExtVO } from "api/RulDescItemSpecExtVO";
import { FilterFieldRecordRef } from "./fields/FilterFieldRecordRef";
import { RulDataTypeVO } from "api/RulDataTypeVO";
import { FilterValueFieldProps } from "./fields/types";

/**
 * Persisted shape of one entry in a DescItem filter's `data` array.
 * `itemType` is denormalised — every entry carries it, but the modal's UI
 * enforces uniformity, so all entries within a single filter share the same
 * `itemType`. `itemValue` is always a string at this level; field components
 * encapsulate the per-data-type encoding (numbers, dates, ids are stringified).
 */
export interface DescItemValue {
  itemType: DescItemTypeRef;
  itemSpec?: RulDescItemSpecExtVO;
  itemValue?: string;
  itemLabel?: string;
}

interface DescItemFilterItem {
  itemSpecCode?: string;
  itemSpecQuery: string;
  operation?: OperationCompareType;
  itemValue?: string;
  itemLabel?: string;
  isValueValid: boolean;
}

interface DataTypeFilterDefinition {
  operations: OperationCompareType[];
  fieldComponent?: (props: FilterValueFieldProps) => JSX.Element;
}

type DataTypeFiltersMap = Partial<Record<RulDataTypeCodeEnum, DataTypeFilterDefinition>>;

function formatSpecNode(item: { itemSpec?: RulDescItemSpecExtVO; itemLabel?: string }, operation: OperationCompareType, dataType: RulDataTypeVO) {
  const ignoreValue = operation === OperationCompareType.NotNull || operation === OperationCompareType.IsNull;
  if (!item.itemSpec) { return undefined; }
  if (dataType.code === RulDataTypeCodeEnum.ENUM) {
    if (ignoreValue) { return undefined; }
    return item.itemSpec.name;
  }
  return <>&nbsp;({item.itemSpec.name})</>
}

function formatLabelNode(item: { itemLabel?: string }, operation: OperationCompareType) {
  const ignoreValue = operation === OperationCompareType.NotNull || operation === OperationCompareType.IsNull;
  return !ignoreValue && item.itemLabel
}

export function FilterDescItemModal({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialValue,
}: FilterFormProps<DescItemValue>) {
  const availableDataTypesMap: DataTypeFiltersMap = {
    [RulDataTypeCodeEnum.INT]: {
      operations: [
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.Lt,
        OperationCompareType.Gt,
        OperationCompareType.Lte,
        OperationCompareType.Gte,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldNumber,
    },
    [RulDataTypeCodeEnum.DECIMAL]: {
      operations: [
        OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.Lt, OperationCompareType.Gt,
        OperationCompareType.Lte, OperationCompareType.Gte,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldNumber,
    },
    [RulDataTypeCodeEnum.STRING]: {
      operations: [
        OperationCompareType.Contains, OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.TEXT]: {
      operations: [
        OperationCompareType.Contains, OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.UNITDATE]: {
      operations: [
        OperationCompareType.Contains, OperationCompareType.Intersect, OperationCompareType.IsIn,
        OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.Lt, OperationCompareType.Gt, OperationCompareType.Lte, OperationCompareType.Gte,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldUnitdate,
    },
    [RulDataTypeCodeEnum.ENUM]: {
      operations: [
        OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
    },
    [RulDataTypeCodeEnum.RECORD_REF]: {
      operations: [
        OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldRecordRef,
    },
    [RulDataTypeCodeEnum.STRUCTURED]: {
      operations: [
        OperationCompareType.Contains, OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.URI_REF]: {
      operations: [
        OperationCompareType.Contains, OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.BIT]: {
      operations: [OperationCompareType.IsNull, OperationCompareType.NotNull],
    },
    [RulDataTypeCodeEnum.DATE]: {
      operations: [OperationCompareType.IsNull, OperationCompareType.NotNull],
    },
    [RulDataTypeCodeEnum.UNITID]: {
      operations: [
        OperationCompareType.Contains, OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.FILE_REF]: {
      operations: [
        OperationCompareType.Contains, OperationCompareType.Eq, OperationCompareType.Neq,
        OperationCompareType.IsNull, OperationCompareType.NotNull,
      ],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.COORDINATES]: {
      operations: [OperationCompareType.IsNull, OperationCompareType.NotNull],
    },
  }

  const initialItems: DescItemFilterItem[] = (() => {
    const data = initialValue?.data;
    if (!Array.isArray(data) || data.length === 0) {
      return [{ itemSpecQuery: "", isValueValid: false }];
    }
    return data.map(({ value, operation }) => ({
      itemSpecCode: value.itemSpec?.code,
      itemSpecQuery: value.itemSpec?.name || "",
      operation,
      itemValue: value.itemValue,
      itemLabel: value.itemLabel,
      isValueValid: value.itemValue ? true : false,
    }));
  })();

  const initialItemType = initialValue?.data?.[0]?.value.itemType;
  const [itemTypeCode, setItemTypeCode] = useState<string>(initialItemType?.code);
  const [itemTypeQuery, setItemTypeQuery] = useState<string>(initialItemType?.name || "");
  const [items, setItems] = useState<DescItemFilterItem[]>(initialItems);
  const [descItemTypes, setDescItemTypes] = useState<DescItemTypeRef[]>([]);
  const [descItemTypesFetched, setDescItemTypesFetched] = useState<boolean>(false);

  const dataTypes = useSelector(({ refTables }: AppState) => refTables.rulDataTypes);

  const allDescItemTypes = useSelector(({ refTables }: AppState) => {
    if (!dataTypes.fetched) { return null; }
    return refTables.descItemTypes.items.filter(({ dataTypeId }) => {
      const dataType = dataTypes.items?.find(({ id }) => id === dataTypeId);
      if (dataType) {
        return !!availableDataTypesMap[dataType.code];
      }
    })
  });

  const arrangementRuleSets = useSelector(({ refTables }: AppState) =>
    refTables.ruleSet.items.filter(({ ruleType }) => ruleType === RuleType.ARRANGEMENT));

  const dispatch = useThunkDispatch();

  const intl = useIntl();
  const { formatMessage } = intl;

  const inputRef = useRef(null);
  useInitialFocus(inputRef);

  // desc item types filtered by query
  const filterItemTypes = () => {
    if (!itemTypeQuery) { return descItemTypes; }
    const normalizedQuery = itemTypeQuery.toLowerCase();
    return descItemTypes.filter(({ name, shortcut }) => {
      const sources = [
        name.toLowerCase(),
        shortcut.toLowerCase(),
        name.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase(),
        shortcut.normalize("NFD").replace(/\p{Diacritic}/gu, "").toLowerCase(),
      ]
      return sources.find((normalizedSource) => normalizedSource.indexOf(normalizedQuery) >= 0) != undefined;
    })
  }

  const filteredItemTypes = filterItemTypes();

  let selectedItemType: DescItemTypeRef | undefined;
  let selectedDataType: RulDataTypeVO | undefined;
  let dataTypeFilterDefinition: DataTypeFilterDefinition | undefined;

  if (itemTypeCode && descItemTypes.length > 0 && dataTypes) {
    selectedItemType = descItemTypes.find(({ code }) => code === itemTypeCode);
    if (!selectedItemType) { throw `Missing item type: ${itemTypeCode}`; }

    selectedDataType = dataTypes.items.find(({ id }) => id === selectedItemType.dataTypeId);
    if (!selectedDataType) { throw `Missing data type: ${selectedItemType.dataTypeId}`; }

    dataTypeFilterDefinition = availableDataTypesMap[selectedDataType.code];
  }

  // Load used refTables data, if not present
  useEffect(() => {
    dispatch(descItemTypesFetchIfNeeded());
    dispatch(refRulDataTypesFetchIfNeeded());
    dispatch(refRuleSetFetchIfNeeded());
  }, [])

  useEffect(() => {
    if (!descItemTypesFetched && arrangementRuleSets.length > 0 && allDescItemTypes?.length > 0) {
      (async function () {
        const promises = arrangementRuleSets.map(({ id }) => WebApi.getItemTypeCodesByRuleSet(id));
        const itemTypes = await Promise.all(promises);
        const descItemTypeCodes: string[] = [].concat(...itemTypes);
        const availableDescItemTypes = allDescItemTypes.filter(({ code }) => descItemTypeCodes?.includes(code));
        setDescItemTypes(availableDescItemTypes);
        setDescItemTypesFetched(true);
      })()
    }
  }, [allDescItemTypes, arrangementRuleSets, descItemTypesFetched])

  // Preselect first operation from filter definition when missing
  useEffect(() => {
    if (!dataTypeFilterDefinition) { return; }
    setItems((prev) => prev.map((item) => item.operation
      ? item
      : { ...item, operation: dataTypeFilterDefinition.operations?.[0] }));
  }, [dataTypeFilterDefinition?.operations])

  const handleItemTypeSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    setItemTypeQuery(data.optionText || "");
    setItemTypeCode(data.optionValue || "");
    setItems([{ itemSpecQuery: "", isValueValid: false }]);
  }

  const updateItem = useCallback((index: number, patch: Partial<DescItemFilterItem>) => {
    setItems((prev) => prev.map((item, i) => i === index ? { ...item, ...patch } : item));
  }, []);

  const removeItem = useCallback((index: number) => {
    setItems((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const addItem = useCallback(() => {
    setItems((prev) => [...prev, {
      itemSpecQuery: "",
      operation: prev[prev.length - 1]?.operation || dataTypeFilterDefinition?.operations?.[0],
      isValueValid: false,
    }]);
  }, [dataTypeFilterDefinition?.operations])

  function validateItem(item: DescItemFilterItem): boolean {
    if (!item.operation) { return false; }
    if (
      item.operation === OperationCompareType.IsNull
      || item.operation === OperationCompareType.NotNull
      || !dataTypeFilterDefinition?.fieldComponent
    ) {
      return true;
    }
    return item.isValueValid;
  }

  const isValid = !!itemTypeCode && items.length > 0 && items.every(validateItem);
  const isDirty = JSON.stringify({ itemTypeCode, items }) !== JSON.stringify({
    itemTypeCode: initialItemType?.code,
    items: initialItems,
  });

  const handleFilterChange = useCallback(() => {
    const itemType = itemTypeCode && descItemTypes.find(({ code }) => code === itemTypeCode);
    if (!itemType || !isDirty) { return; }
    const dataType = dataTypes.itemsMap[itemType.dataTypeId];

    const entries: FilterEntry<DescItemValue>[] = items.map((item) => {
      const itemSpec = item.itemSpecCode && itemType.descItemSpecs?.find(({ code }) => code === item.itemSpecCode);
      let _itemValue = item.itemValue;
      let _itemLabel = item.itemLabel;
      let _itemSpec: RulDescItemSpecExtVO | undefined = itemSpec || undefined;

      if (item.operation === OperationCompareType.IsNull || item.operation === OperationCompareType.NotNull) {
        if (dataType.code === RulDataTypeCodeEnum.ENUM) {
          _itemSpec = undefined;
        }
        _itemValue = undefined;
        _itemLabel = undefined;
      }

      return {
        value: {
          itemType,
          itemSpec: _itemSpec,
          itemValue: _itemValue,
          itemLabel: _itemLabel,
        },
        operation: item.operation!,
      };
    });

    const isEnumValue = dataType.code === RulDataTypeCodeEnum.ENUM
      || dataType.code === RulDataTypeCodeEnum.FILE_REF
      || dataType.code === RulDataTypeCodeEnum.RECORD_REF
      || dataType.code === RulDataTypeCodeEnum.STRUCTURED

    onFilterChange({
      filterType: FilterType.FieldValue,
      name: itemType.code,
      data: entries,
      getDisplayValue: ({ data }) => <>
        <b>{data[0]?.value.itemType.shortcut}</b>
        {data.map(({ value, operation }, index) => (
          <Fragment key={index}>
            {index > 0 && <span style={{ padding: "0 5px" }}><FormattedMessage {...messages.filter_or} /></span>}
            {dataType.code === RulDataTypeCodeEnum.ENUM && formatOperation(operation, intl, isEnumValue)}
            {formatSpecNode(value, operation, dataType)}
            {dataType.code !== RulDataTypeCodeEnum.ENUM && formatOperation(operation, intl, isEnumValue)}
            {formatLabelNode(value, operation)}
          </Fragment>
        ))}
      </>,
      getFilterValue: ({ filterType, data }) => {
        const fieldValueFilters = data.map(({ value, operation }) => ({
          filterType: FilterType.FieldValue,
          field: {
            fieldType: FieldType.DescItem,
            typeCode: value.itemType.code,
            specCode: value.itemSpec?.code,
          },
          operation,
          value: value.itemValue,
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
        const parts = data.map(({ value, operation }, index) => {
          const typeCode = value.itemType.code;
          if (value.itemSpec && value.itemValue) {
            return `${index}:${typeCode} - ${value.itemSpec.code} ${operation} ${value.itemValue}`;
          } else if (value.itemSpec && !value.itemValue) {
            return `${index}:${typeCode} ${operation} ${value.itemSpec.code}`;
          } else if (!value.itemSpec && value.itemValue) {
            return `${index}:${typeCode} ${operation} ${value.itemValue}`;
          }
          return `${index}:${typeCode} ${operation}`;
        });
        return parts.join("|");
      },
    });
  }, [onFilterChange, itemTypeCode, items, descItemTypes, isDirty, intl, dataTypes]);

  return <MultiFilterWindow
    filterName={formatMessage(messages[filterName])}
    isValid={isValid}
    isDirty={isDirty}
    onClose={onClose}
    onFilterConfirm={handleFilterChange}
    onAddItem={itemTypeCode ? addItem : undefined}
    canAddItem={items.every(validateItem)}
  >
    <Combobox
      ref={inputRef}
      clearable={true}
      value={itemTypeQuery}
      defaultValue={itemTypeQuery}
      onChange={(e) => setItemTypeQuery(e.target.value)}
      onOptionSelect={handleItemTypeSelect}
      positioning={{ position: "below", autoSize: "height" }}
      style={{ marginBottom: "4px" }}
    >
      {filteredItemTypes.map(({ name, id, code }) => (
        <Option key={id} value={code.toString()}>{name}</Option>
      ))}
    </Combobox>
    {itemTypeCode && items.map((item, index) => {
      const itemSpec = item.itemSpecCode && selectedItemType?.descItemSpecs?.find(({ code }) => code === item.itemSpecCode);
      const filteredItemSpecs = selectedItemType?.descItemSpecs?.filter((spec) =>
        spec.name.toLowerCase().indexOf((item.itemSpecQuery || "").toLowerCase()) >= 0) || [];

      const hideValue = item.operation === OperationCompareType.IsNull || item.operation === OperationCompareType.NotNull;
      const hideSpec = hideValue && selectedDataType?.code === RulDataTypeCodeEnum.ENUM;

      return <Fragment key={index}>
        {index > 0 && <Divider style={{ margin: "4px 0", fontSize: "0.75rem", color: "#666" }}>
          <FormattedMessage {...messages.filter_or} />
        </Divider>}
        <FilterItem
          operation={item.operation}
          availableOperations={dataTypeFilterDefinition?.operations || []}
          onOperationChange={(operation) => updateItem(index, { operation })}
          onRemove={() => removeItem(index)}
          canRemove={items.length > 1}
        >
          <div style={{ display: "flex", gap: "5px" }}>
            {selectedItemType?.useSpecification
              && selectedItemType?.descItemSpecs?.length > 0
              && !hideSpec
              && <Combobox
                clearable={true}
                value={item.itemSpecQuery}
                defaultValue={item.itemSpecQuery}
                onChange={(e) => updateItem(index, { itemSpecQuery: e.target.value })}
                onOptionSelect={(_e, data) => updateItem(index, {
                  itemSpecQuery: data.optionText || "",
                  itemSpecCode: data.optionValue || undefined,
                })}
                positioning={{ position: "below", autoSize: "height" }}
              >
                {filteredItemSpecs.map(({ name, id, code }) => (
                  <Option key={id} value={code.toString()}>{name}</Option>
                ))}
              </Combobox>}
            {!!dataTypeFilterDefinition?.fieldComponent
              && !hideValue
              && <dataTypeFilterDefinition.fieldComponent
                value={item.itemValue || ""}
                label={item.itemLabel}
                onChange={(value, isValid, valueLabel) => updateItem(index, {
                  itemValue: value,
                  itemLabel: valueLabel || value,
                  isValueValid: value !== "" && (isValid ?? true),
                })}
                itemType={selectedItemType}
                itemSpec={itemSpec || undefined}
              />}
          </div>
        </FilterItem>
      </Fragment>
    })}
  </MultiFilterWindow>
}
