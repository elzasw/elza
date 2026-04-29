import { FieldValueFilter, FilterType, LogicalFilter, MultimatchContainsFilter, OperationCompareType } from "elza-api";

export interface FilterObject<T = unknown> {
  filterType: FilterType;
  name?: string;
  operation?: OperationCompareType;
  data: T;
  getDisplayValue: (filter: FilterObject<T>) => React.ReactNode;
  getFilterValue: (filter: FilterObject<T>) => FieldValueFilter | MultimatchContainsFilter | LogicalFilter;
  getSerializedString: (filter: FilterObject<T>) => string;
}

export interface FilterFormProps<T = unknown> {
  filterName: string;
  onFilterChange: (data: FilterObject<T>) => void;
  onClose: () => void;
  initialValue?: Partial<FilterObject<T>>;
}
