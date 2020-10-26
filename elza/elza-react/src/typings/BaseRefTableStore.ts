export interface BaseRefTableStore<T> {
    dirty: boolean;
    fetched: boolean;
    isFetching: boolean;
    items?: T[];
    itemsMap?: {[key: number]: T};
    lastUpdated?: number;
}
