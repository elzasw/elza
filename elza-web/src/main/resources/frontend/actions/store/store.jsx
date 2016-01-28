import * as types from 'actions/constants/actionTypes';
import {save} from 'stores/app/AppStore'

var _data;
export function storeSave() {
    return (dispatch, getState) => {
        var store = getState();
        var data = save(store);
        _data = data;
        dispatch(storeStateData(data))
    }
}

export function storeStateData(data) {
    return {
        type: types.STORE_STATE_DATA,
        ...data
    }
}

export function storeLoadData(type, data) {
    return (dispatch, getState) => {
        switch (type) {
            case 'PARTY_REGION':
                dispatch(storeLoad({partyRegion: data}));
                break;
            case 'REGISTRY_REGION':
                dispatch(storeLoad({registryRegion: data}));
                break;
            case 'ARR_REGION':
                dispatch(storeLoad({arrRegion: data}));
                break;
            case 'ARR_REGION_FA':
                dispatch(storeLoad({arrRegionFa: data}));
                break;
        }
    }
console.log("SSSSS storeLoadData:", type, data);
}

export function storeLoad(data) {
    return {
        type: types.STORE_LOAD,
        ...data
    }
}
export function ___old_storeLoad() {
    return (dispatch, getState) => {
        var store = getState();

        var action = {
            type: types.STORE_LOAD,
            ..._data
        }

        dispatch(action);
    }
}
