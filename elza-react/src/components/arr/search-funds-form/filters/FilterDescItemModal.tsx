import { Combobox, OptionOnSelectData, SelectionEvents, Option, Menu, MenuTrigger, MenuButton, MenuPopover, MenuItem } from "@fluentui/react-components";
import { ShapeIntersectFilled } from "@fluentui/react-icons";
import { SquaresNestedRegular } from "@fluentui/react-icons";
import { useCallback, useEffect, useRef, useState } from "react";
import { useInitialFocus } from "./utils";
import { useSelector } from "react-redux";
import { AppState, DescItemTypeRef, RuleType } from "typings/store";
import { messages } from "./messages";
import { FieldType, FilterType, OperationCompareType } from "elza-api";
import { IntlShape, useIntl } from "react-intl";
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
import { RulDescItemSpecExtVO } from "api/RulDescItemSpecExtVO";
import { FilterFieldRecordRef } from "./fields/FilterFieldRecordRef";
import { RulDataTypeVO } from "api/RulDataTypeVO";
import { FilterValueFieldProps } from "./fields/types";

function formatOperation(operation: OperationCompareType, intl?: IntlShape) {
  switch (operation) {
    case OperationCompareType.Eq:
      return ": "
    case OperationCompareType.Neq:
      return <div style={{ padding: "0 5px", fontSize: "1.4rem" }}>≠</div>
    case OperationCompareType.Contains:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>∋</div>
    case OperationCompareType.Gt:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{">"}</div>
    case OperationCompareType.Lt:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{"<"}</div>
    case OperationCompareType.Gte:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{">="}</div>
    case OperationCompareType.Lte:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}>{"<="}</div>
    case OperationCompareType.Intersect:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}><ShapeIntersectFilled /></div>
    case OperationCompareType.IsIn:
      return <div style={{ padding: "0 5px", fontSize: "1.2rem" }}><SquaresNestedRegular /></div>
    case OperationCompareType.NotNull:
    case OperationCompareType.IsNull:
      return <div style={{ padding: "0 5px" }}>{intl?.formatMessage(messages[operation]) || operation}</div>
    default:
      return <div style={{ padding: "0 5px" }}>{operation}</div>;
  }
}

function formatSpec(operation: OperationCompareType, data: any, dataType: RulDataTypeVO) {
  const ignoreValue = operation === OperationCompareType.NotNull || operation === OperationCompareType.IsNull;

  if (!data.itemSpec) {
    return undefined;
  }
  if (dataType.code === RulDataTypeCodeEnum.ENUM) {
    if (ignoreValue) { return undefined; }
    return data.itemSpec.name;
  }
  return <>
    &nbsp;
    ({data.itemSpec.name})
  </>
}

function formatLabel(operation: OperationCompareType, data: any) {
  const ignoreValue = operation === OperationCompareType.NotNull || operation === OperationCompareType.IsNull;
  return !ignoreValue && data.itemLabel
}

function formatDisplayValue(operation: OperationCompareType, data: any, dataType: RulDataTypeVO, intl: IntlShape) {
  return <>
    <b>{data.itemType.shortcut}</b>
    {dataType.code === RulDataTypeCodeEnum.ENUM && formatOperation(operation, intl)}
    {formatSpec(operation, data, dataType)}
    {dataType.code !== RulDataTypeCodeEnum.ENUM && formatOperation(operation, intl)}
    {formatLabel(operation, data)}
  </>
}

