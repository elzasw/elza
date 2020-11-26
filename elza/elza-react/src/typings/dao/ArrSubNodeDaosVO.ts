import {ArrDaoVO} from "./ArrDaoVO";

export interface ArrSubNodeDaosVO {
    data: ArrDaoVO[]
    isFetching: boolean;
    fetched: boolean;
    dirty: boolean;
    currentDataKey: number | string;
}

