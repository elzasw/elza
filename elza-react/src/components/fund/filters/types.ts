import { FieldValueFilter, FilterType, FundsFieldName, MultimatchContainsFilter, OperationCompareType } from "elza-api";

export interface FilterObject<T = unknown> {
  filterType: FilterType;
  name?: FundsFieldName;
  operation?: OperationCompareType;
  data: T;
  getDisplayValue: (filter: FilterObject<T>) => React.ReactNode;
  getFilterValue: (filter: FilterObject<T>) => FieldValueFilter | MultimatchContainsFilter;
  getSerializedString: (filter: FilterObject<T>) => string;
}

export interface FilterFormProps<T = unknown> {
  filterName: FundsFieldName;
  onFilterChange: (data: FilterObject<T>) => void;
  onClose: () => void;
  initialValue?: Partial<FilterObject<T>>;
}
