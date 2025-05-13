import { Button } from "@fluentui/react-components";
import { DraggableWindow } from "components/shared";
import { Position } from "components/shared/draggable-window";
import { FieldValueFilter, FilterType, FundsFieldName, MultimatchContainsFilter, OperationCompareType } from "elza-api";
import { FundFilterInstitutionRefModal } from "./FundFilterInstitutionRefModal";
import { FundFilterNumberForm } from "./FundFilterNumber";
import { Institution } from "typings/store";
import { FundFilterTextForm } from "./FundFilterText";

export interface FilterChange {
  name: FundsFieldName;
  value: string;
  operation: OperationCompareType;
}

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

export interface Props {
  filterName: FundsFieldName;
  onFilterChange: (data: FilterObject) => void;
  onClose: () => void;
  initialPosition?: Position;
  initialValue?: Partial<FilterObject>;
}
export function FundFilterModal({
  filterName,
  onFilterChange,
  onClose = () => { console.warn("'onClose' not defined") },
  initialPosition,
  initialValue = {},
}: Props) {

  let filterForm = <div style={{ background: "red", padding: "20px" }}>
    <div><h1>Not implemented</h1></div>
    <div>{filterName}</div>
    <Button onClick={onClose}>Close</Button>
  </div>

  switch (filterName) {
    case FundsFieldName.InstitutionCode:
      filterForm = <FundFilterInstitutionRefModal
        initialValue={initialValue as FilterObject<Institution>}
        filterName={filterName}
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
    case FundsFieldName.FundNumber:
      filterForm = <FundFilterNumberForm
        initialValue={initialValue as FilterObject<string>}
        filterName={filterName}
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
    case FundsFieldName.Mark:
    case FundsFieldName.Name:
    case FundsFieldName.InternalCode:
      filterForm = <FundFilterTextForm
        initialValue={initialValue as FilterObject<string>}
        filterName={filterName}
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
  }

  return <DraggableWindow disableDrag={true} initialPosition={initialPosition}>
    {filterForm}
  </DraggableWindow>;
}

