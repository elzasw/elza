import { Action } from 'redux';

export interface AreaAction<T extends string> extends Action<T> {
    area: string;
    store?: {[key: string]: any};
}
