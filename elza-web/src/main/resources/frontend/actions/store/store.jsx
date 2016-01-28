import * as types from 'actions/constants/actionTypes';
import {save} from 'stores/app/AppStore'
import {routerNavigate} from 'actions/router'

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
                dispatch(routerNavigate('/party'));
                break;
            case 'REGISTRY_REGION':
                dispatch(storeLoad({registryRegion: data}));
                dispatch(routerNavigate('/registry'));
                break;
            case 'ARR_REGION':
                dispatch(storeLoad({arrRegion: data}));
                dispatch(routerNavigate('/arr'));
                break;
            case 'ARR_REGION_FA':
                dispatch(storeLoad({arrRegionFa: data}));
                dispatch(routerNavigate('/arr'));
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
