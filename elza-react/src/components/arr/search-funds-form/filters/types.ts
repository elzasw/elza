import { FieldValueFilter, FilterType, LogicalFilter, MultimatchContainsFilter, OperationCompareType } from "elza-api";

/**
 * One entry in a multi-item filter's data list — pairs a value with the
 * comparison operation chosen for it. Multiple entries are OR-combined.
 */
export interface FilterEntry<T> {
  value: T;
  operation: OperationCompareType;
}

/**
 * A search-funds-form filter that holds an array (or other custom shape) of
 * values, each with its own operation. Unlike the fund-filters' single-value
 * `FilterObject`, there is no top-level `operation` field.
 */
export interface MultiFilterObject<T = unknown> {
  filterType: FilterType;
  name?: string;
  data: FilterEntry<T>[];
  getDisplayValue: (filter: MultiFilterObject<T>) => React.ReactNode;
  getFilterValue: (filter: MultiFilterObject<T>) => FieldValueFilter | MultimatchContainsFilter | LogicalFilter;
  getSerializedString: (filter: MultiFilterObject<T>) => string;
}

export interface FilterFormProps<T = unknown> {
  filterName: string;
  onFilterChange: (data: MultiFilterObject<T>) => void;
  onClose: () => void;
  initialValue?: Partial<MultiFilterObject<T>>;
}
