import { FluentDialogContext } from "components/shared/dialog/FluentModalDialog";
import { useContext } from "react";
import { FilterModal } from "./FilterModal";
import { Position } from "components/shared/draggable-window";
import { MultiFilterObject } from "./types";

export function useFilterModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);
  return async function showModal(filter: Partial<MultiFilterObject>, initialPosition: Position) {
    return _showModal<"OK" | "CANCEL", MultiFilterObject>({
      createDialog: ({ handleResult }) =>
        <FilterModal
          initialValue={filter}
          initialPosition={initialPosition}
          filterName={filter.name}
          onFilterChange={(filterChange) => handleResult("OK", filterChange)}
          onClose={() => handleResult("CANCEL")}
        />
    })
  }
}
