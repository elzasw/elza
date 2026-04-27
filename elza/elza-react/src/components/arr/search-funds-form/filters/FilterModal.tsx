import { Button } from "@fluentui/react-components";
import { DraggableWindow } from "components/shared";
import { Position } from "components/shared/draggable-window";
import { FilterObject } from "./types";
import { FilterDescItemModal } from "./FilterDescItemModal";
import { FondsFieldName, NodeFieldName } from "elza-api";
import { FilterText } from "./FilterText";
import { FundFilterInstitutionRefModal } from "components/fund/filters/FundFilterInstitutionRefModal";
import { Institution } from "typings/store";

import { FilterFondsRef, FondsData } from "./FilterFondsRef";

export interface Props {
  filterName: string;
  onFilterChange: (data: FilterObject) => void;
  onClose: () => void;
  initialPosition?: Position;
  initialValue?: Partial<FilterObject>;
}

export function FilterModal({
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
    case "DescItem":
      filterForm = <FilterDescItemModal
        initialValue={initialValue as FilterObject<string>}
        filterName={filterName}
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
    case NodeFieldName.ConformityError:
    case NodeFieldName.ConformityMissing:
    case NodeFieldName.Uuid:
      filterForm = <FilterText
        initialValue={initialValue as FilterObject<string>}
        filterName={filterName}
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
    case FondsFieldName.InstitutionId:
      filterForm = <FundFilterInstitutionRefModal
        initialValue={initialValue as FilterObject<Institution>}
        filterName={filterName}
        valueSource="id"
        onFilterChange={onFilterChange}
        onClose={onClose}
      />
      break;
    case FondsFieldName.FondsId:
      filterForm = <FilterFondsRef
        initialValue={initialValue as FilterObject<FondsData>}
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

