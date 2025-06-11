import { Combobox, OptionOnSelectData, SelectionEvents, Option, Menu, MenuTrigger, MenuButton, MenuPopover, MenuItem } from "@fluentui/react-components";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useInitialFocus } from "./utils";
import { useSelector } from "react-redux";
import { AppState, DescItemTypeRef, RuleType } from "typings/store";
import { messages } from "./messages";
import { FieldType, FilterType, OperationCompareType } from "elza-api";
import { useIntl } from "react-intl";
import { BaseFilterWindow } from "./FilterWindow";
import { FilterFormProps } from "./types";
import { descItemTypesFetchIfNeeded } from "actions/refTables/descItemTypes";
import { useThunkDispatch } from "utils/hooks";
import { RulDataTypeCodeEnum } from "api/RulDataTypeCodeEnum";
import { refRulDataTypesFetchIfNeeded } from "actions/refTables/rulDataTypes";
import { FilterFieldNumber } from "./fields/FilterFieldNumber";
import { FilterFieldText } from "./fields/FilterFieldText";
import { FilterFieldUnitdate } from "./fields/FilterFieldUnitdate";
import { WebApi } from "actions";
import { refRuleSetFetchIfNeeded } from "actions/refTables/ruleSet";

function formatOperation(operation: OperationCompareType) {
  switch (operation) {
    case OperationCompareType.Eq:
      return ": "
    case OperationCompareType.Neq:
      return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>≠</div>
    case OperationCompareType.Contains:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>*</div>
    case OperationCompareType.Gt:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{">"}</div>
    case OperationCompareType.Lt:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{"<"}</div>
    case OperationCompareType.Gte:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{">="}</div>
    case OperationCompareType.Lte:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{"<="}</div>
    default:
      return operation;
  }
}

interface DataTypeFilterDefinition {
  operations: OperationCompareType[];
  fieldComponent?: (props: {
    onChange: (value: string, isValid?: boolean) => void;
    value: string;
  }) => JSX.Element;
}

type DataTypeFiltersMap = Partial<Record<RulDataTypeCodeEnum, DataTypeFilterDefinition>>;

// const OperationCompareTypeEx = {
//   ...OperationCompareType,
//   Test: "TEST",
// } as const;
//
// type OperationCompareTypeEx = typeof OperationCompareTypeEx[keyof typeof OperationCompareTypeEx];

