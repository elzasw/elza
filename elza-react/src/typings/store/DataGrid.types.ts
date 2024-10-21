import { SubNodeForm } from "./SubNodeForm.types";

export interface FundDataGrid {
    cellFocus?: unknown;
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
    pageIndex?: number;
    pageSize?: number;
    searchedCurrentIndex?: number;
    searchedItems?: unknown[];
    selectedIds?: unknown[];
    selectedRowIndexes?: unknown[];
    subNodeForm?: SubNodeForm;
    visibleColumns?: unknown;
}
