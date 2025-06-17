import { FieldValueFilter, FilterType, FondsFieldName, MultimatchContainsFilter, OperationCompareType } from "elza-api";

export interface FilterObject<T = unknown> {
  filterType: FilterType;
  name?: FondsFieldName;
  operation?: OperationCompareType;
  data: T;
  getDisplayValue: (filter: FilterObject<T>) => React.ReactNode;
  getFilterValue: (filter: FilterObject<T>) => FieldValueFilter | MultimatchContainsFilter;
  getSerializedString: (filter: FilterObject<T>) => string;
}

export interface FilterFormProps<T = unknown> {
  filterName: FondsFieldName;
  onFilterChange: (data: FilterObject<T>) => void;
  onClose: () => void;
  initialValue?: Partial<FilterObject<T>>;
}
