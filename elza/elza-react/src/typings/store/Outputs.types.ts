import { SubNodeForm } from "./SubNodeForm.types";

export interface FundOutputDetail {
    currentDataKey?: string;
    fetched?: boolean;
    fetching?: boolean;
    id?: number;
    subNodeForm?: SubNodeForm;
}

export interface FundOutput {
    currentDataKey?: string;
    fetched?: boolean;
    filterState?: unknown;
    fundOutputDetail?: FundOutputDetail;
    fundOutputFiles?: unknown;
    fundOutputFunctions?: unknown;
    isFetching?: boolean;
    outputs?: unknown[];
}