interface DataTypeFilterDefinition {
  operations: OperationCompareType[];
  fieldComponent?: (props: FilterValueFieldProps) => JSX.Element;
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
      operations: [
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.Lt,
        OperationCompareType.Gt,
        OperationCompareType.Lte,
        OperationCompareType.Gte,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull
      ],
      fieldComponent: FilterFieldNumber,
    },
    [RulDataTypeCodeEnum.DECIMAL]: {
      operations: [
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.Lt,
        OperationCompareType.Gt,
        OperationCompareType.Lte,
        OperationCompareType.Gte,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull
      ],
      fieldComponent: FilterFieldNumber,
    },
    [RulDataTypeCodeEnum.STRING]: {
      operations: [
        OperationCompareType.Contains,
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull
      ],
      fieldComponent: FilterFieldText,
    },
    [RulDataTypeCodeEnum.TEXT]: {
      operations: [
        OperationCompareType.Contains,
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull
      ],
      fieldComponent: FilterFieldText,
    },
    // [RulDataTypeCodeEnum.COORDINATES]: {operations: [OperationCompareType.Eq, OperationCompareType.Neq]},
    [RulDataTypeCodeEnum.UNITDATE]: {
      operations: [
        OperationCompareType.Contains,
        OperationCompareType.Intersect,
        OperationCompareType.IsIn,
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.Lt,
        OperationCompareType.Gt,
        OperationCompareType.Lte,
        OperationCompareType.Gte,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull
      ],
      fieldComponent: FilterFieldUnitdate,
    },
    [RulDataTypeCodeEnum.ENUM]: {
      operations: [
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull
      ],
    },
    [RulDataTypeCodeEnum.RECORD_REF]: {
      operations: [
        OperationCompareType.Eq,
        OperationCompareType.Neq,
        OperationCompareType.IsNull,
        OperationCompareType.NotNull
      ],
      fieldComponent: FilterFieldRecordRef,
    },
  }

  const [itemTypeCode, setItemTypeCode] = useState<string>(initialValue?.data?.itemType.code);
  const [itemSpecCode, setItemSpecCode] = useState<string>(initialValue?.data?.itemSpec?.code);
  const [itemTypeQuery, setItemTypeQuery] = useState<string>(initialValue?.data?.itemType?.name || "");
  const [itemSpecQuery, setItemSpecQuery] = useState<string>(initialValue?.data?.itemSpec?.name || "");
  const [itemValue, setItemValue] = useState<string>(initialValue?.data?.itemValue);
  const [itemLabel, setItemLabel] = useState<string>(initialValue?.data?.itemLabel);
  const [isValueValid, setIsValueValid] = useState<boolean>(initialValue?.data?.itemValue ? true : false);
  const [operation, setOperation] = useState<OperationCompareType>(initialValue.operation);
  const [descItemTypes, setDescItemTypes] = useState<DescItemTypeRef[]>([]);
  const [descItemTypesFetched, setDescItemTypesFetched] = useState<boolean>(false);

  const dataTypes = useSelector(({ refTables }: AppState) => refTables.rulDataTypes);

  // desc item types filtered by possible data types
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

  const isDirty = itemTypeCode != initialValue?.data?.code || (initialValue.operation && operation != initialValue.operation) || (!itemTypeCode && !!itemTypeQuery);
  const inputRef = useRef(null)
  useInitialFocus(inputRef);

  // desc item types filtered by query
  const filterItemTypes = () => {
    if (!itemTypeQuery || !isDirty) { return descItemTypes; }

    const normalizedQuery = (itemTypeQuery).toLowerCase();

    return descItemTypes.filter(({ name, shortcut }) => {
      const sources = [
        name.toLowerCase(),
        shortcut.toLowerCase(),
        name.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase(), // without diacritic
        shortcut.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase() // without diacritic
      ]
      return sources.find((normalizedSource) => normalizedSource.indexOf(normalizedQuery) >= 0) != undefined;
    })
  }

  const filteredItemTypes = filterItemTypes();

  let selectedItemType: DescItemTypeRef | undefined;
  let selectedItemSpec: RulDescItemSpecExtVO | undefined;
  let selectedDataType: RulDataTypeVO | undefined;
  let dataTypeFilterDefinition: DataTypeFilterDefinition | undefined;

  let filteredItemSpecs = [];

  if (itemTypeCode && descItemTypes.length > 0 && dataTypes) {

    selectedItemType = descItemTypes.find(({ code }) => code === itemTypeCode);
    if (!selectedItemType) {
      throw `Missing item type: ${itemTypeCode}`;
    }

    selectedDataType = dataTypes.items.find(({ id }) => id === selectedItemType.dataTypeId);
    if (!selectedDataType) {
      throw `Missing data type: ${selectedItemType.dataTypeId}`;
    }

    // desc item spec of selected item type filtered by query
    filteredItemSpecs = isDirty
      ? selectedItemType.descItemSpecs?.filter((spec) => spec.name.toLowerCase().indexOf((itemSpecQuery || "").toLowerCase()) >= 0)
      : selectedItemType.descItemSpecs || [];

    if (itemSpecCode) {
      selectedItemSpec = selectedItemType.descItemSpecs.find(({ code }) => code === itemSpecCode);
    }

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

  const handleValueChange = (value: string = "", valid: boolean = true, valueLabel?: string) => {
    setItemValue(value);
    setItemLabel(valueLabel || value);
    setIsValueValid(value != "" && valid);
  }

  const handleFilterChange = useCallback(() => {
    const itemType = itemTypeCode && descItemTypes.find(({ code }) => code === itemTypeCode);
    const itemSpec = itemSpecCode && itemType?.descItemSpecs?.find(({ code }) => code === itemSpecCode);
    const dataType = dataTypes.itemsMap[itemType.dataTypeId];

    let _itemValue = itemValue;
    let _itemLabel = itemLabel;
    let _itemSpec = itemSpec;
    // remove value when not used by operation
    if (operation === OperationCompareType.IsNull || operation === OperationCompareType.NotNull) {
      // remove spec when used as value in enum type
      if (dataType.code === RulDataTypeCodeEnum.ENUM) {
        _itemSpec = undefined;
      }
      _itemValue = undefined;
      _itemLabel = undefined;
    }

    if (isDirty && itemType) {
      onFilterChange({
        filterType: FilterType.FieldValue,
        name: itemType.code,
        data: {
          itemType,
          itemSpec: _itemSpec,
          itemValue: _itemValue,
          itemLabel: _itemLabel,
        },
        operation,
        getDisplayValue: ({ operation, data }) => <>
          {formatDisplayValue(operation, data, dataType, intl)}
        </>,
        getFilterValue: ({ filterType, operation, data }) => {
          return ({
            filterType,
            field: {
              fieldType: FieldType.DescItem,
              typeCode: data.itemType.code,
              specCode: data.itemSpec?.code,
            },
            operation,
            value: data.itemValue,
          })
        },
        getSerializedString: ({ data, operation }) => {
          if (data.itemSpec && data.itemValue) {
            return `${data.itemType.code} - ${data.itemSpec.code} ${operation} ${data.itemValue}`
          }
          else if (data.itemSpec && !data.itemValue) {
            return `${data.itemType.code} ${operation} ${data.itemSpec.code}`
          }
          else if (!data.itemSpec && data.itemValue) {
            return `${data.itemType.code} ${operation} ${data.itemValue}`
          }
          return `${data.itemType.code} ${operation}`;
        },
      });
    }
  }, [
    onFilterChange,
    itemTypeCode,
    itemSpecCode,
    itemValue,
    itemLabel,
    descItemTypes,
    operation,
    isDirty,
    intl,
    dataTypes,
  ]);

  function validate() {
    if (itemTypeCode == "") { return false; }
    if (!operation) { return false; }

    // if operation is IsNull or NotNull, ignore invalid value
    if (
      operation === OperationCompareType.IsNull
      || operation === OperationCompareType.NotNull
      // if value field component is defined filter value is required
      || !dataTypeFilterDefinition?.fieldComponent
    ) {
      return true;
    }

    return isValueValid;
  }

  const hideValue = operation === OperationCompareType.IsNull || operation === OperationCompareType.NotNull;
  const hideSpec = hideValue && selectedDataType?.code === RulDataTypeCodeEnum.ENUM;

  return <BaseFilterWindow
    filterName={formatMessage(messages[filterName])}
    isValid={validate()}
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
      && !hideSpec
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
      && !hideValue
      && <dataTypeFilterDefinition.fieldComponent
        value={itemValue}
        label={itemLabel}
        onChange={handleValueChange}
        itemType={selectedItemType}
        itemSpec={selectedItemSpec}
      />}
  </BaseFilterWindow>
}