export function FilterDescItemModal({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialValue,
}: FilterFormProps<any>) {
  const availableDataTypesMap: DataTypeFiltersMap = {
    [RulDataTypeCodeEnum.INT]: {
      operations: [OperationCompareType.Eq, OperationCompareType.Neq, OperationCompareType.Lt, OperationCompareType.Gt, OperationCompareType.Lte, OperationCompareType.Gte],
      fieldComponent: FilterFieldNumber,
    },
    [RulDataTypeCodeEnum.DECIMAL]: {
      operations: [OperationCompareType.Eq, OperationCompareType.Neq, OperationCompareType.Lt, OperationCompareType.Gt, OperationCompareType.Lte, OperationCompareType.Gte],
      fieldComponent: FilterFieldNumber,
    },
    [RulDataTypeCodeEnum.STRING]: {
      operations: [OperationCompareType.Eq, OperationCompareType.Neq, OperationCompareType.Contains],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.TEXT]: {
      operations: [OperationCompareType.Eq, OperationCompareType.Neq, OperationCompareType.Contains],
      fieldComponent: FilterFieldText,
    },
    // [RulDataTypeCodeEnum.COORDINATES]: {operations: [OperationCompareType.Eq, OperationCompareType.Neq]},
    [RulDataTypeCodeEnum.UNITDATE]: {
      operations: [OperationCompareType.Eq, OperationCompareType.Neq, OperationCompareType.Lt, OperationCompareType.Gt, OperationCompareType.Lte, OperationCompareType.Gte],
      fieldComponent: FilterFieldUnitdate,
    },
    [RulDataTypeCodeEnum.ENUM]: {
      operations: [OperationCompareType.Eq, OperationCompareType.Neq],
    },
    // [RulDataTypeCodeEnum.RECORD_REF]: {operations: [OperationCompareType.Eq, OperationCompareType.Neq]},
  }

  const [itemTypeCode, setItemTypeCode] = useState<string>(initialValue?.data?.itemType.code);
  const [itemSpecCode, setItemSpecCode] = useState<string>(initialValue?.data?.itemSpec?.code);
  const [itemTypeQuery, setItemTypeQuery] = useState<string>(initialValue?.data?.itemType?.name || "");
  const [itemSpecQuery, setItemSpecQuery] = useState<string>(initialValue?.data?.itemSpec?.name || "");
  const [itemValue, setItemValue] = useState<string>(initialValue?.data?.itemValue);
  const [isValueValid, setIsValueValid] = useState<boolean>(initialValue?.data?.itemValue ? true : false);
  const [operation, setOperation] = useState<OperationCompareType>(initialValue.operation);
  const [descItemTypes, setDescItemTypes] = useState<DescItemTypeRef[]>([]);
  const [descItemTypesFetched, setDescItemTypesFetched] = useState<boolean>(false);

  const dataTypes = useSelector(({ refTables }: AppState) => refTables.rulDataTypes.items);

  // desc item types filtered by possible data types
  const allDescItemTypes = useSelector(({ refTables }: AppState) => refTables.descItemTypes.items.filter(({ dataTypeId }) => {
    const dataType = dataTypes.find(({ id }) => id === dataTypeId);
    return !!availableDataTypesMap[dataType.code];
  }));
  const arrangementRuleSets = useSelector(({ refTables }: AppState) =>
    refTables.ruleSet.items.filter(({ ruleType }) => ruleType === RuleType.ARRANGEMENT));

  const dispatch = useThunkDispatch();

  const { formatMessage } = useIntl();

  const isDirty = itemTypeCode != initialValue?.data?.code || (initialValue.operation && operation != initialValue.operation) || (!itemTypeCode && itemTypeQuery && true);
  const inputRef = useRef(null)
  useInitialFocus(inputRef);

  // desc item types filtered by query
  const filteredItemTypes = isDirty
    ? descItemTypes.filter((type) => {
      const string_norm = `${type.name}       ${type.shortcut}`.normalize('NFD').replace(/\p{Diacritic}/gu, '');
      return string_norm.toLowerCase().indexOf((itemTypeQuery || "").toLowerCase()) >= 0
    })
    : descItemTypes;

  const selectedItemType = itemTypeCode && descItemTypes.find(({ code }) => code === itemTypeCode);
  const selectedDataType = dataTypes.find(({ id }) => id === selectedItemType?.dataTypeId);
  const dataTypeFilterDefinition = selectedDataType?.code && availableDataTypesMap[selectedDataType.code];

  // desc item spec of selected item type filtered by query
  const filteredItemSpecs = isDirty ? selectedItemType?.descItemSpecs?.filter((spec) => spec.name.toLowerCase().indexOf((itemSpecQuery || "").toLowerCase()) >= 0) : selectedItemType?.descItemSpecs || [];

  // Load used refTables data, if not present
  useEffect(() => {
    dispatch(descItemTypesFetchIfNeeded());
    dispatch(refRulDataTypesFetchIfNeeded());
    dispatch(refRuleSetFetchIfNeeded());
  }, [])

  useEffect(() => {
    if (!descItemTypesFetched && arrangementRuleSets.length > 0 && allDescItemTypes.length > 0) {
      (async function () {
        const promises = arrangementRuleSets.map(({ id }) => WebApi.getItemTypeCodesByRuleSet(id));
        const itemTypes = await Promise.all(promises);
        // merge all arays into one
        const descItemTypeCodes: string[] = [].concat(...itemTypes);

        const availableDescItemTypes = allDescItemTypes.filter(({ code }) => descItemTypeCodes?.includes(code));
        setDescItemTypes(availableDescItemTypes);
        setDescItemTypesFetched(true);
      })()
    }
  }, [allDescItemTypes, arrangementRuleSets, descItemTypesFetched])

  // Preselect first operation from filter definition when item type changes
  useEffect(() => {
    if (!operation) {
      setOperation(dataTypeFilterDefinition?.operations?.[0]);
    }
  }, [dataTypeFilterDefinition?.operations, operation])

  const handleItemTypeSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    setItemTypeQuery(data.optionText || "");
    setItemTypeCode(data.optionValue || "");
    setOperation(undefined);
    setItemSpecQuery("");
    setItemSpecCode(undefined);
  }

  const handleItemSpecSelect = (_e: SelectionEvents, data: OptionOnSelectData) => {
    setItemSpecQuery(data.optionText || "");
    setItemSpecCode(data.optionValue || "");
  }

  const handleValueChange = (value: string = "", valid: boolean = true) => {
    setItemValue(value);
    setIsValueValid(value != "" && valid);
  }

  const handleFilterChange = useCallback(() => {
    const itemType = itemTypeCode && descItemTypes.find(({ code }) => code === itemTypeCode);
    const itemSpec = itemSpecCode && itemType?.descItemSpecs?.find(({ code }) => code === itemSpecCode);

    if (isDirty && itemType) {
      onFilterChange({
        filterType: FilterType.FieldValue,
        name: itemType.code,
        data: {
          itemType,
          itemSpec,
          itemValue,
        },
        operation,
        getDisplayValue: ({ operation, data }) => <>
          <b>{data.itemType.shortcut}</b>
          {formatOperation(operation)}
          {data.itemSpec && data.itemSpec.name}
          {data.itemSpec && data.itemValue && ": "}
          {data.itemValue && data.itemValue}
        </>,
        getFilterValue: ({ filterType, operation, data }) => ({
          filterType,
          field: {
            fieldType: FieldType.Node,
            typeCode: data.itemType.code,
            specCode: data.itemSpec?.code,
          },
          operation,
          value: data.itemValue,
        }),
        getSerializedString: ({ data, operation }) => {
          if (data.itemSpec && data.itemValue) {
            return `${data.itemType.name} - ${data.itemSpec.name} ${operation} ${data.itemValue}`
          }
          else if (data.itemSpec && !data.itemValue) {
            return `${data.itemType.name} ${operation} ${data.itemSpec.name}`
          }
          else if (!data.itemSpec && data.itemValue) {
            return `${data.itemType.name} ${operation} ${data.itemValue}`
          }
          return data.itemType.name;
        },
      });
    }
  }, [
    onFilterChange,
    itemTypeCode,
    itemSpecCode,
    itemValue,
    descItemTypes,
    operation,
    isDirty,
  ]);

  return <BaseFilterWindow
    filterName={formatMessage(messages[filterName])}
    isValid={itemTypeCode != ""
      && !!operation
      && dataTypeFilterDefinition?.fieldComponent ? isValueValid : true
        && selectedItemType?.useSpecification ? itemSpecCode != undefined : true
    }
    isDirty={isDirty}
    onClose={onClose}
    onFilterConfirm={handleFilterChange}
  >
    <Combobox
      ref={inputRef}
      clearable={true}
      value={itemTypeQuery}
      defaultValue={itemTypeQuery}
      onChange={(e) => {
        setItemTypeQuery(e.target.value);
      }}
      onOptionSelect={handleItemTypeSelect}
      positioning={{ position: "below", autoSize: "height" }}
    >
      {filteredItemTypes.map(({ name, id, code }) => {
        return <Option key={id} value={code.toString()}>{name}</Option>
      })}
    </Combobox>
    {dataTypeFilterDefinition
      && dataTypeFilterDefinition.operations.length > 1
      && <div style={{ margin: "5px 0" }}>
        <Menu>
          <MenuTrigger>
            <MenuButton size="small">
              {operation ? formatMessage(messages[operation]) : "-"}
            </MenuButton>
          </MenuTrigger>
          <MenuPopover>
            {dataTypeFilterDefinition.operations.map((_operation) => {
              return <MenuItem
                onClick={() => setOperation(_operation)}
              >{formatMessage(messages[_operation])}</MenuItem>
            })}
          </MenuPopover>
        </Menu>
      </div>
    }
    {selectedItemType?.useSpecification
      && selectedItemType?.descItemSpecs?.length > 0
      && <Combobox
        ref={inputRef}
        clearable={true}
        value={itemSpecQuery}
        defaultValue={itemSpecQuery}
        onChange={(e) => {
          setItemSpecQuery(e.target.value);
        }}
        onOptionSelect={handleItemSpecSelect}
        positioning={{ position: "below", autoSize: "height" }}
      >
        {filteredItemSpecs.map(({ name, id, code }) => {
          return <Option key={id} value={code.toString()}>{name}</Option>
        })}
      </Combobox>}
    {!!dataTypeFilterDefinition?.fieldComponent
      && <dataTypeFilterDefinition.fieldComponent value={itemValue} onChange={handleValueChange} />}
  </BaseFilterWindow>
}

