import {ItemTypeLiteVO} from '../api/ItemTypeLiteVO';

type X = string | number;

export interface AppStoreState {
    infoTypesMap: {[key in X]: ItemTypeLiteVO};
    refTypesMap;
}
