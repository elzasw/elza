import { Button } from "@fluentui/react-components";
import { DraggableWindow } from "components/shared";
import { Position } from "components/shared/draggable-window";
import { FondsFieldName } from "elza-api";
import { FundFilterInstitutionRefModal } from "./FundFilterInstitutionRefModal";
import { FundFilterNumberForm } from "./FundFilterNumber";
import { Institution } from "typings/store";
import { FundFilterTextForm } from "./FundFilterText";
import { FilterObject } from "./types";

export interface Props {
  filterName: string;
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
    case FondsFieldName.InstitutionCode:
      filterForm = <FundFilterInstitutionRefModal
        initialValue={initialValue as FilterObject<Institution>}
        filterName={filterName}
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
    case FondsFieldName.FondsNumber:
      filterForm = <FundFilterNumberForm
        initialValue={initialValue as FilterObject<string>}
        filterName={filterName}
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
    case FondsFieldName.Mark:
    case FondsFieldName.Name:
    case FondsFieldName.InternalCode:
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

