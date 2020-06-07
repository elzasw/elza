import {AnyAction} from 'redux';

export type ThunkAction<R, S, E> = (dispatch: Dispatch<S>, getState: () => S, extraArgument: E) => R;

export interface Dispatch<S> {
    <R, E>(asyncAction: ThunkAction<R, S, E> | AnyAction | void): R;
}

export interface AppFetchingStore {
	id?: number,
	fetched: boolean,
	fetching: boolean,
	currentDataKey: any,
}

export enum ApSearchType {
    DISABLED='DISABLED',
    JOIN='JOIN',
    FULLTEXT='FULLTEXT',
    RIGHT_SIDE_LIKE='RIGHT_SIDE_LIKE'
}
