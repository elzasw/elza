import { SubNodeForm } from "./SubNodeForm.types";

export interface FundDataGrid {
    columnInfos?: unknown;
    columnsOrder?: unknown[];
    currentDataKey?: string;
    data?: unknown;
    fetchedData?: boolean;
    fetchedFilter?: boolean;
    filter?: unknown;
    filterDirty?: boolean;
    initialised?: boolean;
    isFetchingData?: boolean;
    isFetchingFilter?: boolean;
    items?: unknown[];
    itemsCount?: number;
    searchedCurrentIndex?: number;
    searchedItems?: unknown[];
    subNodeForm?: SubNodeForm;
    visibleColumns?: unknown;
    serializedFilter?: string;
}
