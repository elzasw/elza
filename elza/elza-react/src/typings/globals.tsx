import {AnyAction} from 'redux';

export type ThunkAction<R, S, E> = (dispatch: Dispatch<S>, getState: () => S, extraArgument: E) => R;

export interface Dispatch<S> {
    <R, E>(asyncAction: ThunkAction<R, S, E> | AnyAction | void): R;
}
