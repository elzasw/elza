import {
    AbstractFilter,
    AipField,
    AipFieldName,
    BoolValueFilter,
    DateValueFilter,
    EnumValueFilter,
    FieldType,
    FilterType,
    NumberValueFilter,
    OperationEqualityType,
    OperationNumberType,
    OperationRangeType,
    OperationTextType,
    RefValueFilter,
    TextValueFilter,
} from "elza-api";

/**
 * Kind of value a column holds, and with it the filter model that carries the value.
 *
 * The two enumerated columns are kept apart because they offer different constants,
 * not because the server treats them differently.
 */
export type AipValueType =
    | "text" | "number" | "bool" | "date" | "ref" | "importState" | "exportState" | "problemType"
    | "linkState";

/**
 * Comparison offered for a column. A subset of the shared contract - the AIP list
 * implements part of it and refuses the rest, so nothing outside this list is offered.
 */
export type AipOperation =
    | typeof OperationTextType.Eq
    | typeof OperationEqualityType.Neq
    | typeof OperationTextType.Contains
    | typeof OperationTextType.NotContains
    | typeof OperationNumberType.Between
    | typeof OperationTextType.IsNull
    | typeof OperationTextType.NotNull;

/**
 * Comparisons offered per kind of value.
 *
 * This is the client side of AipFilterCapabilities; a comparison missing here is one the
 * server would refuse.
 */
export const OPERATIONS: Record<AipValueType, AipOperation[]> = {
    text: ["CONTAINS", "NOT_CONTAINS", "EQ", "IS_NULL", "NOT_NULL"],
    number: ["BETWEEN", "EQ", "IS_NULL", "NOT_NULL"],
    bool: ["EQ", "NEQ", "IS_NULL", "NOT_NULL"],
    date: ["BETWEEN", "IS_NULL", "NOT_NULL"],
    ref: ["EQ", "NEQ", "IS_NULL", "NOT_NULL"],
    importState: ["EQ", "NEQ", "IS_NULL", "NOT_NULL"],
    exportState: ["EQ", "NEQ", "IS_NULL", "NOT_NULL"],
    problemType: ["EQ", "NEQ", "IS_NULL", "NOT_NULL"],
    linkState: ["EQ", "NEQ"],
};

/** Comparisons that need no value at all. */
export const isNullaryOperation = (operation: AipOperation) =>
    operation === "IS_NULL" || operation === "NOT_NULL";

/** Comparisons that need a pair of bounds rather than one value. */
export const isRangeOperation = (operation: AipOperation) => operation === "BETWEEN";

export const aipField = (fieldName: AipFieldName): AipField => ({
    fieldType: FieldType.AipField,
    fieldName,
});

export type AipFilterValues = {
    operation: AipOperation;
    value?: string | number | boolean | null;
    from?: string | null;
    to?: string | null;
};

/**
 * Builds the filter of the shared contract that carries a value of this kind.
 *
 * Every branch produces a model whose value is typed as its column is, so the server can
 * bind the parameter in the type of that column.
 */
export const buildFilter = (
    fieldName: AipFieldName,
    valueType: AipValueType,
    values: AipFilterValues,
): AbstractFilter => {
    const field = aipField(fieldName);
    const nullary = isNullaryOperation(values.operation);

    switch (valueType) {
        case "text": {
            const filter: TextValueFilter = {
                filterType: FilterType.TextValue,
                field,
                operation: values.operation as OperationTextType,
            };
            if (!nullary) {
                filter.value = values.value == null ? undefined : String(values.value);
            }
            return filter;
        }
        case "number": {
            const filter: NumberValueFilter = {
                filterType: FilterType.NumberValue,
                field,
                operation: values.operation as OperationNumberType,
            };
            if (isRangeOperation(values.operation)) {
                filter.from = toNumber(values.from);
                filter.to = toNumber(values.to);
            } else if (!nullary) {
                filter.value = toNumber(values.value);
            }
            return filter;
        }
        case "bool": {
            const filter: BoolValueFilter = {
                filterType: FilterType.BoolValue,
                field,
                operation: values.operation as OperationEqualityType,
            };
            if (!nullary) {
                filter.value = values.value === true || values.value === "true";
            }
            return filter;
        }
        case "date": {
            const filter: DateValueFilter = {
                filterType: FilterType.DateValue,
                field,
                operation: values.operation as OperationRangeType,
            };
            if (isRangeOperation(values.operation)) {
                filter.from = values.from ?? undefined;
                filter.to = values.to ?? undefined;
            }
            return filter;
        }
        case "ref": {
            const filter: RefValueFilter = {
                filterType: FilterType.RefValue,
                field,
                operation: values.operation as OperationEqualityType,
            };
            if (!nullary) {
                filter.value = toNumber(values.value);
            }
            return filter;
        }
        case "importState":
        case "exportState":
        case "problemType":
        case "linkState": {
            const filter: EnumValueFilter = {
                filterType: FilterType.EnumValue,
                field,
                operation: values.operation as OperationEqualityType,
            };
            if (!nullary) {
                filter.value = values.value == null ? undefined : String(values.value);
            }
            return filter;
        }
    }
};

const toNumber = (value: unknown): number | undefined => {
    if (value == null || value === "") {
        return undefined;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
};
