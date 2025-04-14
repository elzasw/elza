import { FluentDialogContext } from "components/shared/dialog/FluentModalDialog";
import { useContext } from "react";
import { FilterChange, FundFilterModal } from "./FundFilterModal";
import { Position } from "components/shared/draggable-window";
// import { FondsFilterField } from "elza-api";

export function useFilterModal() {
  const { showModal: _showModal } = useContext(FluentDialogContext);
  return async function showModal(filter: Partial<FilterChange>, initialPosition: Position) {
    console.log("#ff", filter, initialPosition);
    return _showModal<"OK" | "CANCEL", FilterChange>({
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
