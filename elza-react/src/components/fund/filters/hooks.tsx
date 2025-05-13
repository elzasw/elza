import { FluentDialogContext } from "components/shared/dialog/FluentModalDialog";
import { useContext } from "react";
import { FundFilterModal } from "./FundFilterModal";
import { Position } from "components/shared/draggable-window";
import { FilterObject } from "./types";

export function useFilterModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);
  return async function showModal(filter: Partial<FilterObject>, initialPosition: Position) {
    return _showModal<"OK" | "CANCEL", FilterObject>({
      createDialog: ({ handleResult }) =>
        <FundFilterModal
          initialValue={filter}
          initialPosition={initialPosition}
          filterName={filter.name}
          onFilterChange={(filterChange) => handleResult("OK", filterChange)}
          onClose={() => handleResult("CANCEL")}
        />
    })
  }
}
