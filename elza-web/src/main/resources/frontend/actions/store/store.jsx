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
console.log("SSSSS storeLoadData:", type, data);
}

export function storeLoad() {
    return (dispatch, getState) => {
        var store = getState();

        var action = {
            type: types.STORE_LOAD,
            ..._data
        }

        dispatch(action);
    }
}
